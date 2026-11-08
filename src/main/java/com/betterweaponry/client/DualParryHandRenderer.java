package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.WeaponProfiles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Vue première personne des parades que {@link WeaponClientExtensions} ne peut pas placer : l'arme de la main
 * secondaire d'une parade à deux armes (ex : deux saïs), et un objet adopté par un fichier d'arme (ex : une épée
 * vanilla). On les dessine nous-mêmes dans la pose de parade 1.8, ce qui marche avec n'importe quel objet.
 * (Vue troisième personne : PlayerRendererMixin lève aussi le bras secondaire.)
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class DualParryHandRenderer {

    private DualParryHandRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isScoping()) return;
        boolean mainHand = event.getHand() == InteractionHand.MAIN_HAND;
        boolean posed = mainHand
                ? ParrySystem.isParrying(player) && WeaponProfiles.isAdopted(event.getItemStack())
                : ParrySystem.isParryingWithBothHands(player);
        if (!posed) return;

        HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean leftHand = arm == HumanoidArm.LEFT;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        WeaponClientExtensions.applyParryPose(poseStack, arm, event.getEquipProgress());
        minecraft.gameRenderer.itemInHandRenderer.renderItem(player, event.getItemStack(),
                leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                leftHand, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
        // Le rendu vanilla de cette main est remplacé par le nôtre.
        event.setCanceled(true);
    }
}
