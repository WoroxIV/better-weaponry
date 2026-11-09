package com.betterweaponry.mixin;

import com.betterweaponry.combat.ComboSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vue troisième personne d'un coup à deux armes (combo) : au lieu d'un bras qui frappe en tournant le buste,
 * les deux bras s'abattent ensemble, face à l'ennemi. Mêmes courbes que l'animation vanilla d'un bras.
 */
@Mixin(HumanoidModel.class)
public abstract class DualSwingModelMixin {

    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$swingBothArms(LivingEntity entity, float ageInTicks, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        float time = model.attackTime;
        if (time <= 0.0F || !(entity instanceof Player player) || !ComboSystem.isDualSwinging(player)) return;

        // Courbe vanilla : montée rapide puis retour (HumanoidModel.setupAttackAnimation).
        float eased = 1.0F - time;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float lift = Mth.sin(eased * Mth.PI) * 1.2F + Mth.sin(time * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;
        float roll = Mth.sin(time * Mth.PI) * 0.4F;
        model.rightArm.xRot -= lift;
        model.leftArm.xRot -= lift;
        model.rightArm.zRot -= roll;
        model.leftArm.zRot += roll;
        ci.cancel();
    }
}
