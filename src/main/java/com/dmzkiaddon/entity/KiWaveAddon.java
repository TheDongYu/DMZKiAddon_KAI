package com.dmzkiaddon.entity;

import com.dmzkiaddon.registry.ModSounds;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

public class KiWaveAddon extends KiWaveEntity {

    public enum SoundType { KAMEHAMEHA, GALICK_GUN, FINAL_FLASH }

    // ── Caché estático de accessors — se resuelve una sola vez en toda la sesión ──
    // Esto elimina el delay del primer lanzamiento causado por reflection en caliente.
    private static EntityDataAccessor<Float> CACHED_YAW_ACCESSOR   = null;
    private static EntityDataAccessor<Float> CACHED_PITCH_ACCESSOR  = null;
    private static boolean reflectionResolved = false;

    private Level.ExplosionInteraction explosionInteraction = Level.ExplosionInteraction.MOB;

    public void setExplosionInteraction(Level.ExplosionInteraction mode) {
        this.explosionInteraction = mode;
    }

    public Level.ExplosionInteraction getExplosionInteraction() {
        return explosionInteraction;
    }

    @SuppressWarnings("unchecked")
    public KiWaveAddon(Level level, LivingEntity owner, SoundType soundType) {
        super((EntityType) MainEntities.KI_WAVE.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;

        float yaw   = owner.getYHeadRot();
        float pitch = owner.getXRot();

        this.setYRot(yaw);
        this.setXRot(pitch);

        setFixedRotation(yaw, pitch);

        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        Vec3 startPos = owner.getEyePosition().add(look.scale(0.5));
        this.setPos(startPos.x, startPos.y, startPos.z);

        if (owner instanceof net.minecraft.world.entity.player.Player player) {
            net.minecraft.sounds.SoundEvent sound = switch (soundType) {
                case KAMEHAMEHA  -> ModSounds.HAMEHA_FIRE.get();
                case GALICK_GUN  -> ModSounds.FBEAM_1.get();
                case FINAL_FLASH -> ModSounds.FINALFLASH_CHARGE.get();
            };
            level.playSound(player, owner.getX(), owner.getY(), owner.getZ(),
                    sound, SoundSource.PLAYERS, 1.0f,
                    0.9f + level.random.nextFloat() * 0.2f);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void setFixedRotation(float yaw, float pitch) {
        // Si ya resolvimos los accessors en una instancia anterior, reutilizarlos
        if (!reflectionResolved) {
            resolveAccessors();
        }

        if (CACHED_YAW_ACCESSOR != null && CACHED_PITCH_ACCESSOR != null) {
            this.entityData.set(CACHED_YAW_ACCESSOR, yaw);
            this.entityData.set(CACHED_PITCH_ACCESSOR, pitch);
        } else {
            com.dmzkiaddon.DMZKiAddon.LOGGER.warn(
                    "KiWaveAddon: FIXED_YAW/PITCH accessors not found — beam direction may be wrong");
        }
    }

    /**
     * Resuelve los EntityDataAccessors de FIXED_YAW y FIXED_PITCH de KiWaveEntity
     * y los guarda en caché estático. Solo se ejecuta una vez por sesión de juego.
     *
     * KiWaveEntity define 3 EntityDataAccessors en orden:
     *   index 0 → BEAM_LENGTH (Float)
     *   index 1 → FIXED_YAW   (Float)  ← queremos este
     *   index 2 → FIXED_PITCH  (Float)  ← y este
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized void resolveAccessors() {
        if (reflectionResolved) return; // double-check dentro del synchronized

        try {
            Class<?> clazz = KiWaveEntity.class;

            // Intento 1: buscar por nombre (mappings oficiales)
            Field yawField   = null;
            Field pitchField = null;

            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == EntityDataAccessor.class) {
                    String name = f.getName();
                    if (name.equals("FIXED_YAW"))   yawField   = f;
                    if (name.equals("FIXED_PITCH"))  pitchField = f;
                }
            }

            if (yawField != null && pitchField != null) {
                CACHED_YAW_ACCESSOR   = (EntityDataAccessor<Float>) yawField.get(null);
                CACHED_PITCH_ACCESSOR = (EntityDataAccessor<Float>) pitchField.get(null);
            } else {
                // Intento 2: fallback por posición ordinal
                // KiWaveEntity: BEAM_LENGTH(1er), FIXED_YAW(2do), FIXED_PITCH(3er)
                int count = 0;
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType() == EntityDataAccessor.class) {
                        count++;
                        if (count == 2) CACHED_YAW_ACCESSOR   = (EntityDataAccessor<Float>) f.get(null);
                        if (count == 3) CACHED_PITCH_ACCESSOR  = (EntityDataAccessor<Float>) f.get(null);
                    }
                }
            }

            if (CACHED_YAW_ACCESSOR != null && CACHED_PITCH_ACCESSOR != null) {
                com.dmzkiaddon.DMZKiAddon.LOGGER.info("KiWaveAddon: FIXED_YAW/PITCH accessors cached successfully");
            } else {
                com.dmzkiaddon.DMZKiAddon.LOGGER.error("KiWaveAddon: Could not find FIXED_YAW/PITCH accessors");
            }

        } catch (Exception e) {
            com.dmzkiaddon.DMZKiAddon.LOGGER.error("KiWaveAddon: reflection failed: {}", e.getMessage());
        } finally {
            reflectionResolved = true; // no volver a intentarlo aunque falle
        }
    }
}