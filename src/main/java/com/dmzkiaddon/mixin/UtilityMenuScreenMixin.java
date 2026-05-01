package com.dmzkiaddon.mixin;

import com.dmzkiaddon.DMZKiAddon;
import com.dmzkiaddon.client.gui.KiAttackMenuSlot;
import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = UtilityMenuScreen.class, remap = false)
public class UtilityMenuScreenMixin {

    @Shadow @Final
    private static List<IUtilityMenuSlot> ADDON_SLOTS;

    @Shadow @Final
    private static List<IUtilityMenuSlot> MENU_SLOTS;

    @Inject(method = "initMenuSlots", at = @At("TAIL"))
    private static void injectAddonSlot(CallbackInfo ci) {
        boolean addonSlotRegistered = ADDON_SLOTS.stream()
                .anyMatch(slot -> slot instanceof KiAttackMenuSlot);

        if (!addonSlotRegistered) {
            IUtilityMenuSlot slot = new KiAttackMenuSlot();
            ADDON_SLOTS.add(slot);
            // Índice 14 = posición {2, 1} = fila abajo, columna más a la derecha
            MENU_SLOTS.set(13, slot);
            DMZKiAddon.LOGGER.info("[DMZKiAddon] KiAttackMenuSlot inyectado en MENU_SLOTS[13]");
        }
    }
}