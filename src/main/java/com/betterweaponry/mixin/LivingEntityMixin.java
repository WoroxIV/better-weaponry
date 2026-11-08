package com.betterweaponry.mixin;

import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.MobCombat;
import com.betterweaponry.combat.PlayerCombatState;
import com.betterweaponry.combat.WeaponProfiles;
import com.betterweaponry.data.WeaponData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Partie "LivingEntity" du dual wield et des combos. Ces méthodes sont définies dans LivingEntity,
 * mais on n'agit que sur les joueurs.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Le jeu fait toujours balancer la main principale quand on frappe :
     * en dual wield, on redirige l'animation vers la main dont c'est le tour.
     * Un coup à deux armes arme aussi l'animation des deux bras pour ce coup de bras.
     */
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$alternateSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        if ((Object) this instanceof Mob mob) {
            // Côté serveur seulement : le client reçoit déjà la bonne main.
            if (hand == InteractionHand.MAIN_HAND && !mob.level().isClientSide && MobCombat.isOffhandTurn(mob) && DualWieldSystem.isDualWielding(mob)) {
                ci.cancel();
                mob.swing(InteractionHand.OFF_HAND, updateSelf);
            }
            return;
        }
        if (!((Object) this instanceof Player player)) return;
        PlayerCombatState state = PlayerCombatState.of(player);
        // Pendant notre propre appel redirigé, on laisse le jeu faire.
        if (state.redirectingSwing) return;

        // L'animation à deux bras ne vaut que pour le coup de bras qui suit le coup à deux armes.
        state.dualSwingActive = state.dualSwingPending;
        state.dualSwingPending = false;
        // Consommé à chaque swing pour qu'une ancienne attaque n'influence jamais un swing plus tard.
        boolean justAttacked = state.justAttacked;
        state.justAttacked = false;

        if (!DualWieldSystem.isDualWielding(player)) return;
        // Les autres joueurs vus par ce client : le serveur envoie déjà la bonne main, on ne l'alterne pas une 2e fois.
        if (player.level().isClientSide() && !player.isLocalPlayer()) return;

        boolean offhandSwing;
        if (justAttacked && state.lastAttackSpecial) {
            // Coup spécial du combo : porté par l'arme principale, et la suite repart de la main principale.
            offhandSwing = false;
            state.offhandTurn = false;
        } else {
            // Après une attaque, on anime la main qui a réellement frappé ; sinon (clic dans le vide) on suit le tour.
            offhandSwing = justAttacked ? state.lastAttackOffhand : state.offhandTurn;
            state.offhandTurn = !state.offhandTurn;
        }

        InteractionHand expectedHand = offhandSwing ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (hand != expectedHand) {
            ci.cancel();
            state.redirectingSwing = true;
            try {
                player.swing(expectedHand, updateSelf);
            } finally {
                state.redirectingSwing = false;
            }
        }
    }

    /** Objet adopté par une arme qui désactive les boucliers (champ "items"). */
    @Inject(method = "canDisableShield", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$adoptedDisablesShield(CallbackInfoReturnable<Boolean> cir) {
        ItemStack weapon = ((LivingEntity) (Object) this).getMainHandItem();
        if (WeaponData.rules().shieldDisablingActive() && WeaponProfiles.isAdopted(weapon)
                && WeaponProfiles.definition(weapon).orElseThrow().disablesShield()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Si l'arme secondaire casse pendant son attaque, le jeu (qui la croit en main principale)
     * signalerait la casse de la main principale : son et particules seraient ceux de la mauvaise arme.
     * On inverse donc la main signalée tant que les mains sont échangées.
     */
    @ModifyVariable(method = "onEquippedItemBroken", at = @At("HEAD"), argsOnly = true)
    private EquipmentSlot betterweaponry$reportBrokenHandCorrectly(EquipmentSlot slot) {
        if ((Object) this instanceof Player player && PlayerCombatState.of(player).activeSwap != null) {
            if (slot == EquipmentSlot.MAINHAND) return EquipmentSlot.OFFHAND;
            if (slot == EquipmentSlot.OFFHAND) return EquipmentSlot.MAINHAND;
        }
        return slot;
    }
}
