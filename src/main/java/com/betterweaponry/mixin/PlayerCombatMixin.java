package com.betterweaponry.mixin;

import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.HandSwap;
import com.betterweaponry.combat.ICombatPlayer;
import com.betterweaponry.combat.PlayerCombatState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Donne à chaque joueur son état de combat ({@link PlayerCombatState}) et entoure ses attaques :
 * dual wield (la bonne main frappe) et combos (coups spéciaux).
 * Tourne côté client (prédiction) et côté serveur (dégâts réels) avec la même logique.
 */
@Mixin(Player.class)
public abstract class PlayerCombatMixin implements ICombatPlayer {

    @Unique private final PlayerCombatState betterweaponry$combat = new PlayerCombatState();

    @Override
    public PlayerCombatState betterweaponry$combat() {
        return betterweaponry$combat;
    }

    /**
     * Entoure toute la méthode attack() :
     * <ul>
     *   <li>combo : l'étape est décidée avant le coup et le combo avance après ({@link ComboSystem}) ;</li>
     *   <li>coup spécial : toujours porté par l'arme principale, avec ses bonus ;</li>
     *   <li>sinon, si c'est au tour de la main secondaire, les deux mains sont échangées pendant l'attaque,
     *       puis remises en place dans un finally (même en cas de crash ou de retour anticipé).</li>
     * </ul>
     */
    @WrapMethod(method = "attack")
    private void betterweaponry$attack(Entity target, Operation<Void> original) {
        Player player = (Player) (Object) this;
        PlayerCombatState state = betterweaponry$combat;
        // Appel imbriqué pendant un échange de mains ou un coup spécial : comportement vanilla.
        if (state.activeSwap != null || state.activeStep != null) {
            original.call(target);
            return;
        }

        ComboSystem.Hit hit = ComboSystem.beginHit(player, state);
        boolean special = hit.special();
        boolean dualWielding = DualWieldSystem.isDualWielding(player);
        boolean offhandAttack = dualWielding && state.offhandTurn && !special;
        // Mémorisé pour que l'animation (swing) qui suit l'attaque montre la main qui a frappé.
        if (dualWielding) {
            state.justAttacked = true;
            state.lastAttackOffhand = offhandAttack;
            state.lastAttackSpecial = special;
        }

        try {
            if (special) {
                ComboSystem.performSpecialHit(player, target, hit, () -> original.call(target));
            } else if (offhandAttack) {
                state.activeSwap = HandSwap.begin(player);
                try {
                    original.call(target);
                } finally {
                    state.activeSwap.restore();
                    state.activeSwap = null;
                }
            } else {
                original.call(target);
            }
        } finally {
            ComboSystem.endHit(player, state, hit);
        }
    }

    /**
     * Recharge : celle imposée par un coup spécial du combo, sinon en dual wield celle de l'arme qui va frapper,
     * divisée par le multiplicateur de la config. Sert à la force des coups (client et serveur) et à l'indicateur
     * de recharge du viseur.
     */
    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$attackDelay(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        Float comboDelay = ComboSystem.attackDelayOverride(player, betterweaponry$combat);
        if (comboDelay != null) {
            cir.setReturnValue(comboDelay);
        } else if (DualWieldSystem.isDualWielding(player)) {
            cir.setReturnValue(DualWieldSystem.attackStrengthDelay(player, betterweaponry$combat));
        }
    }
}
