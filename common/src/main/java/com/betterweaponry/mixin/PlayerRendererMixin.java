package com.betterweaponry.mixin;

import com.betterweaponry.combat.ParrySystem;
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
 * Vue troisième personne, pose du bras secondaire :
 * <ul>
 *   <li>avec une arme à deux mains, il pend normalement au lieu de prendre la pose de l'objet qu'il tient
 *       (bouclier levé, etc.) ;</li>
 *   <li>pendant une parade à deux armes (ex : deux saïs), il se lève aussi en garde, comme le bras principal.</li>
 * </ul>
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void betterweaponry$offhandPose(AbstractClientPlayer player, InteractionHand hand,
                                                  CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (hand != InteractionHand.OFF_HAND) return;
        if (TwoHandedSystem.isOffhandLocked(player)) {
            cir.setReturnValue(HumanoidModel.ArmPose.EMPTY);
        } else if (ParrySystem.isParryingWithBothHands(player)) {
            cir.setReturnValue(HumanoidModel.ArmPose.BLOCK);
        }
    }
}
