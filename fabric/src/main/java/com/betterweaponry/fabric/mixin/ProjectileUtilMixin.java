package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Main qui tient l'arme d'un mob qui tire. */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {

    @WrapOperation(method = "getWeaponHoldingHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean betterweaponry$modGear(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || FabricGear.isModVersionOf(stack, item);
    }
}
