package com.dmzkiaddon.network.packets;

import com.dmzkiaddon.config.AddonConfig;
import com.dmzkiaddon.compat.KiGriefingHelper;
import com.dmzkiaddon.entity.KiLaserAddon;
import com.dmzkiaddon.entity.KiWaveAddon;
import com.dmzkiaddon.entity.MakankosappoEntity;
import com.dmzkiaddon.registry.ModSounds;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiDiscEntity;
import com.dragonminez.common.stats.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class FireKiAttackC2S {

    public enum AttackType {
        KAMEHAMEHA, GALICK_GUN, FINAL_FLASH, KI_VOLLEY, KI_DISC, KI_LASER,
        SPIRIT_BOMB, BIG_BANG, DEATH_BALL, MASENKO, DODOMPA, MAKANKOSAPPO,
        TAIYOKEN, FINAL_KAMEHAMEHA, HELLZONE, HAKAI
    }

    private final AttackType attackType;
    private final float chargeLevel;

    public FireKiAttackC2S(AttackType attackType, float chargeLevel) {
        this.attackType = attackType;
        this.chargeLevel = chargeLevel;
    }

    public FireKiAttackC2S(FriendlyByteBuf buffer) {
        this.attackType = buffer.readEnum(AttackType.class);
        this.chargeLevel = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(attackType);
        buffer.writeFloat(chargeLevel);
    }

    public static void handle(FireKiAttackC2S packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats ->
                    fireAttack(packet.attackType, player, stats, packet.chargeLevel));
        });
        ctx.setPacketHandled(true);
    }

    private static void fireAttack(AttackType type, ServerPlayer player, StatsData stats, float charge) {
        int maxEnergy     = stats.getMaxEnergy();
        int currentEnergy = stats.getResources().getCurrentEnergy();

        int attackCost = (int)(maxEnergy * (AddonConfig.getCostPercentage(type) / 100f) * (1f + charge));
        if (currentEnergy < attackCost) return;
        stats.getResources().setCurrentEnergy(currentEnergy - attackCost);

        Level level      = player.level();
        Vec3 lookAngle   = player.getLookAngle();
        float damageMult = 1f + charge;
        float sizeMult   = 1f + charge * 0.5f;

        float kiDamage = (float) stats.getKiDamage() * damageMult * AddonConfig.getDamageScale(type);

        switch (type) {
            case KAMEHAMEHA -> {
                KiWaveAddon wave = new KiWaveAddon(level, player, KiWaveAddon.SoundType.KAMEHAMEHA);
                wave.setColors(0x2266FF, 0x88BBFF);
                wave.setKiDamage(kiDamage);
                wave.setSize(sizeMult);
                wave.setKiSpeed(1.5f);
                wave.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(wave);
            }
            case GALICK_GUN -> {
                KiWaveAddon wave = new KiWaveAddon(level, player, KiWaveAddon.SoundType.GALICK_GUN);
                wave.setColors(0x9922CC, 0xDD88FF);
                wave.setKiDamage(kiDamage * 1.1f);
                wave.setSize(sizeMult);
                wave.setKiSpeed(1.4f);
                wave.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(wave);
            }
            case FINAL_FLASH -> {
                KiWaveAddon wave = new KiWaveAddon(level, player, KiWaveAddon.SoundType.FINAL_FLASH);
                wave.setColors(0xFFDD00, 0xFFFFAA);
                wave.setKiDamage(kiDamage * 1.4f);
                wave.setSize(sizeMult * 1.3f);
                wave.setKiSpeed(1.3f);
                wave.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(wave);
            }
            case MASENKO -> {
                KiWaveAddon wave = new KiWaveAddon(level, player, KiWaveAddon.SoundType.KAMEHAMEHA);
                wave.setColors(0xFF8800, 0xFFCC66);
                wave.setKiDamage(kiDamage);
                wave.setSize(sizeMult * 0.9f);
                wave.setKiSpeed(1.6f);
                wave.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(wave);
            }
            case FINAL_KAMEHAMEHA -> {
                KiWaveAddon waveBlue = new KiWaveAddon(level, player, KiWaveAddon.SoundType.KAMEHAMEHA);
                waveBlue.setColors(0x2266FF, 0x88BBFF);
                waveBlue.setKiDamage(kiDamage * 1.5f);
                waveBlue.setSize(sizeMult * 1.4f);
                waveBlue.setKiSpeed(1.4f);
                waveBlue.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(waveBlue);

                KiWaveAddon waveYellow = new KiWaveAddon(level, player, KiWaveAddon.SoundType.FINAL_FLASH);
                waveYellow.setColors(0xFFDD00, 0xFFFFAA);
                waveYellow.setKiDamage(kiDamage * 1.5f);
                waveYellow.setSize(sizeMult * 1.4f);
                waveYellow.setKiSpeed(1.4f);
                waveYellow.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(waveYellow);
            }
            case KI_LASER -> {
                KiLaserAddon laser = new KiLaserAddon(level, player);
                laser.setColors(0x00FFFF, 0xAAFFFF);
                laser.setKiDamage(kiDamage * 0.6f);
                laser.setKiSpeed(2.0f);
                laser.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(laser);
            }
            case DODOMPA -> {
                KiLaserAddon dodompa = new KiLaserAddon(level, player);
                dodompa.setColors(0xFF44BB, 0xFFAADD);
                dodompa.setKiDamage(kiDamage * 0.8f);
                dodompa.setKiSpeed(1.2f);
                dodompa.setExplosionInteraction(KiGriefingHelper.getExplosionMode(level, player.getX(), player.getY(), player.getZ(), player));
                level.addFreshEntity(dodompa);
            }
            case KI_VOLLEY -> {
                for (int i = 0; i < 5; i++) {
                    Vec3 dir = lookAngle.add(
                            (Math.random() - 0.5) * 0.2,
                            (Math.random() - 0.5) * 0.2,
                            (Math.random() - 0.5) * 0.2);
                    KiBlastEntity blast = new KiBlastEntity(level, player);
                    blast.setColors(0x44FFFF, 0xAAFFFF);
                    blast.setKiDamage(kiDamage * 0.3f);
                    blast.setSize(0.5f);
                    shootProjectile(blast, player, dir, 2.5f);
                    level.addFreshEntity(blast);
                }
            }
            case SPIRIT_BOMB -> {
                KiBlastEntity spirit = new KiBlastEntity(level, player);
                spirit.setColors(0x88BBFF, 0xCCDDFF);
                spirit.setKiDamage(kiDamage * 2.0f * Math.max(charge, 0.1f));
                spirit.setSize(sizeMult * 2f);
                shootProjectile(spirit, player, lookAngle, 1.0f);
                level.addFreshEntity(spirit);
            }
            case BIG_BANG -> {
                KiBlastEntity bigBang = new KiBlastEntity(level, player);
                bigBang.setColors(0xFFFFFF, 0xDDEEFF);
                bigBang.setKiDamage(kiDamage * 1.8f);
                bigBang.setSize(sizeMult * 1.5f);
                shootProjectile(bigBang, player, lookAngle, 1.2f);
                level.addFreshEntity(bigBang);
            }
            case DEATH_BALL -> {
                KiBlastEntity dball = new KiBlastEntity(level, player);
                dball.setColors(0xCC0022, 0xFF6644);
                dball.setKiDamage(kiDamage * 2.0f);
                dball.setSize(sizeMult * 1.8f);
                shootProjectile(dball, player, lookAngle, 1.1f);
                level.addFreshEntity(dball);
            }
            case KI_DISC -> {
                KiDiscEntity disc = new KiDiscEntity(level, player);
                disc.setColors(0xFF44FF, 0xFFAAFF);
                disc.setKiDamage(kiDamage);
                shootProjectile(disc, player, lookAngle, 2.0f);
                level.addFreshEntity(disc);
                player.playSound(ModSounds.DISC_FIRE.get(), 0.6f, 1.0f);
            }
            case MAKANKOSAPPO -> {
                MakankosappoEntity beam = new MakankosappoEntity(level, player);
                beam.setKiDamage(kiDamage * 1.3f);
                beam.setSize(sizeMult * 0.7f);
                beam.setKiSpeed(2.5f);
                level.addFreshEntity(beam);
                player.playSound(ModSounds.BASICBEAM_FIRE.get(), 0.6f, 0.8f);
            }
            case TAIYOKEN -> {
                AABB area = player.getBoundingBox().inflate(10.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                        e -> !e.equals(player) && e.isAlive());
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         (int)(60 * (1f + charge)), 0, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(80 * (1f + charge)), 2, false, false));
                }
            }
            case HELLZONE, HAKAI -> {}
        }
    }

    private static void shootProjectile(Projectile proj, ServerPlayer player, Vec3 direction, float speed) {
        Vec3 spawnPos = player.getEyePosition().add(direction.normalize().scale(0.5));
        proj.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        proj.setOwner(player);
        proj.shoot(direction.x, direction.y, direction.z, speed, 0f);
    }
}