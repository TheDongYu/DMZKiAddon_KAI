package com.dmzkiaddon.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HakaiKeyPressC2S {

    public HakaiKeyPressC2S() {}

    public HakaiKeyPressC2S(FriendlyByteBuf buffer) {}

    public void encode(FriendlyByteBuf buffer) {}

    public static void handle(HakaiKeyPressC2S packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            HakaiHandler.registerKeyPress(player);
        });
        ctx.setPacketHandled(true);
    }
}
