package com.dmzkiaddon.network;

import com.dmzkiaddon.network.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static com.dmzkiaddon.DMZKiAddon.MOD_ID;

public class AddonNetworkHandler {

    private static final String PROTOCOL = "1";

    public static SimpleChannel INSTANCE;
    private static int packetId = 0;

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(MOD_ID, "network"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();

        INSTANCE.messageBuilder(FireKiAttackC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(FireKiAttackC2S::new)
                .encoder(FireKiAttackC2S::encode)
                .consumerMainThread(FireKiAttackC2S::handle)
                .add();

        INSTANCE.messageBuilder(ToggleKiShieldC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleKiShieldC2S::new)
                .encoder(ToggleKiShieldC2S::encode)
                .consumerMainThread(ToggleKiShieldC2S::handle)
                .add();

        INSTANCE.messageBuilder(SpawnHellzoneC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SpawnHellzoneC2S::new)
                .encoder(SpawnHellzoneC2S::encode)
                .consumerMainThread(SpawnHellzoneC2S::handle)
                .add();

        INSTANCE.messageBuilder(LaunchHellzoneC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(LaunchHellzoneC2S::new)
                .encoder(LaunchHellzoneC2S::encode)
                .consumerMainThread(LaunchHellzoneC2S::handle)
                .add();

        INSTANCE.messageBuilder(InitiateHakaiC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(InitiateHakaiC2S::new)
                .encoder(InitiateHakaiC2S::encode)
                .consumerMainThread(InitiateHakaiC2S::handle)
                .add();

        INSTANCE.messageBuilder(HakaiKeyPressC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(HakaiKeyPressC2S::new)
                .encoder(HakaiKeyPressC2S::encode)
                .consumerMainThread(HakaiKeyPressC2S::handle)
                .add();

        INSTANCE.messageBuilder(HakaiUpdateS2C.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(HakaiUpdateS2C::new)
                .encoder(HakaiUpdateS2C::encode)
                .consumerMainThread(HakaiUpdateS2C::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}