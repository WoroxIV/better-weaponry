package com.betterweaponry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

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
        ParryPose.apply(poseStack, arm, equipProcess);
        return true;
    }

    /** Le joueur utilise l'objet de cette main, et cet objet pare (pose "bloquer"). */
    private static boolean isParryingWith(LocalPlayer player, HumanoidArm arm, ItemStack itemInHand) {
        if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0) return false;
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return player.getUsedItemHand() == hand && itemInHand.getUseAnimation() == UseAnim.BLOCK;
    }
}
