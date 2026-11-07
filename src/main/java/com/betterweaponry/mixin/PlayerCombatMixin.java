package com.betterweaponry.mixin;

import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.HandSwap;
import com.betterweaponry.combat.IDualWieldPlayer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stocke l'état du dual wield dans chaque joueur et fait frapper la bonne arme.
 * Tourne côté client (prédiction) et côté serveur (dégâts réels) avec la même logique.
 */
@Mixin(Player.class)
public abstract class PlayerCombatMixin implements IDualWieldPlayer {

    @Unique private boolean betterweaponry$offhandTurn = false;
    @Unique private boolean betterweaponry$justAttacked = false;
    @Unique private boolean betterweaponry$lastAttackOffhand = false;
    @Unique private boolean betterweaponry$redirectingSwing = false;
    @Unique @Nullable private HandSwap betterweaponry$activeSwap = null;

    // --- Implémentation de IDualWieldPlayer ---

    @Override public boolean betterweaponry$isOffhandTurn() { return betterweaponry$offhandTurn; }
    @Override public void betterweaponry$toggleTurn() { betterweaponry$offhandTurn = !betterweaponry$offhandTurn; }

    @Override
    public boolean betterweaponry$consumeJustAttacked() {
        boolean result = betterweaponry$justAttacked;
        betterweaponry$justAttacked = false;
        return result;
    }

    @Override public boolean betterweaponry$isLastAttackOffhand() { return betterweaponry$lastAttackOffhand; }

    @Override public boolean betterweaponry$isRedirectingSwing() { return betterweaponry$redirectingSwing; }
    @Override public void betterweaponry$setRedirectingSwing(boolean redirecting) { betterweaponry$redirectingSwing = redirecting; }

    @Override @Nullable public HandSwap betterweaponry$getActiveSwap() { return betterweaponry$activeSwap; }

    // --- Attaque ---

    /**
     * Entoure toute la méthode attack() : si c'est au tour de la main secondaire, les deux mains sont
     * échangées pendant l'attaque, puis remises en place dans un finally (même en cas de crash ou
     * de retour anticipé). Les événements NeoForge déclenchés pendant l'attaque voient aussi la bonne arme.
     */
    @WrapMethod(method = "attack")
    private void betterweaponry$attackWithCurrentHand(Entity target, Operation<Void> original) {
        Player player = (Player) (Object) this;
        if (!DualWieldSystem.isDualWielding(player) || betterweaponry$activeSwap != null) {
            original.call(target);
            return;
        }

        // Mémorisé pour que l'animation (swing) qui suit l'attaque montre la main qui a frappé.
        boolean offhandAttack = betterweaponry$offhandTurn;
        betterweaponry$justAttacked = true;
        betterweaponry$lastAttackOffhand = offhandAttack;

        if (!offhandAttack) {
            original.call(target);
            return;
        }

        betterweaponry$activeSwap = HandSwap.begin(player);
        try {
            original.call(target);
        } finally {
            betterweaponry$activeSwap.restore();
            betterweaponry$activeSwap = null;
        }
    }

    /**
     * Recharge en dual wield : celle de l'arme qui va frapper, divisée par le multiplicateur de la config.
     * Sert à la force des coups (client et serveur) et à l'indicateur de recharge du viseur.
     */
    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$dualWieldAttackDelay(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (DualWieldSystem.isDualWielding(player)) {
            cir.setReturnValue(DualWieldSystem.attackStrengthDelay(player, this));
        }
    }
}
