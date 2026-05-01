package com.dmzkiaddon.client.gui;

import com.dmzkiaddon.registry.AttackRegistry.KiAttackEntry;
import com.dmzkiaddon.client.AttackSelector;
import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Slot en el menú X de DragonMineZ.
 * Muestra el ataque Ki seleccionado actualmente y permite ciclarlo con click izquierdo.
 * Los ataques especiales (Hakai, Hellzone) aparecen en el contador ★ y tienen tecla dedicada.
 *
 * Left click  → ciclar al siguiente ataque normal aprendido
 * Right click → sin acción
 */
public class KiAttackMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {

    @Override
    public ButtonInfo render(StatsData statsData) {
        List<KiAttackEntry> allLearned    = AttackSelector.getAllLearnedAttacks();
        List<KiAttackEntry> normalLearned = AttackSelector.getLearnedAttacks();

        if (allLearned.isEmpty()) {
            ButtonInfo empty = new ButtonInfo(
                    Component.translatable("gui.dmzkiaddon.no_attacks").withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.dmzkiaddon.learn_from_master")
            );
            empty.setColor(0xAAAAAA);
            return empty;
        }

        KiAttackEntry selected = AttackSelector.getSelected();

        Component line1;
        if (selected != null) {
            line1 = Component.literal(selected.displayName()).withStyle(ChatFormatting.BOLD);
        } else {
            line1 = Component.literal("Ataques Ki").withStyle(ChatFormatting.BOLD);
        }

        int specialCount = (int) allLearned.stream().filter(KiAttackEntry::isSpecial).count();
        String line2Text;
        if (selected != null) {
            int index = AttackSelector.getSelectedIndex() + 1;
            line2Text = "Ki: " + selected.baseCost() + "  [" + index + "/" + normalLearned.size() + "]";
            if (specialCount > 0) line2Text += "  ★" + specialCount;
        } else {
            line2Text = "★" + specialCount + " especial" + (specialCount != 1 ? "es" : "");
        }

        ButtonInfo info = new ButtonInfo(line1, Component.literal(line2Text), true);

        if (selected != null) {
            int r = (int)(selected.colorR() * 255);
            int g = (int)(selected.colorG() * 255);
            int b = (int)(selected.colorB() * 255);
            info.setColor((r << 16) | (g << 8) | b);
        } else {
            info.setColor(0xAA00AA);
        }

        return info;
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        List<KiAttackEntry> normalLearned = AttackSelector.getLearnedAttacks();
        List<KiAttackEntry> allLearned    = AttackSelector.getAllLearnedAttacks();
        if (allLearned.isEmpty()) return;

        if (!rightClick && !normalLearned.isEmpty()) {
            AttackSelector.selectNext();
            KiAttackEntry selected = AttackSelector.getSelected();
            if (selected != null) {
                net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    int index = AttackSelector.getSelectedIndex() + 1;
                    player.displayClientMessage(
                            Component.empty()
                                    .append(Component.literal("Ataque seleccionado: ").withStyle(ChatFormatting.GRAY))
                                    .append(Component.literal(selected.displayName()).withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(" [" + index + "/" + normalLearned.size() + "]")
                                            .withStyle(ChatFormatting.DARK_GRAY)),
                            true
                    );
                }
            }
        }

        playUiSound();
    }

    private void playUiSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F));
    }
}