package com.betterweaponry.mixin;

import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.IDualWieldPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Partie "LivingEntity" du dual wield. Ces méthodes sont définies dans LivingEntity,
 * mais on n'agit que sur les joueurs (seuls à implémenter IDualWieldPlayer).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Le jeu fait toujours balancer la main principale quand on frappe :
     * en dual wield, on redirige l'animation vers la main dont c'est le tour.
     */
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$alternateSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        if (!((Object) this instanceof IDualWieldPlayer dw)) return;
        Player player = (Player) (Object) this;

        // Consommé à chaque swing pour qu'une ancienne attaque n'influence jamais un swing plus tard.
        boolean justAttacked = dw.betterweaponry$consumeJustAttacked();

        // Pendant notre propre appel redirigé, on laisse le jeu faire.
        if (dw.betterweaponry$isRedirectingSwing() || !DualWieldSystem.isDualWielding(player)) return;

        // Après une attaque, on anime la main qui a réellement frappé ; sinon (clic dans le vide) on suit le tour.
        boolean offhandTurn = justAttacked ? dw.betterweaponry$isLastAttackOffhand() : dw.betterweaponry$isOffhandTurn();
        dw.betterweaponry$toggleTurn();

        InteractionHand expectedHand = offhandTurn ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (hand != expectedHand) {
            ci.cancel();
            dw.betterweaponry$setRedirectingSwing(true);
            try {
                player.swing(expectedHand, updateSelf);
            } finally {
                dw.betterweaponry$setRedirectingSwing(false);
            }
        }
    }

    /**
     * Si l'arme secondaire casse pendant son attaque, le jeu (qui la croit en main principale)
     * signalerait la casse de la main principale : son et particules seraient ceux de la mauvaise arme.
     * On inverse donc la main signalée tant que les mains sont échangées.
     */
    @ModifyVariable(method = "onEquippedItemBroken", at = @At("HEAD"), argsOnly = true)
    private EquipmentSlot betterweaponry$reportBrokenHandCorrectly(EquipmentSlot slot) {
        if ((Object) this instanceof IDualWieldPlayer dw && dw.betterweaponry$getActiveSwap() != null) {
            if (slot == EquipmentSlot.MAINHAND) return EquipmentSlot.OFFHAND;
            if (slot == EquipmentSlot.OFFHAND) return EquipmentSlot.MAINHAND;
        }
        return slot;
    }
}
