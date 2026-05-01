package com.dmzkiaddon.compat;

import com.dragonminez.common.init.MainGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Helper para respetar el gamerule allowKiGriefingPlayers/Mobs del DragonMineZ.
 *
 * Uso en cualquier lugar donde el addon cause explosiones o destruya bloques:
 *
 *   Level.ExplosionInteraction mode = KiGriefingHelper.getExplosionMode(level, pos, attackerEntity);
 *   level.explode(null, x, y, z, radius, false, mode);
 */
public final class KiGriefingHelper {

    private KiGriefingHelper() {}

    /**
     * Returns the correct ExplosionInteraction based on the DMZ gamerule.
     * If griefing is disabled, returns NONE (no block damage).
     * If griefing is allowed, returns MOB (standard Ki explosion).
     *
     * @param level   the world
     * @param pos     block position of the explosion center
     * @param source  the entity responsible (player vs mob determines which rule is checked)
     */
    public static Level.ExplosionInteraction getExplosionMode(Level level, BlockPos pos, Entity source) {
        if (MainGameRules.canKiGrief(level, pos, source)) {
            return Level.ExplosionInteraction.MOB;
        }
        return Level.ExplosionInteraction.NONE;
    }

    /**
     * Convenience overload when you have exact XYZ coordinates.
     */
    public static Level.ExplosionInteraction getExplosionMode(Level level, double x, double y, double z, Entity source) {
        return getExplosionMode(level, BlockPos.containing(x, y, z), source);
    }
}