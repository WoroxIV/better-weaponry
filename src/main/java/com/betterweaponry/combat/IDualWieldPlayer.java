package com.betterweaponry.combat;

import org.jetbrains.annotations.Nullable;

/**
 * État du dual wield ajouté à chaque joueur par {@link com.betterweaponry.mixin.PlayerCombatMixin}.
 * Le préfixe "betterweaponry$" évite les collisions avec les méthodes d'autres mods.
 * Pour l'utiliser : {@code if (player instanceof IDualWieldPlayer dw) ...}
 */
public interface IDualWieldPlayer {

    /** Main qui doit frapper au prochain coup (true = main secondaire). */
    boolean betterweaponry$isOffhandTurn();

    void betterweaponry$toggleTurn();

    /** Renvoie vrai si une attaque vient d'avoir lieu, puis remet le drapeau à faux. */
    boolean betterweaponry$consumeJustAttacked();

    boolean betterweaponry$isLastAttackOffhand();

    /** Vrai pendant qu'on relance swing() sur l'autre main, pour éviter une boucle infinie. */
    boolean betterweaponry$isRedirectingSwing();

    void betterweaponry$setRedirectingSwing(boolean redirecting);

    /** Échange de mains en cours (pendant une attaque de la main secondaire), sinon null. */
    @Nullable
    HandSwap betterweaponry$getActiveSwap();
}
