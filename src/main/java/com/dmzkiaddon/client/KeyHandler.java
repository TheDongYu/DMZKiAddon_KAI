package com.dmzkiaddon.client;

import com.dmzkiaddon.config.AddonConfig;
import com.dmzkiaddon.registry.AttackRegistry.KiAttackEntry;
import com.dmzkiaddon.network.AddonNetworkHandler;
import com.dmzkiaddon.network.packets.FireKiAttackC2S;
import com.dmzkiaddon.network.packets.FireKiAttackC2S.AttackType;
import com.dmzkiaddon.network.packets.HakaiKeyPressC2S;
import com.dmzkiaddon.network.packets.InitiateHakaiC2S;
import com.dmzkiaddon.network.packets.LaunchHellzoneC2S;
import com.dmzkiaddon.network.packets.SpawnHellzoneC2S;
import com.dmzkiaddon.network.packets.ToggleKiShieldC2S;
import com.dragonminez.client.events.LockOnEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class KeyHandler {

    private static final int MAX_CHARGE              = 100;
    private static final int MIN_CHARGE              = 10;
    private static final int MAX_GRENADES            = 8;
    private static final int HELLZONE_SPAWN_INTERVAL = 8;
    private static final int HELLZONE_MAX_HOLD       = MAX_GRENADES * HELLZONE_SPAWN_INTERVAL;

    private static final Map<AttackType, Integer> cooldownMap = new EnumMap<>(AttackType.class);

    public static int getCooldownFor(AttackType type) {
        return cooldownMap.getOrDefault(type, 0);
    }

    private static int chargeTick = 0;
    private static boolean prevFire     = false;
    private static boolean prevNext     = false;
    private static boolean prevPrev     = false;
    private static boolean prevKiShield = false;

    private static boolean prevHellzone   = false;
    private static int hellzoneHoldTick   = 0;
    private static int cooldownHellzone   = 0;
    private static boolean hellzoneActive = false;

    private static boolean prevHakaiSpam = false;
    private static boolean prevHakai     = false;
    private static int cooldownHakai     = 0;

    private static boolean prevTaiyoken = false;
    private static int cooldownTaiyoken = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        Player player = mc.player;
        ScreenEffects.tick();

        for (AttackType type : AttackType.values()) {
            int cd = cooldownMap.getOrDefault(type, 0);
            if (cd > 0) cooldownMap.put(type, cd - 1);
        }
        if (cooldownHellzone > 0) cooldownHellzone--;
        if (cooldownHakai    > 0) cooldownHakai--;
        if (cooldownTaiyoken > 0) cooldownTaiyoken--;

        boolean curFire      = ClientSetup.KEY_FIRE.isDown();
        boolean curNext      = ClientSetup.KEY_ATTACK_NEXT.isDown();
        boolean curPrev      = ClientSetup.KEY_ATTACK_PREV.isDown();
        boolean curKiShield  = ClientSetup.KEY_KI_SHIELD.isDown();
        boolean curHellzone  = ClientSetup.KEY_HELLZONE.isDown();
        boolean curHakaiSpam = ClientSetup.KEY_HAKAI_SPAM.isDown();
        boolean curHakai     = ClientSetup.KEY_HAKAI.isDown();
        boolean curTaiyoken  = ClientSetup.KEY_TAIYOKEN.isDown();

        if (curNext && !prevNext) {
            AttackSelector.selectNext();
            ScreenEffects.stopCharging();
            chargeTick = 0;
            showSelectedAttack(player);
        }
        if (curPrev && !prevPrev) {
            AttackSelector.selectPrev();
            ScreenEffects.stopCharging();
            chargeTick = 0;
            showSelectedAttack(player);
        }

        KiAttackEntry selected = AttackSelector.getSelected();
        if (selected != null) {
            int cd = getCooldownFor(selected.type());

            if (cd > 0 && curFire && !prevFire) {
                showCooldownMessage(player, selected.displayName(), cd);
            }

            if (cd == 0) {
                if (selected.isCharged()) {
                    if (curFire) {
                        chargeTick = Math.min(chargeTick + 1, MAX_CHARGE);
                        ScreenEffects.setCharging(true, (float) chargeTick / MAX_CHARGE,
                                selected.colorR(), selected.colorG(), selected.colorB(),
                                selected.displayName(), selected.baseCost());
                        ChargeEffects.onChargeTick(chargeTick, MAX_CHARGE,
                                selected.colorR(), selected.colorG(), selected.colorB());
                    }
                    if (!curFire && prevFire) {
                        if (chargeTick >= MIN_CHARGE) {
                            float chargeLevel = (float) chargeTick / MAX_CHARGE;
                            fireAttack(selected, chargeLevel);
                            cooldownMap.put(selected.type(), AddonConfig.getCooldown(selected.type()));
                            ScreenEffects.stopCharging();
                            ScreenEffects.triggerShake(4, 8);
                            ChargeEffects.onFireAttack(selected.colorR(), selected.colorG(), selected.colorB(), chargeLevel);
                        } else {
                            ScreenEffects.stopCharging();
                        }
                        chargeTick = 0;
                    }
                } else {
                    if (curFire && !prevFire) {
                        fireAttack(selected, 1.0f);
                        cooldownMap.put(selected.type(), AddonConfig.getCooldown(selected.type()));
                        ScreenEffects.triggerShake(3, 6);
                        ChargeEffects.onFireAttack(selected.colorR(), selected.colorG(), selected.colorB(), 1.0f);
                    }
                }
            }
        }

        if (!curFire) chargeTick = 0;

        if (curKiShield && !prevKiShield) {
            AddonNetworkHandler.sendToServer(new ToggleKiShieldC2S());
        }

        if (curTaiyoken && !prevTaiyoken) {
            if (hasSkill(player, "addon_taiyoken")) {
                if (cooldownTaiyoken > 0) {
                    showCooldownMessage(player,
                            Component.translatable("attack.dmzkiaddon.taiyoken").getString(),
                            cooldownTaiyoken);
                } else {
                    AddonNetworkHandler.sendToServer(new FireKiAttackC2S(AttackType.TAIYOKEN, 1.0f));
                    cooldownTaiyoken = AddonConfig.getCooldown(AttackType.TAIYOKEN);
                    ScreenEffects.triggerFlash(1.0f, 20);
                    ScreenEffects.triggerShake(3, 6);
                }
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.dmzkiaddon.taiyoken_no_skill")
                                .withStyle(s -> s.withColor(0xFFFF55)),
                        true
                );
            }
        }

        // ── Hellzone Grenade ─────────────────────────────────────────────
        if (hasSkill(player, "addon_hellzone")) {
            if (curHellzone) {
                if (cooldownHellzone > 0) {
                    if (!prevHellzone) {
                        showCooldownMessage(player,
                                Component.translatable("attack.dmzkiaddon.hellzone").getString(),
                                cooldownHellzone);
                    }
                } else {
                    if (!prevHellzone) {
                        AddonNetworkHandler.sendToServer(new SpawnHellzoneC2S(getLockOnTargetId()));
                        hellzoneHoldTick = 0;
                        hellzoneActive = true;
                    }
                    if (hellzoneActive) {
                        hellzoneHoldTick++;
                        if (hellzoneHoldTick % HELLZONE_SPAWN_INTERVAL == 0) {
                            AddonNetworkHandler.sendToServer(new SpawnHellzoneC2S(getLockOnTargetId()));
                        }
                        float progress = Math.min(1.0f, (float) hellzoneHoldTick / HELLZONE_MAX_HOLD);
                        int hellzoneCostDisplay = StatsProvider.get(StatsCapability.INSTANCE, player)
                                .map(s -> (int)(s.getMaxEnergy() * (AddonConfig.HELLZONE_KI_COST_PCT.get() / 100f)))
                                .orElse(0);
                        ScreenEffects.setCharging(true, progress,
                                0.3f, 0.9f, 0.3f,
                                Component.translatable("attack.dmzkiaddon.hellzone").getString(),
                                hellzoneCostDisplay);
                    }
                }
            }

            if (!curHellzone && prevHellzone && hellzoneActive && cooldownHellzone == 0) {
                AddonNetworkHandler.sendToServer(new LaunchHellzoneC2S(getLockOnTargetId()));
                cooldownHellzone = AddonConfig.getCooldown(AttackType.HELLZONE);
                hellzoneHoldTick = 0;
                hellzoneActive = false;
                ScreenEffects.stopCharging();
                ScreenEffects.triggerShake(5, 10);
            }

            if (!curHellzone && prevHellzone && cooldownHellzone > 0) {
                hellzoneHoldTick = 0;
                hellzoneActive = false;
                ScreenEffects.stopCharging();
            }
        }

        if (curHakaiSpam && !prevHakaiSpam && ScreenEffects.isHakaiActive()) {
            AddonNetworkHandler.sendToServer(new HakaiKeyPressC2S());
        }

        if (curHakai && !prevHakai && !ScreenEffects.isHakaiActive()) {
            if (hasSkill(player, "addon_hakai")) {
                if (cooldownHakai > 0) {
                    showCooldownMessage(player,
                            Component.translatable("attack.dmzkiaddon.hakai").getString(),
                            cooldownHakai);
                } else {
                    int targetId = getLockOnTargetId();
                    if (targetId != -1) {
                        AddonNetworkHandler.sendToServer(new InitiateHakaiC2S(targetId));
                        cooldownHakai = AddonConfig.getCooldown(AttackType.HAKAI);
                    } else {
                        player.displayClientMessage(
                                Component.translatable("msg.dmzkiaddon.hakai_no_target")
                                        .withStyle(s -> s.withColor(0xAA00AA)),
                                true
                        );
                    }
                }
            }
        }

        prevFire      = curFire;
        prevNext      = curNext;
        prevPrev      = curPrev;
        prevKiShield  = curKiShield;
        prevHellzone  = curHellzone;
        prevHakaiSpam = curHakaiSpam;
        prevHakai     = curHakai;
        prevTaiyoken  = curTaiyoken;
    }

    private static void showCooldownMessage(Player player, String attackName, int cooldownTicks) {
        float secs = cooldownTicks / 20.0f;
        int cAttack = AddonConfig.getAttackNameColor();
        int cLabel  = AddonConfig.getLabelColor();
        int cTime   = AddonConfig.getTimeColor();
        player.displayClientMessage(
                Component.empty()
                        .append(Component.literal(attackName)
                                .withStyle(s -> s.withColor(cAttack)))
                        .append(Component.translatable("msg.dmzkiaddon.cooldown_label")
                                .withStyle(s -> s.withColor(cLabel)))
                        .append(Component.literal(String.format("%.1fs", secs))
                                .withStyle(s -> s.withColor(cTime))),
                true
        );
    }

    private static void fireAttack(KiAttackEntry entry, float chargeLevel) {
        AddonNetworkHandler.sendToServer(new FireKiAttackC2S(entry.type(), chargeLevel));
    }

    private static void showSelectedAttack(Player player) {
        KiAttackEntry selected = AttackSelector.getSelected();
        if (selected == null) return;
        java.util.List<KiAttackEntry> learned = AttackSelector.getLearnedAttacks();
        int index = AttackSelector.getSelectedIndex() + 1;
        player.displayClientMessage(
                Component.empty()
                        .append(Component.translatable("msg.dmzkiaddon.attack_selected")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(selected.displayName())
                                .withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" [" + index + "/" + learned.size() + "]")
                                .withStyle(ChatFormatting.DARK_GRAY)),
                true
        );
    }

    private static boolean hasSkill(Player player, String skillId) {
        boolean[] result = {false};
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            var skill = stats.getSkills().getSkill(skillId);
            result[0] = skill != null && skill.getLevel() > 0;
        });
        return result[0];
    }

    private static int getLockOnTargetId() {
        try {
            Field field = LockOnEvent.class.getDeclaredField("lockedTarget");
            field.setAccessible(true);
            Object target = field.get(null);
            if (target instanceof LivingEntity le && le.isAlive()) return le.getId();
        } catch (Exception ignored) {}
        return -1;
    }
}