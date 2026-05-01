package com.dmzkiaddon.client.renderer;

import com.dmzkiaddon.client.model.MasterPiccoloModel;
import com.dmzkiaddon.entity.masters.MasterPiccoloEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MasterPiccoloRenderer extends GeoEntityRenderer<MasterPiccoloEntity> {

    public MasterPiccoloRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MasterPiccoloModel());
    }

    @Override
    public ResourceLocation getTextureLocation(MasterPiccoloEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("dragonminez",
                "textures/entity/sagas/saga_piccolo_kami.png");
    }
}