package com.dmzkiaddon.network.packets;

import com.dragonminez.common.stats.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LaunchHellzoneC2S {

    private final int targetEntityId;

    public LaunchHellzoneC2S(int targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public LaunchHellzoneC2S(FriendlyByteBuf buffer) {
        this.targetEntityId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(targetEntityId);
    }

    public static void handle(LaunchHellzoneC2S packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                Entity targetEntity = player.level().getEntity(packet.targetEntityId);
                LivingEntity lockTarget = (targetEntity instanceof LivingEntity le && le.isAlive()) ? le : null;
                float damage = (float) stats.getKiDamage();
                HellzoneHandler.launch(player, damage, lockTarget);
            });
        });
        ctx.setPacketHandled(true);
    }
}
