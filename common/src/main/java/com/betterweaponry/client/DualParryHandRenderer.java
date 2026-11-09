package com.betterweaponry.client;

import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.WeaponProfiles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Vue première personne des parades que le rendu normal de l'arme ne peut pas placer : l'arme de la main
 * secondaire d'une parade à deux armes (ex : deux saïs), et un objet adopté par un fichier d'arme (ex : une épée
 * vanilla). On les dessine nous-mêmes dans la pose de parade 1.8, ce qui marche avec n'importe quel objet.
 * (Vue troisième personne : PlayerRendererMixin lève aussi le bras secondaire.)
 */
public final class DualParryHandRenderer {

    private DualParryHandRenderer() {
    }

    /**
     * @param allParries aussi les armes du mod en main principale, pour les chargeurs qui n'ont pas de rendu par objet
     * @return vrai si l'objet a été dessiné ici : le jeu ne doit pas le dessiner une seconde fois
     */
    public static boolean render(InteractionHand hand, ItemStack stack, PoseStack poseStack, MultiBufferSource buffers, int packedLight, float equipProgress, boolean allParries) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isScoping()) return false;
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        boolean posed = mainHand
                ? ParrySystem.isParrying(player) && (allParries || WeaponProfiles.isAdopted(stack))
                : ParrySystem.isParryingWithBothHands(player);
        if (!posed) return false;

        HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean leftHand = arm == HumanoidArm.LEFT;
        poseStack.pushPose();
        ParryPose.apply(poseStack, arm, equipProgress);
        minecraft.gameRenderer.itemInHandRenderer.renderItem(player, stack,
                leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                leftHand, poseStack, buffers, packedLight);
        poseStack.popPose();
        return true;
    }
}
