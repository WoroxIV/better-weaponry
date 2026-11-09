package com.betterweaponry.mixin;

import com.betterweaponry.item.WeaponBowItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Un squelette vise avec les arcs du mod comme avec l'arc vanilla. */
@Mixin(SkeletonModel.class)
public abstract class SkeletonModelMixin {

    @WrapOperation(method = {"prepareMobModel(Lnet/minecraft/world/entity/Mob;FFF)V", "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modBows(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof WeaponBowItem;
    }
}
