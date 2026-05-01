package com.dmzkiaddon.entity;

import com.dmzkiaddon.registry.ModSounds;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

public class KiLaserAddon extends KiLaserEntity {

    private Level.ExplosionInteraction explosionInteraction = Level.ExplosionInteraction.MOB;

    public void setExplosionInteraction(Level.ExplosionInteraction mode) {
        this.explosionInteraction = mode;
    }

    public Level.ExplosionInteraction getExplosionInteraction() {
        return explosionInteraction;
    }

    @SuppressWarnings("unchecked")
    public KiLaserAddon(Level level, LivingEntity owner) {
        super((EntityType) MainEntities.KI_LASER.get(), level);
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
            level.playSound(player, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.BASICBEAM_FIRE.get(), SoundSource.PLAYERS,
                    0.6f, 0.9f + level.random.nextFloat() * 0.2f);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setFixedRotation(float yaw, float pitch) {
        try {
            Class<?> clazz = KiLaserEntity.class;
            Field fixedYawField   = null;
            Field fixedPitchField = null;

            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == EntityDataAccessor.class) {
                    String name = f.getName();
                    if (name.equals("FIXED_YAW"))   fixedYawField   = f;
                    if (name.equals("FIXED_PITCH")) fixedPitchField = f;
                }
            }

            if (fixedYawField != null && fixedPitchField != null) {
                EntityDataAccessor<Float> yawAccessor   = (EntityDataAccessor<Float>) fixedYawField.get(null);
                EntityDataAccessor<Float> pitchAccessor = (EntityDataAccessor<Float>) fixedPitchField.get(null);
                this.entityData.set(yawAccessor, yaw);
                this.entityData.set(pitchAccessor, pitch);
            } else {
                // Fallback por orden: BEAM_LENGTH(0), FIXED_YAW(1), FIXED_PITCH(2)
                Field[] fields = clazz.getDeclaredFields();
                int accessorCount = 0;
                for (Field f : fields) {
                    f.setAccessible(true);
                    if (f.getType() == EntityDataAccessor.class) {
                        accessorCount++;
                        if (accessorCount == 2) {
                            EntityDataAccessor<Float> acc = (EntityDataAccessor<Float>) f.get(null);
                            this.entityData.set(acc, yaw);
                        } else if (accessorCount == 3) {
                            EntityDataAccessor<Float> acc = (EntityDataAccessor<Float>) f.get(null);
                            this.entityData.set(acc, pitch);
                        }
                    }
                }
            }
        } catch (Exception e) {
            com.dmzkiaddon.DMZKiAddon.LOGGER.warn("KiLaserAddon: Could not set FIXED_YAW/PITCH via reflection: {}", e.getMessage());
        }
    }
}