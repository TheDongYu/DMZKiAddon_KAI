package com.dmzkiaddon.network.packets;

import com.dmzkiaddon.client.ScreenEffects;
import com.dmzkiaddon.network.AddonNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HakaiUpdateS2C {

    private final float attackerProgress;
    private final float defenderProgress;
    private final boolean finished;
    private final boolean playerWon;

    public HakaiUpdateS2C(float attackerProgress, float defenderProgress, boolean finished, boolean playerWon) {
        this.attackerProgress = attackerProgress;
        this.defenderProgress = defenderProgress;
        this.finished = finished;
        this.playerWon = playerWon;
    }

    public HakaiUpdateS2C(FriendlyByteBuf buffer) {
        this.attackerProgress = buffer.readFloat();
        this.defenderProgress = buffer.readFloat();
        this.finished = buffer.readBoolean();
        this.playerWon = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(attackerProgress);
        buffer.writeFloat(defenderProgress);
        buffer.writeBoolean(finished);
        buffer.writeBoolean(playerWon);
    }

    public static void handle(HakaiUpdateS2C packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (packet.finished) {
                ScreenEffects.stopHakaiMinigame(packet.playerWon);
            } else {
                ScreenEffects.updateHakaiBar(packet.attackerProgress / 100f, packet.defenderProgress / 100f);
            }
        });
        ctx.setPacketHandled(true);
    }

    public static void send(ServerPlayer player, HakaiHandler.PlayerHakaiData data) {
        boolean isAttacker = player.getUUID().equals(data.attackerId);
        float myProg    = isAttacker ? data.attackerProgress : data.defenderProgress;
        float theirProg = isAttacker ? data.defenderProgress : data.attackerProgress;

        HakaiUpdateS2C packet = new HakaiUpdateS2C(myProg, theirProg, false, false);
        AddonNetworkHandler.INSTANCE.sendTo(packet,
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendFinished(ServerPlayer player, boolean won) {
        HakaiUpdateS2C packet = new HakaiUpdateS2C(0, 0, true, won);
        AddonNetworkHandler.INSTANCE.sendTo(packet,
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
    }
}
