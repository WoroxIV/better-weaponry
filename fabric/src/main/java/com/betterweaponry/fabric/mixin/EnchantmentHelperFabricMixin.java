package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.ModEnchantment;
import com.betterweaponry.item.ShurikenItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** À la table d'enchantement, les objets et enchantements du mod suivent les mêmes règles qu'à l'enclume. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperFabricMixin {

    @WrapOperation(method = "getAvailableEnchantmentResults", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentCategory;canEnchant(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean betterweaponry$modRules(EnchantmentCategory category, Item item, Operation<Boolean> original,
                                                   @Local Enchantment enchantment, @Local(argsOnly = true) ItemStack stack) {
        if (item instanceof ShurikenItem shuriken) return shuriken.canApplyAtEnchantingTable(stack, enchantment);
        return enchantment instanceof ModEnchantment ? enchantment.canEnchant(stack) : original.call(category, item);
    }
}
