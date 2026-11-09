package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricGear;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Un piglin avec une arbalète du mod combat à distance. */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @WrapOperation(method = "hasCrossbow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isHolding(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean betterweaponry$modCrossbow(LivingEntity piglin, Item item, Operation<Boolean> original) {
        return original.call(piglin, item) || FabricGear.holdsModVersionOf(piglin, item);
    }
}
