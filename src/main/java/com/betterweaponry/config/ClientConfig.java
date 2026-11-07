package com.betterweaponry.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Préférences propres à chaque joueur : config/better_weaponry-client.toml.
 * (Les stats des armes et les règles sont dans les fichiers JSON, voir {@link ConfigFolder}.)
 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_ATTACK_REACH_TOOLTIP;
    public static final ModConfigSpec.BooleanValue SHOW_TWO_HANDED_TOOLTIP;
    public static final ModConfigSpec.BooleanValue SHOW_COMBO_HUD;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SHOW_ATTACK_REACH_TOOLTIP = builder
                .comment("Show the 'Attack Reach' stat on weapon tooltips.")
                .define("show_attack_reach_tooltip", true);
        SHOW_TWO_HANDED_TOOLTIP = builder
                .comment("Show 'Two Handed' on tooltips of two-handed weapons.")
                .define("show_two_handed_tooltip", true);
        SHOW_COMBO_HUD = builder
                .comment("Show the combo progress dots under the crosshair.")
                .define("show_combo_hud", true);
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    /** Valeur de la config, ou sa valeur par défaut si le fichier n'est pas encore chargé. */
    public static boolean get(ModConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
