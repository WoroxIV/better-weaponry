package com.betterweaponry.compat;

import com.betterweaponry.platform.Platform;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;

/**
 * Better Combat et Epic Fight remplacent le système de combat du jeu (combos, portée, dual wield, deux mains...) :
 * quand l'un d'eux est présent, tous nos systèmes de combat se coupent (voir {@code RulesDefinition}).
 * Les armes elles-mêmes et leurs enchantements restent disponibles.
 */
public final class CombatCompat {
    public static final String BETTER_COMBAT = "bettercombat";
    public static final String EPIC_FIGHT = "epicfight";

    // Calculé une seule fois, au premier appel (la liste des mods ne change pas en cours de partie).
    private static final List<String> LOADED = List.of(BETTER_COMBAT, EPIC_FIGHT).stream().filter(Platform.get()::isModLoaded).toList();

    /** Tests automatiques uniquement : fait comme si un mod de combat était installé. */
    private static volatile boolean simulated;

    private CombatCompat() {
    }

    /** Vrai si un mod de combat installé remplace nos systèmes. */
    public static boolean externalCombatMod() {
        return simulated || !LOADED.isEmpty();
    }

    /** Nom du mod de combat installé, pour l'afficher au joueur. */
    public static Optional<String> externalCombatModName() {
        if (LOADED.isEmpty()) return simulated ? Optional.of("Better Combat") : Optional.empty();
        return Optional.of(LOADED.getFirst().equals(BETTER_COMBAT) ? "Better Combat" : "Epic Fight");
    }

    /**
     * Joueur simulé par un mod (déployeur de Create...) : il tient une arme simple, qui applique ses dégâts et rien d'autre.
     * Dual wield, combos, parade, dash et effets des armes ne servent qu'aux vrais joueurs et aux mobs.
     */
    public static boolean isFakePlayer(Entity entity) {
        return Platform.get().isFakePlayer(entity);
    }

    public static void simulateForTests(boolean value) {
        simulated = value;
    }
}
