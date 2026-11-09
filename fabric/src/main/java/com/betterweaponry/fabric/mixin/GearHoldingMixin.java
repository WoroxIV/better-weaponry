package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Les tâches de tir des monstres ne reconnaissent que l'arc et l'arbalète vanilla. */
@Mixin({RangedBowAttackGoal.class, RangedCrossbowAttackGoal.class})
public abstract class GearHoldingMixin {

    @WrapOperation(method = {"isHoldingBow", "isHoldingCrossbow"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Monster;isHolding(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modGear(Monster mob, Item item, Operation<Boolean> original) {
        return original.call(mob, item) || FabricGear.holdsModVersionOf(mob, item);
    }
}
