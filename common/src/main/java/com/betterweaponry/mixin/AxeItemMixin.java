package com.betterweaponry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Une hache n'écorce pas un bloc quand on veut lever le bouclier en main secondaire : le jeu ne pense qu'au bouclier vanilla. */
@Mixin(AxeItem.class)
public abstract class AxeItemMixin {

    @WrapOperation(method = "playerHasShieldUseIntent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean betterweaponry$anyShield(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof ShieldItem;
    }
}
