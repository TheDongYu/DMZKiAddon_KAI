package com.dmzkiaddon.registry;

import com.dmzkiaddon.network.packets.FireKiAttackC2S.AttackType;
import java.util.*;

public class AttackRegistry {

    public record KiAttackEntry(
            AttackType type,
            String id,
            String displayName,
            float colorR, float colorG, float colorB,
            int baseCost,
            int cooldownTicks,
            boolean isCharged,
            boolean requiresSkill,
            boolean isSpecial
    ) {
        // Constructor sin isSpecial — por defecto false (ataque normal ciclo KEY_FIRE)
        public KiAttackEntry(AttackType type, String id, String displayName,
                             float colorR, float colorG, float colorB,
                             int baseCost, int cooldownTicks,
                             boolean isCharged, boolean requiresSkill) {
            this(type, id, displayName, colorR, colorG, colorB,
                    baseCost, cooldownTicks, isCharged, requiresSkill, false);
        }
    }

    public static final List<KiAttackEntry> ALL = List.of(
            // ── Ataques normales (ciclo KEY_FIRE) ─────────────────────────────────
            // KI_LASER: beam tipo rayo, cargable (más tiempo = más daño)
            new KiAttackEntry(AttackType.KI_LASER,         "addon_ki_laser",         "Ki Laser",         0.0f, 1.0f, 1.0f,  10,  10, true,  true),
            // DODOMPA: beam cargado rosado
            new KiAttackEntry(AttackType.DODOMPA,          "addon_dodompa",          "Dodompa",          1.0f, 0.4f, 0.8f,  10,  60, true,  true),
            // KI_VOLLEY: ráfaga de 5 bolas, no cargable
            new KiAttackEntry(AttackType.KI_VOLLEY,        "addon_ki_volley",        "Ki Volley",        1.0f, 1.0f, 0.3f,  20,  20, false, true),
            // MASENKO: beam cargado naranja, disparo rápido
            new KiAttackEntry(AttackType.MASENKO,          "addon_masenko",          "Masenko",          1.0f, 0.6f, 0.0f,  30, 160, true,  true),
            // KI_DISC: proyectil disco, no cargable
            new KiAttackEntry(AttackType.KI_DISC,          "addon_ki_disc",          "Kienzan",          1.0f, 0.8f, 1.0f,  30,  30, false, true),
            // TAIYOKEN: AoE ceguera + lentitud — tecla dedicada (isSpecial=true, no va al ciclo KEY_FIRE)
            new KiAttackEntry(AttackType.TAIYOKEN, "addon_taiyoken", "Taiyoken",
                    1.0f, 1.0f, 1.0f, 20, 120, false, true, true),
            // GALICK_GUN: beam cargado morado
            new KiAttackEntry(AttackType.GALICK_GUN,       "addon_galick_gun",       "Galick Gun",       0.6f, 0.1f, 0.9f,  40, 220, true,  true),
            // KAMEHAMEHA: beam cargado azul
            new KiAttackEntry(AttackType.KAMEHAMEHA,       "addon_kamehameha",       "Kamehameha",       0.2f, 0.5f, 1.0f,  40, 200, true,  true),
            // MAKANKOSAPPO: beam espiral cargado
            new KiAttackEntry(AttackType.MAKANKOSAPPO,     "addon_makankosappo",     "Makankosappo",     0.3f, 0.3f, 1.0f,  50, 240, true,  true),
            // BIG_BANG: bola cargada blanca
            new KiAttackEntry(AttackType.BIG_BANG,         "addon_big_bang",         "Big Bang Attack",  1.0f, 1.0f, 1.0f,  60, 260, true,  true),
            // SPIRIT_BOMB: bola cargada celeste, más daño con más carga
            new KiAttackEntry(AttackType.SPIRIT_BOMB,      "addon_spirit_bomb",      "Spirit Bomb",      0.3f, 0.6f, 1.0f,  80, 400, true,  true),
            // DEATH_BALL: bola cargada roja grande
            new KiAttackEntry(AttackType.DEATH_BALL,       "addon_death_ball",       "Death Ball",       0.8f, 0.0f, 0.1f,  70, 280, true,  true),
            // FINAL_FLASH: beam cargado dorado, gran tamaño
            new KiAttackEntry(AttackType.FINAL_FLASH,      "addon_final_flash",      "Final Flash",      1.0f, 0.9f, 0.1f,  70, 300, true,  true),
            // FINAL_KAMEHAMEHA: doble beam azul+dorado cargado
            new KiAttackEntry(AttackType.FINAL_KAMEHAMEHA, "addon_final_kamehameha", "Final Kamehameha", 0.2f, 0.5f, 1.0f, 100, 400, true,  true),

            // ── Ataques especiales — tecla dedicada, NO van al ciclo KEY_FIRE ─────
            new KiAttackEntry(AttackType.HELLZONE, "addon_hellzone", "Hellzone Grenade",
                    0.9f, 0.1f, 0.1f, 50, 100, false, true, true),
            new KiAttackEntry(AttackType.HAKAI, "addon_hakai", "Hakai",
                    0.7f, 0.0f, 0.9f, 100, 200, false, true, true)
    );

    // Ataques que van al ciclo KEY_FIRE (todos menos HELLZONE y HAKAI)
    public static final List<KiAttackEntry> NORMAL = ALL.stream()
            .filter(e -> !e.isSpecial())
            .toList();

    private static final Map<String, KiAttackEntry> BY_ID = new LinkedHashMap<>();

    static {
        for (KiAttackEntry e : ALL) BY_ID.put(e.id(), e);
    }

    public static Optional<KiAttackEntry> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<KiAttackEntry> byType(AttackType type) {
        return ALL.stream().filter(e -> e.type() == type).findFirst();
    }
}