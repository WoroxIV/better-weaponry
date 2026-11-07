package com.betterweaponry.combat;

import com.betterweaponry.compat.BetterCombatCompat;
import com.betterweaponry.item.WeaponItem;
import net.minecraft.world.entity.player.Player;

/**
 * Règles des armes à deux mains : tant que le joueur en tient une en main principale,
 * la main secondaire est inutilisable et cachée (événements + mixins de rendu).
 */
public final class TwoHandedSystem {

    private TwoHandedSystem() {
    }

    /** Toujours faux si Better Combat est installé : il gère lui-même les armes à deux mains. */
    public static boolean isOffhandLocked(Player player) {
        return !BetterCombatCompat.isLoaded() && WeaponItem.isTwoHanded(player.getMainHandItem());
    }
}
