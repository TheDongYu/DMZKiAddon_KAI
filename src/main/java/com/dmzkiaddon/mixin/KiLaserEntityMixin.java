package com.dmzkiaddon.mixin;

import com.dmzkiaddon.compat.KiGriefingHelper;
import com.dmzkiaddon.config.AddonConfig;
import com.dmzkiaddon.entity.KiLaserAddon;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = KiLaserEntity.class, remap = false)
public abstract class KiLaserEntityMixin {

    @Inject(
            method = "explodeAndDie",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onExplodeAndDie(Vec3 pos, CallbackInfo ci) {
        KiLaserEntity self = (KiLaserEntity)(Object) this;
        Level level = self.level();

        if (level.isClientSide) return;

        ci.cancel();

        float radius = self.getSize();
        AABB area = new AABB(pos, pos).inflate(radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity target : entities) {
            if (!self.shouldDamage(target)) continue;
            double dist = target.distanceToSqr(pos);
            if (dist <= radius * radius) {
                float finalDamage = AddonConfig.calculateDamageWithDefense(self.getKiDamage(), target,
                        com.dragonminez.common.stats.StatsProvider.class,
                        com.dragonminez.common.stats.StatsCapability.INSTANCE);
                net.minecraft.world.damagesource.DamageSource dmgSource;
                if (self.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
                    dmgSource = level.damageSources().playerAttack(player);
                } else if (self.getOwner() instanceof LivingEntity living) {
                    dmgSource = level.damageSources().mobAttack(living);
                } else {
                    dmgSource = level.damageSources().generic();
                }
                target.hurt(dmgSource, finalDamage);
            }
        }

        // Partícula y sonido
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1.0, 0.0, 0.0);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        Level.ExplosionInteraction mode;
        if (self instanceof KiLaserAddon addon) {
            mode = addon.getExplosionInteraction();
        } else {
            mode = KiGriefingHelper.getExplosionMode(level, pos.x, pos.y, pos.z, self.getOwner());
        }

        level.explode(
                self,
                self.damageSources().explosion(self, self.getOwner()),
                null,
                pos.x, pos.y, pos.z,
                radius, false, mode, false
        );

        self.discard();
    }
}