package com.dmzkiaddon.entity;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MakankosappoEntity extends KiLaserEntity {

    private static final Vector3f COLOR_PURPLE = new Vector3f(0.5f, 0.0f, 1.0f);
    private static final Vector3f COLOR_WHITE  = new Vector3f(0.85f, 0.85f, 1.0f);

    private int spiralTick = 0;

    @SuppressWarnings("unchecked")
    public MakankosappoEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_WAVE.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;

        float yaw   = owner.getYHeadRot();
        float pitch = owner.getXRot();

        this.setYRot(yaw);
        this.setXRot(pitch);

        Vec3 look  = Vec3.directionFromRotation(pitch, yaw);
        Vec3 start = owner.getEyePosition().add(look.scale(1.0));
        this.setPos(start.x, start.y, start.z);
        this.setDeltaMovement(look.scale(2.5));
    }

    @Override
    public void tick() {
        super.tick();
        spiralTick++;

        Level currentLevel = this.level();
        if (!(currentLevel instanceof ServerLevel serverLevel)) return;

        Vec3 pos      = this.position();
        Vec3 velocity = this.getDeltaMovement();
        double speed  = velocity.length();
        if (speed < 1e-6) return;

        Vec3 dir     = velocity.scale(1.0 / speed);
        Vec3 worldUp = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 perp1   = cross(dir, worldUp).normalize();
        Vec3 perp2   = cross(dir, perp1).normalize();

        double radius = 0.35;

        for (int strand = 0; strand < 2; strand++) {
            double angle = Math.toRadians(spiralTick * 25.0 + strand * 180.0);
            double cosA  = Math.cos(angle);
            double sinA  = Math.sin(angle);

            Vec3 offset = new Vec3(
                    perp1.x * cosA * radius + perp2.x * sinA * radius,
                    perp1.y * cosA * radius + perp2.y * sinA * radius,
                    perp1.z * cosA * radius + perp2.z * sinA * radius
            );

            serverLevel.sendParticles(
                    new DustParticleOptions(strand == 0 ? COLOR_PURPLE : COLOR_WHITE, 0.8f),
                    pos.x + offset.x,
                    pos.y + offset.y,
                    pos.z + offset.z,
                    1, 0, 0, 0, 0
            );
        }
    }

    private static Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }
}