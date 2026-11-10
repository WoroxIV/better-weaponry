package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Première personne : les arbalètes du mod s'affichent comme la vanilla. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererFabricMixin {

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modCrossbow(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || FabricGear.isModVersionOf(stack, item);
    }
}
