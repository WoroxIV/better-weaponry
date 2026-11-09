package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Pose du piglin qui tient une arbalète. */
@Mixin(Piglin.class)
public abstract class PiglinArmPoseMixin {

    @WrapOperation(method = "getArmPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/Piglin;isHolding(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$modCrossbow(Piglin piglin, Item item, Operation<Boolean> original) {
        return original.call(piglin, item) || FabricGear.holdsModVersionOf(piglin, item);
    }
}
