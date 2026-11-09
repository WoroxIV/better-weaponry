package com.betterweaponry.mixin;

import com.betterweaponry.client.MobArmPoses;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bras levé d'un mob en garde. */
@Mixin(HumanoidModel.class)
public abstract class MobGuardModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void betterweaponry$guardPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        MobArmPoses.applyGuard((HumanoidModel<?>) (Object) this, entity);
    }
}
