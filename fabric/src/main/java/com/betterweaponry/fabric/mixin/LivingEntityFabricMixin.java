package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.CombatEvents;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.ShieldSystem;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.platform.IncomingDamage;
import com.betterweaponry.platform.Platform;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric n'a pas d'événement pour modifier un coup : on se branche aux mêmes endroits que NeoForge. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFabricMixin {
    @Unique private static final int NO_OVERRIDE = -1;

    @Unique private float betterweaponry$armorKept = 1.0F;
    @Unique private int betterweaponry$invulnerabilityTicks = NO_OVERRIDE;

    @Inject(method = "hurt", cancellable = true,
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void betterweaponry$incomingDamage(DamageSource source, float originalAmount, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalFloatRef amount) {
        betterweaponry$armorKept = 1.0F;
        betterweaponry$invulnerabilityTicks = NO_OVERRIDE;
        LivingEntity self = (LivingEntity) (Object) this;
        float[] current = {amount.get()};
        boolean[] canceled = {false};
        IncomingDamage damage = new IncomingDamage() {
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
                return current[0];
            }

            @Override
            public void setAmount(float value) {
                current[0] = value;
            }

            @Override
            public void cancel() {
                canceled[0] = true;
            }

            @Override
            public boolean isCanceled() {
                return canceled[0];
            }

            @Override
            public void scaleArmorReduction(float kept) {
                betterweaponry$armorKept *= kept;
            }

            @Override
            public void setPostAttackInvulnerabilityTicks(int ticks) {
                betterweaponry$invulnerabilityTicks = ticks;
            }
        };
        ParrySystem.onHitWithPerfectBonus(damage);
        CombatEvents.onIncomingDamage(damage);
        if (canceled[0]) {
            cir.setReturnValue(false);
            return;
        }
        amount.set(current[0]);
    }

    /** L'armure retire moins de dégâts : même calcul que la pénétration d'armure sur NeoForge. */
    @WrapMethod(method = "getDamageAfterArmorAbsorb")
    private float betterweaponry$armorPenetration(DamageSource source, float amount, Operation<Float> original) {
        float result = original.call(source, amount);
        float kept = betterweaponry$armorKept;
        betterweaponry$armorKept = 1.0F;
        return kept >= 1.0F ? result : amount - (amount - result) * kept;
    }

    @WrapOperation(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = Opcodes.PUTFIELD))
    private void betterweaponry$postAttackInvulnerability(LivingEntity self, int ticks, Operation<Void> original) {
        original.call(self, betterweaponry$invulnerabilityTicks != NO_OVERRIDE ? betterweaponry$invulnerabilityTicks : ticks);
    }

    /** Coup arrêté par un bouclier : une parade parfaite ne l'use pas. */
    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"))
    private void betterweaponry$shieldBlock(LivingEntity self, float damage, Operation<Void> original, @Local(argsOnly = true) DamageSource source) {
        if (!ShieldSystem.onShieldBlock(self, source, damage)) original.call(self, damage);
    }

    /** Pour le jeu de base, tout objet en pose "bloquer" est un bouclier : une arme qui pare réduit les coups, elle ne les arrête pas. */
    @ModifyReturnValue(method = "isBlocking", at = @At("RETURN"))
    private boolean betterweaponry$onlyShieldsBlock(boolean blocking) {
        return blocking && Platform.get().isShield(((LivingEntity) (Object) this).getUseItem());
    }

    @Inject(method = "startUsingItem", at = @At("HEAD"))
    private void betterweaponry$startUsingItem(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack stack = self.getItemInHand(hand);
        if (!stack.isEmpty() && !self.isUsingItem()) ShieldSystem.onStartUsingItem(self, stack);
    }

    /** Sur le jeu de base, seule la hache désactive les boucliers. */
    @ModifyReturnValue(method = "canDisableShield", at = @At("RETURN"))
    private boolean betterweaponry$weaponsDisableShields(boolean disables) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack weapon = self.getWeaponItem();
        return weapon.getItem() instanceof WeaponItem item ? item.canDisableShield(weapon, self.getUseItem(), self, self) : disables;
    }
}
