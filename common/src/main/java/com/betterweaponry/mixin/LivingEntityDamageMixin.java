package com.betterweaponry.mixin;

import com.betterweaponry.combat.EnchantmentEvents;
import com.betterweaponry.platform.IncomingDamage;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** La 1.20.1 n'a pas d'événement qui modifie un coup avant l'armure : le mod se branche directement dans {@code hurt}. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void betterweaponry$incomingDamage(DamageSource source, float originalAmount, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalFloatRef amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        EnchantmentEvents.onIncomingDamage(new IncomingDamage() {
            @Override
            public LivingEntity target() {
                return self;
            }

            @Override
            public DamageSource source() {
                return source;
            }

            @Override
            public float amount() {
                return amount.get();
            }

            @Override
            public void setAmount(float value) {
                amount.set(value);
            }
        });
    }
}
