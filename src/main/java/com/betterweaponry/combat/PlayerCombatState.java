package com.betterweaponry.combat;

import com.betterweaponry.data.ComboStep;
import com.betterweaponry.item.WeaponType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * État de combat d'un joueur : dual wield, combo, animation à deux bras.
 * Un objet par joueur, ajouté par {@link com.betterweaponry.mixin.PlayerCombatMixin} ; le client et le serveur
 * ont chacun le leur et le font évoluer avec la même logique (le serveur corrige le combo du client si besoin).
 * C'est un simple conteneur de valeurs : la logique est dans {@link DualWieldSystem}, {@link ComboSystem}
 * et LivingEntityMixin.
 */
public final class PlayerCombatState {

    // --- Dual wield ---

    /** Main qui doit frapper au prochain coup (true = main secondaire). */
    public boolean offhandTurn;
    /** Une attaque vient d'avoir lieu : le prochain coup de bras (swing) l'anime. Remis à faux par le swing. */
    public boolean justAttacked;
    /** La dernière attaque a été portée par la main secondaire. */
    public boolean lastAttackOffhand;
    /** Vrai pendant qu'on relance swing() sur l'autre main, pour éviter une boucle infinie. */
    public boolean redirectingSwing;
    /** Échange de mains en cours (pendant une attaque de la main secondaire), sinon null. */
    @Nullable
    public HandSwap activeSwap;

    // --- Combo ---

    /** Coup du combo qui va être porté (0 = premier). */
    public int comboStep;
    /** Moment (temps du monde, en ticks) après lequel le combo repart du début. */
    public long comboExpiresAt;
    /** Type d'arme du combo en cours : changer d'arme remet le combo à zéro. */
    @Nullable
    public WeaponType comboType;
    /** La dernière attaque était un coup spécial : l'animation part de la main principale, et le tour y revient. */
    public boolean lastAttackSpecial;
    /** Recharge imposée au prochain coup (ticks) après un coup spécial, 0 = recharge normale. Vaut pour comboType. */
    public float nextAttackDelay;
    /** Coup spécial en cours (pendant attack()), pour couper le coup balayé vanilla d'un coup en zone. */
    @Nullable
    public ComboStep activeStep;

    // --- Animation à deux bras ---

    /** Un coup à deux armes vient d'être porté : le prochain coup de bras animera les deux bras. */
    public boolean dualSwingPending;
    /** Le coup de bras en cours anime les deux bras. */
    public boolean dualSwingActive;

    /** Remet le combo au début (arme changée, combo désactivé...). */
    public void resetCombo() {
        comboStep = 0;
        comboType = null;
        nextAttackDelay = 0.0F;
    }

    /** État de ce joueur (tous les joueurs en ont un, grâce au mixin). */
    public static PlayerCombatState of(Player player) {
        return ((ICombatPlayer) player).betterweaponry$combat();
    }
}
