package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.EnchantmentEvents;
import com.betterweaponry.combat.ShieldSystem;
import com.betterweaponry.platform.Platform;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ce que Forge modifie dans les créatures, et que Fabric laisse tel quel. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFabricMixin {

    /** Les dégâts finaux, une fois l'armure et l'absorption comptées. */
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void betterweaponry$damageDealt(CombatTracker tracker, DamageSource source, float damage, Operation<Void> original) {
        original.call(tracker, source, damage);
        EnchantmentEvents.onDamageDealt((LivingEntity) (Object) this, source, damage);
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
}
