package com.dmzkiaddon.registry;

import com.dmzkiaddon.DMZKiAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DMZKiAddon.MOD_ID);

    public static final RegistryObject<SoundEvent> CBEAM_1 = register("cbeam1s");
    public static final RegistryObject<SoundEvent> CBEAM_2 = register("cbeam2s");
    public static final RegistryObject<SoundEvent> CBEAM_3 = register("cbeam3s");
    public static final RegistryObject<SoundEvent> CBEAM_4 = register("cbeam4s");
    public static final RegistryObject<SoundEvent> CBEAM_5 = register("cbeam5s");
    public static final RegistryObject<SoundEvent> CBEAM_6 = register("cbeam6s");
    public static final RegistryObject<SoundEvent> CBEAM_7 = register("cbeam7s");
    public static final RegistryObject<SoundEvent> FBEAM_1 = register("fbeam1s");
    public static final RegistryObject<SoundEvent> FBEAM_2 = register("fbeam2s");
    public static final RegistryObject<SoundEvent> FBEAM_3 = register("fbeam3s");
    public static final RegistryObject<SoundEvent> FBEAM_4 = register("fbeam4s");
    public static final RegistryObject<SoundEvent> FBEAM_5 = register("fbeam5s");
    public static final RegistryObject<SoundEvent> CDISK_1 = register("cdisk1s");
    public static final RegistryObject<SoundEvent> CDISK_2 = register("cdisk2s");
    public static final RegistryObject<SoundEvent> DISC_KILL = register("disckill");
    public static final RegistryObject<SoundEvent> DISC_FIRE = register("disc_fire");
    public static final RegistryObject<SoundEvent> BIGBANG_FIRE = register("bigbang_fire");
    public static final RegistryObject<SoundEvent> DEATHBALL_CHARGE = register("deathball_charge");
    public static final RegistryObject<SoundEvent> DEATHBALL_FIRE = register("deathball_fire");
    public static final RegistryObject<SoundEvent> FINALFLASH_CHARGE = register("finalflash_charge");
    public static final RegistryObject<SoundEvent> KIBALL_RELEASE = register("kiball_release");
    public static final RegistryObject<SoundEvent> BASICBEAM_FIRE = register("basicbeam_fire");
    public static final RegistryObject<SoundEvent> BLAST = register("blast");
    public static final RegistryObject<SoundEvent> HAMEHA_FIRE = register("hamehafire");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createFixedRangeEvent(
                ResourceLocation.fromNamespaceAndPath(DMZKiAddon.MOD_ID, name), 16.0f));
    }
}
