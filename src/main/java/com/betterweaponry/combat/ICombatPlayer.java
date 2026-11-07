package com.betterweaponry.combat;

/**
 * Ajouté à chaque joueur par {@link com.betterweaponry.mixin.PlayerCombatMixin} pour lui donner un
 * {@link PlayerCombatState}. Le préfixe "betterweaponry$" évite les collisions avec les méthodes d'autres mods.
 * Utiliser plutôt {@link PlayerCombatState#of}.
 */
public interface ICombatPlayer {
    PlayerCombatState betterweaponry$combat();
}
