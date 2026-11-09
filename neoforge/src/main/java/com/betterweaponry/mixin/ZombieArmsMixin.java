package com.betterweaponry.mixin;

import com.betterweaponry.client.MobArmPoses;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Les zombies lèvent les deux bras d'un seul mouvement : en dual wield, seul le bras qui frappe doit bouger. */
@Mixin(AbstractZombieModel.class)
public abstract class ZombieArmsMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V", at = @At("TAIL"))
    private void betterweaponry$alternateArms(Monster entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        MobArmPoses.keepIdleArmStill(model, entity);
        MobArmPoses.applyGuard(model, entity);
    }
}
