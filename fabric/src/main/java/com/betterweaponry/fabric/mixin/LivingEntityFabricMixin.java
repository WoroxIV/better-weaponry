package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.EnchantmentEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fabric n'a pas d'événement pour les dégâts infligés à une créature. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFabricMixin {

    /** Les dégâts finaux, une fois l'armure et l'absorption comptées. */
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void betterweaponry$damageDealt(CombatTracker tracker, DamageSource source, float damage, Operation<Void> original) {
        original.call(tracker, source, damage);
        EnchantmentEvents.onDamageDealt((LivingEntity) (Object) this, source, damage);
    }
}
