package com.dmzkiaddon.network.packets;

import com.dmzkiaddon.config.AddonConfig;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static com.dmzkiaddon.DMZKiAddon.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class HakaiHandler {

    private static final int NPC_HAKAI_DURATION = 60;
    private static final float BAR_MAX = 100f;

    private static final Map<UUID, NpcHakaiData>    NPC_HAKAI    = new HashMap<>();
    private static final Map<UUID, PlayerHakaiData> PLAYER_HAKAI = new HashMap<>();

    public static void startNpcHakai(ServerPlayer attacker, LivingEntity target) {
        if (target instanceof MastersEntity) return;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NPC_HAKAI_DURATION, 10, false, false));
        NPC_HAKAI.put(attacker.getUUID(), new NpcHakaiData(target, attacker));
    }

    public static boolean startPlayerHakai(ServerPlayer attacker, ServerPlayer defender) {
        if (PLAYER_HAKAI.containsKey(attacker.getUUID())) return false;

        float[] bpData = {0f};
        StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(stats -> bpData[0] = stats.getBattlePower());
        float attackerBP = bpData[0];

        float[] defBP = {0f};
        StatsProvider.get(StatsCapability.INSTANCE, defender).ifPresent(stats -> defBP[0] = stats.getBattlePower());
        float defenderBP = defBP[0];

        PlayerHakaiData data = new PlayerHakaiData(attacker.getUUID(), defender.getUUID(), attackerBP, defenderBP);
        PLAYER_HAKAI.put(attacker.getUUID(), data);

        HakaiUpdateS2C.send(attacker, data);
        HakaiUpdateS2C.send(defender, data);
        return true;
    }

    public static void registerKeyPress(ServerPlayer player) {
        PlayerHakaiData data = PLAYER_HAKAI.get(player.getUUID());
        if (data == null) {
            for (PlayerHakaiData d : PLAYER_HAKAI.values()) {
                if (d.defenderId.equals(player.getUUID())) {
                    data = d;
                    break;
                }
            }
        }
        if (data == null || data.finished) return;

        if (player.getUUID().equals(data.attackerId)) {
            data.attackerProgress = Math.min(BAR_MAX, data.attackerProgress + data.attackerSpeedPerPress);
        } else {
            data.defenderProgress = Math.min(BAR_MAX, data.defenderProgress + data.defenderSpeedPerPress);
        }

        ServerPlayer attacker = player.getServer().getPlayerList().getPlayer(data.attackerId);
        ServerPlayer defender = player.getServer().getPlayerList().getPlayer(data.defenderId);
        if (attacker != null) HakaiUpdateS2C.send(attacker, data);
        if (defender != null) HakaiUpdateS2C.send(defender, data);

        if (data.attackerProgress >= BAR_MAX) {
            data.finished = true;
            finishMinigame(data, true, player.getServer());
        } else if (data.defenderProgress >= BAR_MAX) {
            data.finished = true;
            finishMinigame(data, false, player.getServer());
        }
    }

    private static void finishMinigame(PlayerHakaiData data, boolean attackerWon,
                                       net.minecraft.server.MinecraftServer server) {
        ServerPlayer attacker = server.getPlayerList().getPlayer(data.attackerId);
        ServerPlayer defender = server.getPlayerList().getPlayer(data.defenderId);
        ServerPlayer loser = attackerWon ? defender : attacker;

        if (loser != null && loser.level() instanceof ServerLevel serverLevel) {
            // 计算考虑DMZ防御属性后的伤害
            float originalDamage = Float.MAX_VALUE;
            float finalDamage = AddonConfig.calculateDamageWithDefense(originalDamage, loser,
                    StatsProvider.class, StatsCapability.INSTANCE);
            
            // 使用普通伤害源以考虑防御属性
            loser.hurt(serverLevel.damageSources().generic(), finalDamage);
            HakaiUpdateS2C.sendFinished(loser, false);
        }
        if (attacker != null) HakaiUpdateS2C.sendFinished(attacker, attackerWon);
        if (defender != null) HakaiUpdateS2C.sendFinished(defender, !attackerWon);

        PLAYER_HAKAI.remove(data.attackerId);
    }

    public static boolean isInMinigame(UUID playerId) {
        if (PLAYER_HAKAI.containsKey(playerId)) return true;
        return PLAYER_HAKAI.values().stream().anyMatch(d -> d.defenderId.equals(playerId));
    }

    // ── FIX: limpiar minigame si alguien muere durante el cast ────────────
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) return;
        UUID deadId = deadPlayer.getUUID();

        // Caso 1: el atacante murió
        PlayerHakaiData data = PLAYER_HAKAI.get(deadId);
        if (data != null && !data.finished) {
            data.finished = true;
            if (deadPlayer.getServer() != null) {
                ServerPlayer defender = deadPlayer.getServer().getPlayerList().getPlayer(data.defenderId);
                // El defensor sobrevive — mandarle "ganaste" para que la UI se cierre
                if (defender != null) HakaiUpdateS2C.sendFinished(defender, true);
            }
            PLAYER_HAKAI.remove(deadId);
            // También limpiar el NPC hakai que este atacante tenía pendiente
            NPC_HAKAI.remove(deadId);
            return;
        }

        // Caso 2: el defensor murió
        UUID attackerIdToRemove = null;
        for (Map.Entry<UUID, PlayerHakaiData> entry : PLAYER_HAKAI.entrySet()) {
            PlayerHakaiData d = entry.getValue();
            if (d.defenderId.equals(deadId) && !d.finished) {
                d.finished = true;
                if (deadPlayer.getServer() != null) {
                    ServerPlayer attacker = deadPlayer.getServer().getPlayerList().getPlayer(d.attackerId);
                    // El atacante gana automáticamente si el defensor muere
                    if (attacker != null) HakaiUpdateS2C.sendFinished(attacker, true);
                }
                attackerIdToRemove = entry.getKey();
                break;
            }
        }
        if (attackerIdToRemove != null) {
            PLAYER_HAKAI.remove(attackerIdToRemove);
        }

        // Caso 3: el atacante murió y tenía un NPC hakai en curso
        NPC_HAKAI.remove(deadId);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Set<UUID> toRemoveNpc = new HashSet<>();
        for (Map.Entry<UUID, NpcHakaiData> entry : NPC_HAKAI.entrySet()) {
            NpcHakaiData data = entry.getValue();
            data.tick++;
            LivingEntity target = data.target;

            if (!target.isAlive() || data.tick > NPC_HAKAI_DURATION) {
                toRemoveNpc.add(entry.getKey());
                continue;
            }

            if (target.level() instanceof ServerLevel serverLevel) {
                spawnDivineParticles(serverLevel, target);

                if (data.tick == NPC_HAKAI_DURATION) {
                    if (!(target instanceof MastersEntity)) {
                        // 计算考虑DMZ防御属性后的伤害
                        float originalDamage = Float.MAX_VALUE;
                        float finalDamage = AddonConfig.calculateDamageWithDefense(originalDamage, target,
                                StatsProvider.class, StatsCapability.INSTANCE);
                        
                        // 使用普通伤害源以考虑防御属性
                        DamageSource dmg = data.attacker != null
                                ? serverLevel.damageSources().playerAttack(data.attacker)
                                : serverLevel.damageSources().generic();
                        target.invulnerableTime = 0;
                        target.hurt(dmg, finalDamage);
                        if (target.isAlive()) {
                            target.kill();
                        }
                    }
                    toRemoveNpc.add(entry.getKey());
                }
            }
        }
        NPC_HAKAI.keySet().removeAll(toRemoveNpc);

        Set<PlayerHakaiData> processed = new HashSet<>();
        for (PlayerHakaiData data : PLAYER_HAKAI.values()) {
            if (processed.contains(data) || data.finished) continue;
            processed.add(data);
            data.attackerProgress = Math.max(0, data.attackerProgress - 0.3f);
            data.defenderProgress = Math.max(0, data.defenderProgress - 0.3f);
        }
    }

    private static void spawnDivineParticles(ServerLevel level, LivingEntity entity) {
        var divineColor  = new org.joml.Vector3f(0.9f, 0.85f, 0.5f);
        var divineBright = new net.minecraft.core.particles.DustParticleOptions(divineColor, 1.2f);
        for (int i = 0; i < 6; i++) {
            double angle = (level.getGameTime() + i * 60) * 0.3;
            double r = 0.8;
            level.sendParticles(divineBright,
                    entity.getX() + Math.cos(angle) * r,
                    entity.getY() + 1.0 + Math.sin(level.getGameTime() * 0.1 + i),
                    entity.getZ() + Math.sin(angle) * r,
                    1, 0, 0, 0, 0.02);
        }
    }

    static class NpcHakaiData {
        final LivingEntity target;
        final ServerPlayer attacker;
        int tick = 0;
        NpcHakaiData(LivingEntity target, ServerPlayer attacker) {
            this.target = target;
            this.attacker = attacker;
        }
    }

    static class PlayerHakaiData {
        final UUID attackerId;
        final UUID defenderId;
        float attackerProgress;
        float defenderProgress;
        final float attackerSpeedPerPress;
        final float defenderSpeedPerPress;
        boolean finished = false;

        PlayerHakaiData(UUID attackerId, UUID defenderId, float attackerBP, float defenderBP) {
            this.attackerId = attackerId;
            this.defenderId = defenderId;
            float totalBP = attackerBP + defenderBP;
            this.attackerSpeedPerPress = totalBP > 0 ? (attackerBP / totalBP) * 10f : 5f;
            this.defenderSpeedPerPress = totalBP > 0 ? (defenderBP / totalBP) * 10f : 5f;
        }
    }
}