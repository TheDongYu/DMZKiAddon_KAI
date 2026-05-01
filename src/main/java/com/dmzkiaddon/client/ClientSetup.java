package com.dmzkiaddon.client;

import com.dmzkiaddon.client.renderer.MasterFriezaRenderer;
import com.dmzkiaddon.client.renderer.MasterPiccoloRenderer;
import com.dmzkiaddon.client.renderer.MasterVegetaRenderer;
import com.dmzkiaddon.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.dmzkiaddon.DMZKiAddon.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    public static final String CATEGORY = "key.categories.dmzkiaddon";

    public static final KeyMapping KEY_FIRE         = registerKey("fire",         InputConstants.KEY_R);
    public static final KeyMapping KEY_ATTACK_PREV  = registerKey("attack_prev",  InputConstants.KEY_LBRACKET);
    public static final KeyMapping KEY_ATTACK_NEXT  = registerKey("attack_next",  InputConstants.KEY_RBRACKET);
    public static final KeyMapping KEY_KI_SHIELD    = registerKey("ki_shield",    -1);
    public static final KeyMapping KEY_HELLZONE     = registerKey("hellzone",     -1);
    public static final KeyMapping KEY_HAKAI_SPAM   = registerKey("hakai_spam",   InputConstants.KEY_SPACE);
    public static final KeyMapping KEY_HAKAI        = registerKey("hakai",        -1);
    public static final KeyMapping KEY_TAIYOKEN     = registerKey("taiyoken",     -1);

    private static KeyMapping registerKey(String name, int defaultKeyCode) {
        return new KeyMapping(
                "key.dmzkiaddon." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                defaultKeyCode,
                CATEGORY
        );
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY_FIRE);
        event.register(KEY_ATTACK_PREV);
        event.register(KEY_ATTACK_NEXT);
        event.register(KEY_KI_SHIELD);
        event.register(KEY_HELLZONE);
        event.register(KEY_HAKAI_SPAM);
        event.register(KEY_HAKAI);
        event.register(KEY_TAIYOKEN);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MASTER_VEGETA.get(),  MasterVegetaRenderer::new);
        event.registerEntityRenderer(ModEntities.MASTER_PICCOLO.get(), MasterPiccoloRenderer::new);
        event.registerEntityRenderer(ModEntities.MASTER_FRIEZA.get(),  MasterFriezaRenderer::new);
    }
}