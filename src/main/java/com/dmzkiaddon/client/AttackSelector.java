package com.dmzkiaddon.client;

import com.dmzkiaddon.registry.AttackRegistry;
import com.dmzkiaddon.registry.AttackRegistry.KiAttackEntry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side singleton that tracks which Ki attack is currently selected
 * and filters the list to only attacks the player has learned.
 */
@OnlyIn(Dist.CLIENT)
public class AttackSelector {

    private static int selectedIndex = 0;

    private AttackSelector() {}

    /**
     * Returns the list of NORMAL (non-special) attacks the local player has learned.
     * These are the attacks cycled via KEY_FIRE.
     * Falls back to NORMAL if capability unavailable.
     */
    public static List<KiAttackEntry> getLearnedAttacks() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return new ArrayList<>(AttackRegistry.NORMAL);

        List<KiAttackEntry> learned = new ArrayList<>();
        boolean[] capFound = {false};
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            capFound[0] = true;
            for (KiAttackEntry entry : AttackRegistry.NORMAL) {
                var skill = stats.getSkills().getSkill(entry.id());
                if (skill != null && skill.getLevel() > 0) {
                    learned.add(entry);
                }
            }
        });
        if (!capFound[0]) learned.addAll(AttackRegistry.NORMAL);
        return learned;
    }

    /**
     * Returns ALL learned attacks including specials (Hakai, Hellzone).
     * Used for display in the X-menu slot.
     */
    public static List<KiAttackEntry> getAllLearnedAttacks() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return new ArrayList<>(AttackRegistry.ALL);

        List<KiAttackEntry> learned = new ArrayList<>();
        boolean[] capFound = {false};
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            capFound[0] = true;
            for (KiAttackEntry entry : AttackRegistry.ALL) {
                var skill = stats.getSkills().getSkill(entry.id());
                if (skill != null && skill.getLevel() > 0) {
                    learned.add(entry);
                }
            }
        });
        if (!capFound[0]) learned.addAll(AttackRegistry.ALL);
        return learned;
    }

    /**
     * Returns the currently selected attack, or null if the player has no learned attacks.
     */
    public static KiAttackEntry getSelected() {
        List<KiAttackEntry> learned = getLearnedAttacks();
        if (learned.isEmpty()) return null;
        selectedIndex = Math.min(selectedIndex, learned.size() - 1);
        return learned.get(selectedIndex);
    }

    /**
     * Selects an attack by its skill id. No-op if not in the learned list.
     */
    public static void selectById(String id) {
        List<KiAttackEntry> learned = getLearnedAttacks();
        for (int i = 0; i < learned.size(); i++) {
            if (learned.get(i).id().equals(id)) {
                selectedIndex = i;
                return;
            }
        }
    }

    public static void selectNext() {
        List<KiAttackEntry> learned = getLearnedAttacks();
        if (learned.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % learned.size();
    }

    public static void selectPrev() {
        List<KiAttackEntry> learned = getLearnedAttacks();
        if (learned.isEmpty()) return;
        selectedIndex = ((selectedIndex - 1) + learned.size()) % learned.size();
    }

    public static int getSelectedIndex() {
        return selectedIndex;
    }

    /** Returns the index within the learned list for a given skill id, or -1. */
    public static int indexOfId(String id) {
        List<KiAttackEntry> learned = getLearnedAttacks();
        for (int i = 0; i < learned.size(); i++) {
            if (learned.get(i).id().equals(id)) return i;
        }
        return -1;
    }
}