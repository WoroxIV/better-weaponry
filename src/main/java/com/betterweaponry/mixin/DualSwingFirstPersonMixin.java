package com.betterweaponry.mixin;

import com.betterweaponry.combat.ComboSystem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vue première personne d'un coup à deux armes (combo) : le vanilla n'anime que la main qui frappe ;
 * on donne la même progression de coup aux deux mains, qui frappent donc ensemble.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class DualSwingFirstPersonMixin {

    @WrapOperation(
            method = "renderHandsWithItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    private void betterweaponry$swingBothHands(ItemInHandRenderer renderer, AbstractClientPlayer player, float partialTick,
                                               float pitch, InteractionHand hand, float swingProgress, ItemStack stack,
                                               float equipProgress, PoseStack poseStack, MultiBufferSource buffer,
                                               int packedLight, Operation<Void> original) {
        float swing = ComboSystem.isDualSwinging(player) ? player.getAttackAnim(partialTick) : swingProgress;
        original.call(renderer, player, partialTick, pitch, hand, swing, stack, equipProgress, poseStack, buffer, packedLight);
    }
}
