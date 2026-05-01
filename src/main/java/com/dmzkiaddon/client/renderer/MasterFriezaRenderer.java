package com.dmzkiaddon.client.renderer;

import com.dmzkiaddon.client.model.MasterFriezaModel;
import com.dmzkiaddon.entity.masters.MasterFriezaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MasterFriezaRenderer extends GeoEntityRenderer<MasterFriezaEntity> {

    public MasterFriezaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MasterFriezaModel());
    }

    @Override
    public ResourceLocation getTextureLocation(MasterFriezaEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "textures/entity/sagas/saga_frieza_base.png");
    }
}