package com.dmzkiaddon.client.renderer;

import com.dmzkiaddon.client.model.MasterVegetaModel;
import com.dmzkiaddon.entity.masters.MasterVegetaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MasterVegetaRenderer extends GeoEntityRenderer<MasterVegetaEntity> {

    public MasterVegetaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MasterVegetaModel());
    }

    @Override
    public ResourceLocation getTextureLocation(MasterVegetaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "textures/entity/sagas/saga_vegeta_namek.png");
    }
}