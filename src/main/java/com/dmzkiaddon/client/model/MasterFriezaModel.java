package com.dmzkiaddon.client.model;

import com.dmzkiaddon.entity.masters.MasterFriezaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MasterFriezaModel extends GeoModel<MasterFriezaEntity> {

    @Override
    public ResourceLocation getModelResource(MasterFriezaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "geo/entity/sagas/saga_frieza_base.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MasterFriezaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "textures/entity/sagas/saga_frieza_base.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MasterFriezaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "animations/entity/sagas/saga_frieza_base.animation.json");
    }

}