package com.dmzkiaddon.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles all client-side screen effects: camera shake, flash overlay,
 * HUD charge bar, and Hakai minigame bar.
 */
@OnlyIn(Dist.CLIENT)
public class ScreenEffects {

    // --- Camera Shake ---
    private static int shakeTimer = 0;
    private static int shakeDuration = 0;
    private static float shakeIntensity = 0f;

    // --- Charge HUD ---
    private static boolean isCharging = false;
    private static float chargeProgress = 0f;
    private static float chargeR = 1f, chargeG = 1f, chargeB = 1f;
    private static String attackName = "";
    private static int baseCost = 0;

    // --- Flash overlay ---
    private static int flashTimer = 0;
    private static float flashAlpha = 0f;

    // --- Hakai minigame ---
    private static boolean hakaiActive = false;
    private static float hakaiMyProgress = 0f;
    private static float hakaiEnemyProgress = 0f;
    private static int hakaiFlashTimer = 0;
    private static boolean hakaiWon = false;

    // ===================== Public API =====================

    public static void triggerFlash(float alpha, int duration) {
        flashAlpha = Math.max(flashAlpha, alpha);
        flashTimer = duration;
    }

    public static void triggerShake(int intensity, int duration) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeDuration = duration;
        shakeTimer = duration;
    }

    public static void setCharging(boolean charging, float progress, float r, float g, float b, String name, int kiBaseCost) {
        isCharging = charging;
        chargeProgress = progress;
        chargeR = r;
        chargeG = g;
        chargeB = b;
        attackName = name;
        baseCost = kiBaseCost;
    }

    public static void stopCharging() {
        isCharging = false;
        attackName = "";
        chargeProgress = 0f;
    }

    public static void updateHakaiBar(float myProgress, float enemyProgress) {
        hakaiActive = true;
        hakaiMyProgress = myProgress;
        hakaiEnemyProgress = enemyProgress;
    }

    public static void stopHakaiMinigame(boolean won) {
        hakaiActive = false;
        hakaiWon = won;
        hakaiFlashTimer = 40;
    }

    public static boolean isHakaiActive() {
        return hakaiActive;
    }

    // ===================== Tick =====================

    public static void tick() {
        if (shakeTimer > 0) shakeTimer--;
        if (flashTimer > 0) flashTimer--;
        if (hakaiFlashTimer > 0) hakaiFlashTimer--;
    }

    // ===================== Events =====================

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.level().isClientSide()) return;

        if (shakeTimer > 0) {
            double time = mc.player.level().getGameTime() + event.getPartialTick();
            float progress = (float) shakeTimer / shakeDuration;
            float amp = shakeIntensity * progress;
            event.setYaw((float) (event.getYaw() + Math.sin(time * 1.8) * amp));
            event.setPitch((float) (event.getPitch() + Math.cos(time * 2.2) * amp * 0.5));
            event.setRoll((float) (event.getRoll() + Math.sin(time * 3.0) * amp * 0.3));
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Window window = mc.getWindow();
        int screenW = window.getGuiScaledWidth();
        int screenH = window.getGuiScaledHeight();
        GuiGraphics gfx = event.getGuiGraphics();

        // Flash overlay (Taiyoken)
        if (flashTimer > 0) {
            float fadeProgress = (float) flashTimer / 20f;
            float fade = Math.min(1f, fadeProgress);
            int color = net.minecraft.util.FastColor.ARGB32.color((int)(flashAlpha * fade * 255), 255, 255, 255);
            gfx.fill(0, 0, screenW, screenH, color);
        }

        // Hakai minigame bar
        if (hakaiActive) {
            renderHakaiBar(gfx, mc, screenW, screenH);
        }

        // Charge HUD bar
        if (isCharging && !attackName.isEmpty()) {
            int hudX = screenW / 2 - 60;
            int barY = screenH - 40;
            int barW = 120;
            int barH = 8;
            int fillW = (int)(barW * chargeProgress);

            int colorAtaque = net.minecraft.util.FastColor.ARGB32.color(200,
                    (int)(chargeR * 255), (int)(chargeG * 255), (int)(chargeB * 255));

            // Background
            gfx.fill(hudX, barY, hudX + barW, barY + barH, 0xAA000000);
            // Fill
            gfx.fill(hudX, barY, hudX + fillW, barY + barH, colorAtaque);

            // Attack name
            gfx.drawString(mc.font, Component.literal(attackName), hudX, barY - 12, colorAtaque, true);

            // Ki cost
            int currentCost = (int)(baseCost * (1f + chargeProgress));
            gfx.drawString(mc.font, Component.literal("Ki: " + currentCost),
                    hudX + barW + 4, barY, 0xFFFFFFFF, true);
        }
    }

    private static void renderHakaiBar(GuiGraphics gfx, Minecraft mc, int screenW, int screenH) {
        int barTotalW = 200;
        int barX = screenW / 2 - barTotalW / 2;
        int barY = screenH / 2 - 30;
        int barH = 10;
        int halfW = barTotalW / 2;

        // Background
        gfx.fill(barX, barY, barX + barTotalW, barY + barH, 0xAA000000);

        // My progress (blue, left side)
        int myFill = (int)(halfW * hakaiMyProgress);
        gfx.fill(barX, barY, barX + myFill, barY + barH, 0xFF3399FF);

        // Enemy progress (red, right side)
        int enemyBarX = barX + halfW;
        int enemyFill = (int)(halfW * hakaiEnemyProgress);
        gfx.fill(enemyBarX + halfW - enemyFill, barY, enemyBarX + halfW, barY + barH, 0xFFFF3333);

        // Label
        String label = Component.translatable("msg.dmzkiaddon.hakai_label").getString();
        int labelW = mc.font.width(label);
        gfx.drawString(mc.font, Component.literal(label), screenW / 2 - labelW / 2, barY - 14, 0xFFFFD700, true);

        // Hint
        String hint  = Component.translatable("msg.dmzkiaddon.hakai_hint").getString();
        int hintW = mc.font.width(hint);
        gfx.drawString(mc.font, Component.literal(hint), screenW / 2 - hintW / 2, barY + barH + 4, 0xFFFFFFFF, true);
    }

    public static void updateHUD(int cooldown, float r, float g, float b, String name, int cost) {
        // Called from KeyHandler to pass HUD data each tick while charging
        chargeR = r;
        chargeG = g;
        chargeB = b;
        attackName = name;
        baseCost = cost;
    }
}
