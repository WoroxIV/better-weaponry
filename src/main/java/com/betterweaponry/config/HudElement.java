package com.betterweaponry.config;

/**
 * Un élément de l'interface en jeu, déplaçable dans l'écran de placement.
 * La position est celle du centre de l'élément, en fraction de l'écran (0 = gauche ou haut, 1 = droite ou bas).
 */
public enum HudElement {
    /** Juste sous le viseur. */
    COMBO("combo", 0.5D, 0.57D),
    /** En bas à gauche, loin du centre de l'écran pour ne pas gêner la vue. */
    GUARD_BREAK("guard_break", 0.1D, 0.85D),
    /** En bas à droite. */
    ABILITY_COOLDOWN("ability_cooldown", 0.9D, 0.85D);

    private final String id;
    private final double defaultX;
    private final double defaultY;

    HudElement(String id, double defaultX, double defaultY) {
        this.id = id;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    public String id() {
        return id;
    }

    public double defaultX() {
        return defaultX;
    }

    public double defaultY() {
        return defaultY;
    }
}
