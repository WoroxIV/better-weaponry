package com.betterweaponry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Rendu des armes en première personne : pendant une parade, l'arme se couche devant le joueur comme en 1.8.
 * (En troisième personne, la pose vanilla "bloquer" est déjà celle de la 1.8 : bras levé et tourné vers l'intérieur.)
 * L'arme de la main secondaire d'une parade à deux armes est dessinée par {@link DualParryHandRenderer}.
 */
public final class WeaponClientExtensions implements IClientItemExtensions {

    /**
     * Appelé à chaque image pour chaque main. Renvoie true si on a placé l'arme nous-mêmes,
     * false pour laisser le jeu faire (arme au repos, coup en cours...).
     */
    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProcess, float swingProcess) {
        if (!isParryingWith(player, arm, itemInHand)) return false;
        applyParryPose(poseStack, arm, equipProcess);
        return true;
    }

    /**
     * Place l'objet tenu par ce bras dans la pose de parade 1.8.
     *
     * @param equipProcess 0 = arme levée, 1 = arme baissée (changement d'objet en cours)
     */
    public static void applyParryPose(PoseStack poseStack, HumanoidArm arm, float equipProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Position normale de l'objet en main (comme ItemInHandRenderer.applyItemArmTransform).
        poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        // Pose de parade 1.8, reprise du snapshot 15w33b (dernière version avec le blocage à l'épée,
        // déjà avec le rendu des objets actuel).
        poseStack.translate(side * -0.14142136F, 0.08F, 0.14142136F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 13.365F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 78.05F));
    }

    /** Le joueur utilise l'objet de cette main, et cet objet pare (pose "bloquer"). */
    private static boolean isParryingWith(LocalPlayer player, HumanoidArm arm, ItemStack itemInHand) {
        if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0) return false;
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return player.getUsedItemHand() == hand && itemInHand.getUseAnimation() == UseAnim.BLOCK;
    }
}
