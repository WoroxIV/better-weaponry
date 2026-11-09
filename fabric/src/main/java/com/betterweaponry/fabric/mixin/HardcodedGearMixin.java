package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Un squelette choisit sa façon de combattre selon son arme : un arc du mod compte comme un arc. */
@Mixin(AbstractSkeleton.class)
public abstract class HardcodedGearMixin {

    @WrapOperation(method = "reassessWeaponGoal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modBow(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || FabricGear.isModVersionOf(stack, item);
    }
}
