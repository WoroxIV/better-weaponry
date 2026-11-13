package com.betterweaponry.compat;

import com.betterweaponry.data.WeaponData;
import com.betterweaponry.platform.Platform;

/** Les autres mods de combat, seulement détectés : aucun n'est nécessaire, et le code de Better Weaponry ne les appelle jamais. */
public final class BwCompat {
    /** Combat Ascended, et son ancien nom Combat Ascended : les deux sont reconnus. */
    public static final String COMBAT_ASCENDED = "combat_ascended";
    public static final String COMBAT_REFORGED = "combat_reforged";
    public static final String BETTER_COMBAT = "bettercombat";
    public static final String EPIC_FIGHT = "epicfight";

    private static final boolean COMBAT_REFORGED_LOADED = Platform.get().isModLoaded(COMBAT_ASCENDED) || Platform.get().isModLoaded(COMBAT_REFORGED);
    private static final boolean BETTER_COMBAT_LOADED = Platform.get().isModLoaded(BETTER_COMBAT);
    private static final boolean EPIC_FIGHT_LOADED = Platform.get().isModLoaded(EPIC_FIGHT);

    private BwCompat() {
    }

    public static boolean combatReforged() {
        return COMBAT_REFORGED_LOADED;
    }

    public static boolean betterCombat() {
        return BETTER_COMBAT_LOADED;
    }

    public static boolean epicFight() {
        return EPIC_FIGHT_LOADED;
    }

    /** Les armes appliquent leur portée : réglage actif, et aucun mod de la liste n'a son propre système de portée. */
    public static boolean reachActive() {
        CompatDefinition compat = WeaponData.compat();
        return compat.reachEnabled() && compat.reachDisabledByMods().stream().noneMatch(BwCompat::isLoaded);
    }

    /** "combat_reforged" dans la liste désigne aussi Combat Ascended, son nouveau nom. */
    private static boolean isLoaded(String modId) {
        return modId.equals(COMBAT_REFORGED) || modId.equals(COMBAT_ASCENDED) ? COMBAT_REFORGED_LOADED : Platform.get().isModLoaded(modId);
    }

    /** Avec Epic Fight, les mobs gardent leur comportement vanilla : il remplace leurs attaques. */
    public static boolean mobBehaviorsActive() {
        return !EPIC_FIGHT_LOADED;
    }

    /** Dossier du profil d'équilibrage choisi selon les mods chargés. {@code null} : les valeurs de base, faites pour Combat Ascended. */
    public static String balanceProfile() {
        if (EPIC_FIGHT_LOADED) return "epic_fight";
        if (BETTER_COMBAT_LOADED) return "better_combat";
        return COMBAT_REFORGED_LOADED ? null : "standalone";
    }
}
