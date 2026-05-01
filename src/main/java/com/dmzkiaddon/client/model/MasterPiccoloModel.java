package com.dmzkiaddon.client.model;

import com.dmzkiaddon.entity.masters.MasterPiccoloEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MasterPiccoloModel extends GeoModel<MasterPiccoloEntity> {

    @Override
    public ResourceLocation getModelResource(MasterPiccoloEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "geo/entity/sagas/saga_piccolo_kami.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MasterPiccoloEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "textures/entity/sagas/saga_piccolo_kami.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MasterPiccoloEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "animations/entity/sagas/saga_piccolo_kami.animation.json");
    }
}