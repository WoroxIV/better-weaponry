package com.betterweaponry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.HumanoidArm;

/** Pose de parade en première personne : l'arme se couche devant le joueur, comme en 1.8. */
public final class ParryPose {

    private ParryPose() {
    }

    /**
     * Place l'objet tenu par ce bras dans la pose de parade 1.8.
     *
     * @param equipProcess 0 = arme levée, 1 = arme baissée (changement d'objet en cours)
     */
    public static void apply(PoseStack poseStack, HumanoidArm arm, float equipProcess) {
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
}
