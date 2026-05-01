package com.dmzkiaddon.world;

import com.dmzkiaddon.DMZKiAddon;
import com.dmzkiaddon.entity.masters.MasterFriezaEntity;
import com.dmzkiaddon.entity.masters.MasterPiccoloEntity;
import com.dmzkiaddon.entity.masters.MasterVegetaEntity;
import com.dmzkiaddon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;

/**
 * Spawns Vegeta/Piccolo/Frieza without causing world-load lag.
 *
 * Key insight: DO NOT call getBiome() during the initial chunk flood.
 * Instead, queue loaded chunks and process ONE per server tick after
 * a player has joined. This spreads the biome checks over many ticks,
 * making each tick cost ~0ms extra.
 */
@Mod.EventBusSubscriber(modid = DMZKiAddon.MOD_ID)
public class MasterSpawnManager {

    // ── Lang keys ──────────────────────────────────────────────────────────
    private static final String LOG_VEGETA_NO_PLAINS = "log.dmzkiaddon.spawn.vegeta_no_plains";
    private static final String LOG_PICCOLO_FALLBACK = "log.dmzkiaddon.spawn.piccolo_fallback";
    private static final String LOG_STRUCT_NOT_FOUND = "log.dmzkiaddon.spawn.structure_not_found";
    private static final String LOG_STRUCT_PLACED    = "log.dmzkiaddon.spawn.structure_placed";
    private static final String LOG_STRUCT_ERROR     = "log.dmzkiaddon.spawn.structure_error";

    private static final String MSG_VEGETA_SPAWN  = "msg.dmzkiaddon.spawn.vegeta";
    private static final String MSG_PICCOLO_SPAWN = "msg.dmzkiaddon.spawn.piccolo";
    private static final String MSG_FRIEZA_SPAWN  = "msg.dmzkiaddon.spawn.frieza";

    // ── Estado ─────────────────────────────────────────────────────────────
    private static boolean needVegeta  = false;
    private static boolean needPiccolo = false;
    private static boolean needFrieza  = false;

    private static BlockPos vegetaCandidate  = null;
    private static BlockPos piccoloCandidate = null;

    /** Whether at least one player has joined (unlocks chunk processing). */
    private static boolean playerJoined = false;

    /**
     * Pending chunks queued during initial load, processed 1-per-tick after player joins.
     * Stored as encoded longs: upper 32 bits = chunkX, lower 32 bits = chunkZ.
     */
    private static final Deque<Long> overworldQueue = new ArrayDeque<>();
    private static final Deque<Long> namekQueue     = new ArrayDeque<>();

    /** ServerLevel references kept until no longer needed. */
    private static ServerLevel pendingOverworld = null;
    private static ServerLevel pendingNamek     = null;

    /** After this many ticks with Vegeta found but not Piccolo, use fallback. */
    private static final int FALLBACK_TICKS       = 6000;
    private static int ticksWaitingForPiccolo      = 0;

    // ── ResourceLocations ──────────────────────────────────────────────────
    private static final ResourceLocation RL_VEGETA =
            ResourceLocation.fromNamespaceAndPath("dmzkiaddon", "masters/master_vegeta");
    private static final ResourceLocation RL_PICCOLO =
            ResourceLocation.fromNamespaceAndPath("dmzkiaddon", "masters/master_piccolo");
    private static final ResourceLocation RL_FRIEZA =
            ResourceLocation.fromNamespaceAndPath("dmzkiaddon", "masters/master_frieza");

    private static final ResourceKey<Level> NAMEK = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("dragonminez", "namek")
    );

    // ── ServerStartedEvent ─────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        MasterSavedData data = MasterSavedData.getOrCreate(overworld);

        needVegeta  = data.getVegetaPos()  == null;
        needPiccolo = data.getPiccoloPos() == null;
        needFrieza  = data.getFriezaPos()  == null;

        vegetaCandidate        = null;
        piccoloCandidate       = null;
        playerJoined           = false;
        ticksWaitingForPiccolo = 0;
        overworldQueue.clear();
        namekQueue.clear();
        pendingOverworld = needVegeta || needPiccolo ? overworld : null;

        DMZKiAddon.LOGGER.info("[DMZKiAddon] Spawn state — need Vegeta:{} Piccolo:{} Frieza:{}",
                needVegeta, needPiccolo, needFrieza);
    }

    // ── ChunkEvent.Load — only ENQUEUE, never do biome work here ──────────

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (level.dimension().equals(Level.OVERWORLD) && (needVegeta || needPiccolo)) {
            ChunkAccess chunk = event.getChunk();
            // Encode chunk coords as a long — zero allocation
            overworldQueue.addLast(packChunk(chunk.getPos().x, chunk.getPos().z));
            if (pendingOverworld == null) pendingOverworld = level;
        }

        if (level.dimension().equals(NAMEK) && needFrieza) {
            ChunkAccess chunk = event.getChunk();
            namekQueue.addLast(packChunk(chunk.getPos().x, chunk.getPos().z));
            if (pendingNamek == null) pendingNamek = level;
        }
    }

    // ── Player join — unlock processing ───────────────────────────────────

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        playerJoined = true;
    }

    // ── ServerTickEvent — process chunks within a time budget per tick ────

    /** Max milliseconds to spend processing chunks per tick. Keeps ticks under 2ms extra. */
    private static final long TICK_BUDGET_NS = 2_000_000L; // 2ms

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!playerJoined) return;

        long deadline = System.nanoTime() + TICK_BUDGET_NS;

        // Drain overworld queue within budget
        while ((needVegeta || needPiccolo) && pendingOverworld != null
                && !overworldQueue.isEmpty() && System.nanoTime() < deadline) {
            long packed = overworldQueue.pollFirst();
            evaluateOverworldChunk(pendingOverworld, unpackX(packed), unpackZ(packed));
        }

        // Drain namek queue within budget
        while (needFrieza && pendingNamek != null
                && !namekQueue.isEmpty() && System.nanoTime() < deadline) {
            long packed = namekQueue.pollFirst();
            evaluateNamekChunk(pendingNamek, unpackX(packed), unpackZ(packed));
        }

        // Piccolo fallback timer
        if (needPiccolo && vegetaCandidate != null) {
            ticksWaitingForPiccolo++;
            if (ticksWaitingForPiccolo >= FALLBACK_TICKS) {
                ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    MasterSavedData data = MasterSavedData.getOrCreate(overworld);
                    if (needPiccolo) { // re-check after potential race
                        BlockPos fallback = vegetaCandidate.offset(-200, 0, -200);
                        DMZKiAddon.LOGGER.warn("[DMZKiAddon] Piccolo fallback at {}", fallback);
                        doSpawnPiccolo(overworld, data, fallback);
                        data.setDirty();
                        needPiccolo            = false;
                        ticksWaitingForPiccolo = 0;
                        cleanupIfDone();
                    }
                }
            }
        }
    }

    // ── Chunk evaluation (called from tick, ONE per tick) ──────────────────

    private static void evaluateOverworldChunk(ServerLevel level, int cx, int cz) {
        int bx = (cx << 4) + 8;
        int bz = (cz << 4) + 8;

        // getChunk with false = never force generation, return null if not loaded
        ChunkAccess chunk = level.getChunk(cx, cz,
                net.minecraft.world.level.chunk.ChunkStatus.FULL, false);
        if (chunk == null) return;

        String biome = level.getBiome(new BlockPos(bx, 64, bz))
                .unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("");

        if (!biome.contains("plains")) return;

        int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx & 15, bz & 15);
        if (surfaceY <= 0) return;

        BlockPos candidate = new BlockPos(bx, surfaceY, bz);

        if (needVegeta && vegetaCandidate == null) {
            vegetaCandidate = candidate;
            DMZKiAddon.LOGGER.info("[DMZKiAddon] Vegeta candidate: {}", candidate);
            if (piccoloCandidate != null) finalizeOverworld(level);
            return;
        }

        if (needPiccolo && piccoloCandidate == null && vegetaCandidate != null
                && distSq(candidate, vegetaCandidate) > 100L * 100L) {
            piccoloCandidate = candidate;
            DMZKiAddon.LOGGER.info("[DMZKiAddon] Piccolo candidate: {}", candidate);
            finalizeOverworld(level);
        }
    }

    private static void evaluateNamekChunk(ServerLevel namek, int cx, int cz) {
        ChunkAccess chunk = namek.getChunk(cx, cz,
                net.minecraft.world.level.chunk.ChunkStatus.FULL, false);
        if (chunk == null) return;

        int bx = (cx << 4) + 8;
        int bz = (cz << 4) + 8;
        int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx & 15, bz & 15);
        if (surfaceY <= 0) return;

        ServerLevel overworld = namek.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        MasterSavedData data = MasterSavedData.getOrCreate(overworld);

        needFrieza = false;
        namekQueue.clear();
        doSpawnFrieza(namek, data, new BlockPos(bx, surfaceY, bz));
        data.setDirty();
        cleanupIfDone();
    }

    // ── Finalize Overworld ─────────────────────────────────────────────────

    private static void finalizeOverworld(ServerLevel level) {
        if (!needVegeta && !needPiccolo) return;
        if (needVegeta  && vegetaCandidate  == null) return;
        if (needPiccolo && piccoloCandidate == null) return;

        MasterSavedData data = MasterSavedData.getOrCreate(level);

        if (needVegeta) {
            doSpawnVegeta(level, data, vegetaCandidate);
            needVegeta = false;
        }

        if (needPiccolo && piccoloCandidate != null) {
            doSpawnPiccolo(level, data, piccoloCandidate);
            needPiccolo            = false;
            ticksWaitingForPiccolo = 0;
        }

        data.setDirty();
        cleanupIfDone();
    }

    private static void cleanupIfDone() {
        if (!needVegeta && !needPiccolo) {
            overworldQueue.clear();
            pendingOverworld = null;
        }
        if (!needFrieza) {
            namekQueue.clear();
            pendingNamek = null;
        }
    }

    // ── Spawn helpers ──────────────────────────────────────────────────────

    private static void doSpawnVegeta(ServerLevel level, MasterSavedData data, BlockPos surfacePos) {
        placeStructure(level, RL_VEGETA, surfacePos.offset(-11, 1, -8));
        MasterVegetaEntity entity = ModEntities.MASTER_VEGETA.get().create(level);
        if (entity == null) return;
        entity.moveTo(surfacePos.getX() + 0.5, surfacePos.getY() + 6, surfacePos.getZ() + 0.5, 0f, 0f);
        entity.setPersistenceRequired();
        level.addFreshEntity(entity);
        data.setVegetaPos(surfacePos);
        DMZKiAddon.LOGGER.info("[DMZKiAddon] Vegeta spawned at {}", surfacePos);
        broadcastToAll(level, MSG_VEGETA_SPAWN);
    }

    private static void doSpawnPiccolo(ServerLevel level, MasterSavedData data, BlockPos surfacePos) {
        placeStructure(level, RL_PICCOLO, surfacePos.offset(-6, 1, -6));
        MasterPiccoloEntity entity = ModEntities.MASTER_PICCOLO.get().create(level);
        if (entity == null) return;
        entity.moveTo(surfacePos.getX() + 11, surfacePos.getY() + 2, surfacePos.getZ() + 5, 0f, 0f);
        entity.setPersistenceRequired();
        level.addFreshEntity(entity);
        data.setPiccoloPos(surfacePos);
        DMZKiAddon.LOGGER.info("[DMZKiAddon] Piccolo spawned at {}", surfacePos);
        broadcastToAll(level, MSG_PICCOLO_SPAWN);
    }

    private static void doSpawnFrieza(ServerLevel namek, MasterSavedData data, BlockPos surfacePos) {
        boolean placed = placeStructure(namek, RL_FRIEZA, surfacePos.offset(-12, 10, -11));
        int spawnY = placed ? surfacePos.getY() + 14 : surfacePos.getY();
        MasterFriezaEntity entity = ModEntities.MASTER_FRIEZA.get().create(namek);
        if (entity == null) return;
        entity.moveTo(surfacePos.getX() + 0.5, spawnY + 17, surfacePos.getZ() - 3, 0f, 0f);
        entity.setPersistenceRequired();
        namek.addFreshEntity(entity);
        data.setFriezaPos(new BlockPos(surfacePos.getX(), spawnY, surfacePos.getZ()));
        DMZKiAddon.LOGGER.info("[DMZKiAddon] Frieza spawned on Namek at {}", surfacePos);
        broadcastToAll(namek, MSG_FRIEZA_SPAWN);
    }

    // ── Structure placement ────────────────────────────────────────────────

    private static boolean placeStructure(ServerLevel level, ResourceLocation location, BlockPos origin) {
        try {
            StructureTemplateManager manager = level.getStructureManager();
            Optional<StructureTemplate> opt  = manager.get(location);
            if (opt.isEmpty()) {
                DMZKiAddon.LOGGER.warn("Structure not found: {}", location);
                return false;
            }
            StructureTemplate template = opt.get();
            StructurePlaceSettings settings = new StructurePlaceSettings();
            template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
            Vec3i size = template.getSize();
            DMZKiAddon.LOGGER.info("[DMZKiAddon] Placed '{}' ({}x{}x{}) at {}",
                    location.getPath(), size.getX(), size.getY(), size.getZ(), origin);
            return true;
        } catch (Exception e) {
            DMZKiAddon.LOGGER.error("[DMZKiAddon] Structure error '{}': {}", location.getPath(), e.getMessage());
            return false;
        }
    }

    // ── Utils ──────────────────────────────────────────────────────────────

    private static long packChunk(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    private static String t(String key, Object... args) {
        String raw = net.minecraft.locale.Language.getInstance().getOrDefault(key);
        if (args.length == 0) return raw;
        try { return String.format(raw, args); }
        catch (Exception e) { return raw + " " + Arrays.toString(args); }
    }

    private static void broadcastToAll(ServerLevel level, String langKey) {
        Component msg = Component.translatable(langKey);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(msg);
        }
    }

    private static long distSq(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    // ── Public locate API ──────────────────────────────────────────────────

    public static BlockPos getVegetaPos(ServerLevel overworld) {
        return MasterSavedData.getOrCreate(overworld).getVegetaPos();
    }

    public static BlockPos getPiccoloPos(ServerLevel overworld) {
        return MasterSavedData.getOrCreate(overworld).getPiccoloPos();
    }

    public static BlockPos getFriezaPos(ServerLevel overworld) {
        return MasterSavedData.getOrCreate(overworld).getFriezaPos();
    }
}