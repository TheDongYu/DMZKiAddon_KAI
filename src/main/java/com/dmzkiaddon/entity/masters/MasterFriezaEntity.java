package com.dmzkiaddon.entity.masters;

import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

public class MasterFriezaEntity extends MastersEntity {

    public MasterFriezaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.masterName = "Frieza";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event ->
                event.setAndContinue(RawAnimation.begin().thenLoop("idle"))
        ));

        controllers.add(new AnimationController<>(this, "tail_controller", 0, event ->
                event.setAndContinue(RawAnimation.begin().thenLoop("tail"))
        ));
    }
}