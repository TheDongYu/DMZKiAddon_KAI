package com.dmzkiaddon.client;

import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.AuraParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * Handles client-side particle and sound effects during Ki charge and fire.
 */
@OnlyIn(Dist.CLIENT)
public class ChargeEffects {

    private static final int SOUND_INTERVAL = 20;

    /**
     * Called every tick while the player is charging an attack.
     *
     * @param chargeTicks current charge tick count
     * @param maxCharge   maximum charge ticks
     * @param colorR      red component (0–1)
     * @param colorG      green component (0–1)
     * @param colorB      blue component (0–1)
     */
    public static void onChargeTick(int chargeTicks, int maxCharge, float colorR, float colorG, float colorB) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        float progress = maxCharge > 0 ? (float) chargeTicks / maxCharge : 0f;

        spawnHandParticles(player, progress, colorR, colorG, colorB);
        spawnAura(player, colorR, colorG, colorB, progress);
        spawnEnergyOrbs(player, colorR, colorG, colorB);
        playChargeSound(player, progress, chargeTicks);
    }

    private static void spawnHandParticles(Player player, float progress, float r, float g, float b) {
        Level level = player.level();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), 0.8f + progress);
        DustParticleOptions dustWhite = new DustParticleOptions(new Vector3f(1f, 1f, 1f), 0.5f);

        Vec3 rightHand = getHandPosition(player, true);
        Vec3 leftHand  = getHandPosition(player, false);

        int particleCount = 2 + (int)(progress * 4);
        for (int i = 0; i < particleCount; i++) {
            double ox = (Math.random() - 0.5) * 0.3;
            double oy = (Math.random() - 0.5) * 0.3;
            double oz = (Math.random() - 0.5) * 0.3;

            DustParticleOptions chosen = Math.random() < 0.7 ? dust : dustWhite;
            level.addParticle(chosen, rightHand.x + ox, rightHand.y + oy, rightHand.z + oz, 0, 0, 0);
            level.addParticle(chosen, leftHand.x + ox,  leftHand.y + oy,  leftHand.z + oz,  0, 0, 0);
        }
    }

    private static void spawnAura(Player player, float r, float g, float b, float progress) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int auraCount = 1 + (int)(progress * 3);
        for (int i = 0; i < auraCount; i++) {
            Particle auraP = mc.particleEngine.createParticle(
                    (net.minecraft.core.particles.ParticleOptions) MainParticles.AURA.get(),
                    player.getX(), player.getY(), player.getZ(), 0, 0, 0);

            if (auraP instanceof AuraParticle ap) {
                ap.resize(0.5f + progress * 1.5f);
                float heightOffset = (float)(Math.random() * 2.0);
                ap.setPos(player.getX(), player.getY() + heightOffset, player.getZ());
            }
        }
    }

    private static void spawnEnergyOrbs(Player player, float r, float g, float b) {
        Level level = player.level();
        int orbCount = 3;
        long time = level.getGameTime();

        for (int i = 0; i < orbCount; i++) {
            float angle = (float) Math.toRadians((time * 15 + i * (360.0 / orbCount)) % 360);
            float radius = 0.8f;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = Math.sin(time * 0.1 + i) * 0.3;

            double velX = -Math.sin(angle) * 0.05;
            double velZ =  Math.cos(angle) * 0.05;

            level.addParticle(
                    new DustParticleOptions(new Vector3f(r, g, b), 0.6f),
                    player.getX() + offsetX,
                    player.getY() + 1.0 + offsetY,
                    player.getZ() + offsetZ,
                    velX, 0, velZ);
        }
    }

    private static void playChargeSound(Player player, float progress, int chargeTicks) {
        if (chargeTicks % SOUND_INTERVAL == 0) {
            float volume = 0.3f + progress * 0.5f;
            float pitch  = 0.8f + progress * 0.6f;
            player.level().playSound(player,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.PLAYERS,
                    volume, pitch);
        }
    }

    /** Spawn a burst of particles when the attack fires. */
    public static void onFireAttack(float r, float g, float b, float chargeLevel) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        Level level = player.level();
        int burstCount = 10 + (int)(chargeLevel * 15);

        for (int i = 0; i < burstCount; i++) {
            double speed = 0.1 + Math.random() * 0.3;
            double angle = Math.random() * Math.PI * 2;
            double velX = Math.cos(angle) * speed;
            double velZ = Math.sin(angle) * speed;
            double velY = Math.random() * 0.4;

            level.addParticle(
                    new DustParticleOptions(new Vector3f(r, g, b), 1.2f),
                    player.getX(), player.getY() + player.getEyeHeight(), player.getZ(),
                    velX, velY, velZ);
        }
    }

    /** Returns an approximate world position for the player's hand. */
    private static Vec3 getHandPosition(Player player, boolean rightHand) {
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        double side = rightHand ? 0.4 : -0.4;
        return player.getEyePosition().add(
                look.x * 0.5 + right.x * side,
                -0.2,
                look.z * 0.5 + right.z * side);
    }
}
