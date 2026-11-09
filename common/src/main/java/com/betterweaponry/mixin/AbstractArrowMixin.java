package com.betterweaponry.mixin;

import com.betterweaponry.item.WeaponCrossbowItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Une flèche tirée par une arbalète du mod compte comme tirée à l'arbalète (son d'impact, succès). */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @WrapOperation(method = "shotFromCrossbow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modCrossbows(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof WeaponCrossbowItem;
    }
}
