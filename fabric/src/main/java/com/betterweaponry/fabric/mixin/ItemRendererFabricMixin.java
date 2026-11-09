package com.betterweaponry.fabric.mixin;

import com.betterweaponry.client.WeaponIconLayout;
import com.betterweaponry.item.WeaponItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** Dans les interfaces, le dessin des grandes armes occupe tout le slot. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererFabricMixin {

    @WrapOperation(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void betterweaponry$weaponIcon(ItemTransform transform, boolean leftHand, PoseStack pose, Operation<Void> original,
                                           @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) ItemDisplayContext context, @Local(argsOnly = true) BakedModel model) {
        if (context == ItemDisplayContext.GUI && stack.getItem() instanceof WeaponItem) {
            Optional<WeaponIconLayout> layout = WeaponIconLayout.cached(model);
            if (layout.isPresent()) {
                layout.get().apply(pose);
                return;
            }
        }
        original.call(transform, leftHand, pose);
    }
}
