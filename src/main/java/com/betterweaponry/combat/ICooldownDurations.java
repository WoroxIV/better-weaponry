package com.betterweaponry.combat;

import net.minecraft.world.item.Item;

/** Ajouté aux recharges du jeu (ItemCooldownsMixin) : le jeu ne donne que la part restante, pas la durée totale. */
public interface ICooldownDurations {
    /** Durée totale en ticks de la dernière recharge de cet objet, 0 s'il n'en a pas eu. */
    int betterweaponry$duration(Item item);
}
