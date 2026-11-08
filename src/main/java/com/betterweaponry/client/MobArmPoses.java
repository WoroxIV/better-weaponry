package com.betterweaponry.client;

import com.betterweaponry.combat.DualWieldSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.UseAnim;

/** Poses des bras des mobs : garde levée et coups alternés. Mêmes valeurs que les poses du joueur. */
public final class MobArmPoses {
    private static final float GUARD_PITCH = 0.9424779F;
    private static final float GUARD_YAW = 0.5235988F;

    private MobArmPoses() {
    }

    public static void applyGuard(HumanoidModel<?> model, LivingEntity entity) {
        if (!(entity instanceof Mob) || !entity.isUsingItem() || entity.getUseItem().getUseAnimation() != UseAnim.BLOCK) return;
        boolean right = armOf(entity, entity.getUsedItemHand()) == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        arm.xRot = arm.xRot * 0.5F - GUARD_PITCH;
        arm.yRot = (right ? -1.0F : 1.0F) * GUARD_YAW;
    }

    /** Retire d'un bras l'élan d'attaque que le zombie applique aux deux (AnimationUtils.animateZombieArms). */
    public static void keepIdleArmStill(HumanoidModel<?> model, LivingEntity entity) {
        float time = model.attackTime;
        if (time <= 0.0F || !DualWieldSystem.isDualWielding(entity)) return;
        boolean rightStrikes = armOf(entity, entity.swingingArm) == HumanoidArm.RIGHT;
        ModelPart idle = rightStrikes ? model.leftArm : model.rightArm;
        float swing = Mth.sin(time * Mth.PI);
        float recoil = Mth.sin((1.0F - (1.0F - time) * (1.0F - time)) * Mth.PI);
        idle.xRot -= swing * 1.2F - recoil * 0.4F;
        idle.yRot = rightStrikes ? 0.1F : -0.1F;
    }

    private static HumanoidArm armOf(LivingEntity entity, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
    }
}
