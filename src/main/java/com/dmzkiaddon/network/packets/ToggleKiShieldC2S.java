package com.dmzkiaddon.network.packets;

import com.dmzkiaddon.config.AddonConfig;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.stats.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleKiShieldC2S {

    public ToggleKiShieldC2S() {}

    public ToggleKiShieldC2S(FriendlyByteBuf buffer) {}

    public void encode(FriendlyByteBuf buffer) {}

    public static void handle(ToggleKiShieldC2S packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                int currentEnergy = stats.getResources().getCurrentEnergy();
                int cost = AddonConfig.KI_SHIELD_KI_COST.get();
                if (currentEnergy < cost) return;
                stats.getResources().setCurrentEnergy(currentEnergy - cost);

                int kiColor = 0x44AAFF;
                try {
                    String auraColor = stats.getCharacter().getAuraColor();
                    if (!auraColor.isEmpty()) {
                        kiColor = Integer.parseInt(auraColor.replace("#", ""), 16);
                    }
                } catch (NumberFormatException ignored) {}

                KiBarrierEntity barrier = new KiBarrierEntity(player.level(), player);
                barrier.setColors(kiColor, 0xFFFFFF);
                player.level().addFreshEntity(barrier);
            });
        });
        ctx.setPacketHandled(true);
    }
}