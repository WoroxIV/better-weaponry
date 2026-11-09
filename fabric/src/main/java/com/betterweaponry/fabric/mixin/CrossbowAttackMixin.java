package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Le comportement de tir des piglins ne reconnaît que l'arbalète vanilla. */
@Mixin(CrossbowAttack.class)
public abstract class CrossbowAttackMixin {

    @WrapOperation(method = {"checkExtraStartConditions", "crossbowAttack"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;isHolding(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modCrossbow(Mob mob, Item item, Operation<Boolean> original) {
        return original.call(mob, item) || FabricGear.holdsModVersionOf(mob, item);
    }
}
