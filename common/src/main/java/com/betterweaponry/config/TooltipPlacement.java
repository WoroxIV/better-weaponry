package com.betterweaponry.config;

/** Où s'affiche une catégorie de lignes dans le tooltip d'une arme. */
public enum TooltipPlacement {
    /** Sous le nom de l'arme, au-dessus de "En main principale :". */
    ABOVE_STATS("above_stats"),
    /** Avec les stats vertes. */
    STATS("stats"),
    /** Après les stats, seulement en maintenant la touche "Détails de l'arme". */
    DETAILS("details"),
    HIDDEN("hidden");

    private final String id;

    TooltipPlacement(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
