package com.dmzkiaddon.network.packets;

import com.dmzkiaddon.compat.KiGriefingHelper;
import com.dmzkiaddon.entity.HellzoneGrenadeEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class HellzoneHandler {

    private static final int   MAX_GRENADES   = 8;
    private static final float ORBIT_RADIUS   = 4.5f;  // antes 2.5f — más separado del jugador
    private static final float NO_TARGET_DIST = 15.0f; // distancia del punto ficticio si no hay lock-on

    private static final Map<UUID, List<HellzoneGrenadeEntity>> ORBITING     = new HashMap<>();
    private static final Map<UUID, LivingEntity>                LOCK_TARGETS = new HashMap<>();

    public static void spawnGrenade(ServerPlayer player, float damage, LivingEntity lockTarget) {
        UUID id = player.getUUID();
        ORBITING.putIfAbsent(id, new ArrayList<>());
        List<HellzoneGrenadeEntity> list = ORBITING.get(id);

        list.removeIf(g -> !g.isAlive());
        if (list.size() >= MAX_GRENADES) return;

        if (lockTarget != null) LOCK_TARGETS.put(id, lockTarget);

        boolean[] hasKi = {false};
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            int energy = stats.getResources().getCurrentEnergy();
            int cost = 5;
            if (energy < cost) return;
            stats.getResources().setCurrentEnergy(energy - cost);
            hasKi[0] = true;
        });
        if (!hasKi[0]) return;

        HellzoneGrenadeEntity grenade = new HellzoneGrenadeEntity(player.level(), player);
        grenade.setKiDamage(damage);
        grenade.setPos(player.getX(), player.getY() + 1.0, player.getZ());
        grenade.setOrbitParams(list.size(), list.size() + 1, ORBIT_RADIUS, player);
        player.level().addFreshEntity(grenade);
        list.add(grenade);

        int total = list.size();
        for (int i = 0; i < total; i++) {
            list.get(i).setOrbitParams(i, total, ORBIT_RADIUS, player);
        }
    }

    public static void launch(ServerPlayer player, float damage, LivingEntity lockTarget) {
        UUID id = player.getUUID();
        List<HellzoneGrenadeEntity> list = ORBITING.getOrDefault(id, Collections.emptyList());
        list.removeIf(g -> !g.isAlive());

        if (list.isEmpty()) {
            ORBITING.remove(id);
            LOCK_TARGETS.remove(id);
            return;
        }

        Level level = player.level();

        // Con lock-on: converger al target real
        LivingEntity target = lockTarget != null ? lockTarget : LOCK_TARGETS.get(id);
        if (target != null && target.isAlive()) {
            for (HellzoneGrenadeEntity grenade : list) {
                Level.ExplosionInteraction explosionMode = KiGriefingHelper.getExplosionMode(
                        level, grenade.getX(), grenade.getY(), grenade.getZ(), player);
                grenade.setExplosionInteraction(explosionMode);
                grenade.converge(target, damage);
            }
        } else {
            // Sin lock-on: crear entidad fantasma en el punto frente al jugador
            // y converger las granadas hacia ese punto en el mundo
            Vec3 look = player.getLookAngle();
            Vec3 point = player.getEyePosition().add(look.scale(NO_TARGET_DIST));
            for (HellzoneGrenadeEntity grenade : list) {
                Level.ExplosionInteraction explosionMode = KiGriefingHelper.getExplosionMode(
                        level, grenade.getX(), grenade.getY(), grenade.getZ(), player);
                grenade.setExplosionInteraction(explosionMode);
                grenade.convergeToPoint(point, damage);
            }
        }

        list.clear();
        ORBITING.remove(id);
        LOCK_TARGETS.remove(id);
    }

    public static void cancel(ServerPlayer player) {
        UUID id = player.getUUID();
        List<HellzoneGrenadeEntity> list = ORBITING.getOrDefault(id, Collections.emptyList());
        for (HellzoneGrenadeEntity g : list) g.discard();
        list.clear();
        ORBITING.remove(id);
        LOCK_TARGETS.remove(id);
    }

    public static boolean hasGrenades(ServerPlayer player) {
        List<HellzoneGrenadeEntity> list = ORBITING.get(player.getUUID());
        return list != null && !list.isEmpty() && list.stream().anyMatch(HellzoneGrenadeEntity::isAlive);
    }

    public static int getCount(ServerPlayer player) {
        List<HellzoneGrenadeEntity> list = ORBITING.get(player.getUUID());
        if (list == null) return 0;
        list.removeIf(g -> !g.isAlive());
        return list.size();
    }
}