package com.betterweaponry.compat;

import net.neoforged.fml.ModList;

/**
 * Better Combat gère lui-même les armes à deux mains et le dual wield :
 * quand il est présent, on désactive nos propres systèmes pour ne pas entrer en conflit.
 */
public final class BetterCombatCompat {
    public static final String MOD_ID = "bettercombat";

    // Calculé une seule fois, au premier appel (la liste des mods ne change pas en cours de partie).
    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private BetterCombatCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }
}
