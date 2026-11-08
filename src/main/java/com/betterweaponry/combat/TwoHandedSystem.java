package com.betterweaponry.combat;

import com.betterweaponry.item.WeaponItem;
import net.minecraft.world.entity.player.Player;

/**
 * Règles des armes à deux mains : tant que le joueur en tient une en main principale,
 * la main secondaire est inutilisable et cachée (événements + mixins de rendu).
 */
public final class TwoHandedSystem {

    private TwoHandedSystem() {
    }

    /** Toujours faux si les armes à deux mains sont coupées (rules.json, Better Combat, Epic Fight). */
    public static boolean isOffhandLocked(Player player) {
        return WeaponItem.isTwoHanded(player.getMainHandItem());
    }
}
