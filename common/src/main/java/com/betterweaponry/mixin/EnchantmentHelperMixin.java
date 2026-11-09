package com.betterweaponry.mixin;

import com.betterweaponry.registry.ModAttributes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** La part de dégâts du coup balayé donnée par l'arme s'ajoute à celle d'Affilage. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @ModifyReturnValue(method = "getSweepingDamageRatio(Lnet/minecraft/world/entity/LivingEntity;)F", at = @At("RETURN"))
    private static float betterweaponry$weaponSweepRatio(float ratio, LivingEntity entity) {
        AttributeInstance weaponRatio = entity.getAttribute(ModAttributes.SWEEPING_DAMAGE_RATIO.get());
        return weaponRatio == null ? ratio : ratio + (float) weaponRatio.getValue();
    }
}
