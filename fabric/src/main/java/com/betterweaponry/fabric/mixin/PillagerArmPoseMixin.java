package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Pose du pillard qui tient une arbalète. */
@Mixin(Pillager.class)
public abstract class PillagerArmPoseMixin {

    @WrapOperation(method = "getArmPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Pillager;isHolding(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modCrossbow(Pillager pillager, Item item, Operation<Boolean> original) {
        return original.call(pillager, item) || FabricGear.holdsModVersionOf(pillager, item);
    }
}
