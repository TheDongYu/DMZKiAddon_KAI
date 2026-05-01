package com.dmzkiaddon.client.model;

import com.dmzkiaddon.entity.masters.MasterVegetaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MasterVegetaModel extends GeoModel<MasterVegetaEntity> {

    @Override
    public ResourceLocation getModelResource(MasterVegetaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "geo/entity/sagas/saga_vegeta_namek.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MasterVegetaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "textures/entity/sagas/saga_vegeta_namek.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MasterVegetaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "animations/entity/sagas/saga_vegeta_namek.animation.json");
    }
}