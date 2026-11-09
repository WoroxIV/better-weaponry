package com.betterweaponry.mixin;

import com.betterweaponry.item.WeaponCrossbowItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Un piglin tient à son arbalète : celles du mod comptent comme la vanilla quand il choisit quoi ramasser. */
@Mixin(Piglin.class)
public abstract class PiglinMixin {

    @WrapOperation(method = "canReplaceCurrentItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modCrossbows(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof WeaponCrossbowItem;
    }
}
