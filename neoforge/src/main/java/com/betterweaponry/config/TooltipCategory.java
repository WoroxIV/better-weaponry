package com.betterweaponry.config;

import net.minecraft.ChatFormatting;

/**
 * Une catégorie de lignes du tooltip d'une arme, avec son placement et sa couleur par défaut.
 * L'ordre de déclaration est l'ordre d'affichage à l'intérieur d'un même placement.
 */
public enum TooltipCategory {
    HANDS("hands", TooltipPlacement.ABOVE_STATS, ChatFormatting.DARK_GRAY),
    REACH("reach", TooltipPlacement.STATS, ChatFormatting.DARK_GREEN),
    DAMAGE("damage", TooltipPlacement.STATS, ChatFormatting.DARK_GREEN),
    SPEED("speed", TooltipPlacement.STATS, ChatFormatting.DARK_GREEN),
    ARMOR_PENETRATION("armor_penetration", TooltipPlacement.STATS, ChatFormatting.DARK_GREEN),
    SWEEP("sweep", TooltipPlacement.HIDDEN, ChatFormatting.DARK_GREEN),
    PROJECTILE_SPEED("projectile_speed", TooltipPlacement.HIDDEN, ChatFormatting.DARK_GREEN),
    SHIELD_DISABLING("shield_disabling", TooltipPlacement.HIDDEN, ChatFormatting.DARK_GREEN),
    EFFECTS("effects", TooltipPlacement.DETAILS, ChatFormatting.BLUE),
    ABILITY("ability", TooltipPlacement.DETAILS, ChatFormatting.GOLD),
    COMBO("combo", TooltipPlacement.DETAILS, ChatFormatting.YELLOW);

    private final String id;
    private final TooltipPlacement defaultPlacement;
    private final ChatFormatting defaultColor;

    TooltipCategory(String id, TooltipPlacement defaultPlacement, ChatFormatting defaultColor) {
        this.id = id;
        this.defaultPlacement = defaultPlacement;
        this.defaultColor = defaultColor;
    }

    public String id() {
        return id;
    }

    public TooltipPlacement defaultPlacement() {
        return defaultPlacement;
    }

    public ChatFormatting defaultColor() {
        return defaultColor;
    }

    /** Les stats principales de l'arme, qui restent visibles dans le préréglage "Minimal". */
    public boolean isMainStat() {
        return this == REACH || this == DAMAGE || this == SPEED;
    }
}
