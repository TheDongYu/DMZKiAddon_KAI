package com.dmzkiaddon.network.packets;

import com.dmzkiaddon.config.AddonConfig;
import com.dragonminez.common.stats.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class InitiateHakaiC2S {

    private final int targetEntityId;

    public InitiateHakaiC2S(int targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public InitiateHakaiC2S(FriendlyByteBuf buffer) {
        this.targetEntityId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(targetEntityId);
    }

    public static void handle(InitiateHakaiC2S packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer attacker = ctx.getSender();
            if (attacker == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, attacker).ifPresent(stats -> {
                int energy = stats.getResources().getCurrentEnergy();
                int cost   = (int)(stats.getMaxEnergy() * (AddonConfig.getCostPercentage(FireKiAttackC2S.AttackType.HAKAI) / 100f));
                if (energy < cost) return;

                Entity targetEntity = attacker.level().getEntity(packet.targetEntityId);
                if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) return;

                boolean handledAsPlayer = false;
                if (target instanceof ServerPlayer defender) {
                    handledAsPlayer = HakaiHandler.startPlayerHakai(attacker, defender);
                }

                if (!handledAsPlayer) {
                    HakaiHandler.startNpcHakai(attacker, target);
                }

                stats.getResources().setCurrentEnergy(energy - cost);
            });
        });
        ctx.setPacketHandled(true);
    }
}