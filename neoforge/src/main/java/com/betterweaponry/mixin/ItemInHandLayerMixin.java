package com.betterweaponry.mixin;

import com.betterweaponry.combat.TwoHandedSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vue troisième personne : cache l'objet de la main secondaire quand le joueur tient une arme à deux mains.
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(
            method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void betterweaponry$hideTwoHandedOffhand(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                                     HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer,
                                                     int packedLight, CallbackInfo ci) {
        // Le bras secondaire est l'opposé du bras principal (gauche pour un droitier).
        if (entity instanceof Player player
                && arm == player.getMainArm().getOpposite()
                && TwoHandedSystem.isOffhandLocked(player)) {
            ci.cancel();
        }
    }
}
