package com.betterweaponry.mixin;

import com.betterweaponry.combat.TwoHandedSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vue troisième personne : avec une arme à deux mains, le bras secondaire pend normalement
 * au lieu de prendre la pose de l'objet qu'il tient (bouclier levé, etc.).
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void betterweaponry$emptyOffhandPose(AbstractClientPlayer player, InteractionHand hand,
                                                       CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (hand == InteractionHand.OFF_HAND && TwoHandedSystem.isOffhandLocked(player)) {
            cir.setReturnValue(HumanoidModel.ArmPose.EMPTY);
        }
    }
}
