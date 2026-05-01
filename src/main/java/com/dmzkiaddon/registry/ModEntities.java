package com.dmzkiaddon.registry;

import com.dmzkiaddon.DMZKiAddon;
import com.dmzkiaddon.entity.masters.MasterFriezaEntity;
import com.dmzkiaddon.entity.masters.MasterPiccoloEntity;
import com.dmzkiaddon.entity.masters.MasterVegetaEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DMZKiAddon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DMZKiAddon.MOD_ID);

    public static final RegistryObject<EntityType<MasterVegetaEntity>> MASTER_VEGETA =
            ENTITY_TYPES.register("master_vegeta", () ->
                    EntityType.Builder.<MasterVegetaEntity>of(MasterVegetaEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .build("master_vegeta")
            );

    public static final RegistryObject<EntityType<MasterPiccoloEntity>> MASTER_PICCOLO =
            ENTITY_TYPES.register("master_piccolo", () ->
                    EntityType.Builder.<MasterPiccoloEntity>of(MasterPiccoloEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.95f)
                            .build("master_piccolo")
            );

    public static final RegistryObject<EntityType<MasterFriezaEntity>> MASTER_FRIEZA =
            ENTITY_TYPES.register("master_frieza", () ->
                    EntityType.Builder.<MasterFriezaEntity>of(MasterFriezaEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .build("master_frieza")
            );

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MASTER_VEGETA.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 20.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.25)
                        .build()
        );
        event.put(MASTER_PICCOLO.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 20.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.25)
                        .build()
        );
        event.put(MASTER_FRIEZA.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 20.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.25)
                        .build()
        );
    }
}