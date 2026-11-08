package com.betterweaponry.config;

import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Préférences propres à chaque joueur : config/better_weaponry-client.toml.
 * (Les stats des armes et les règles sont dans les fichiers JSON, voir {@link ConfigFolder}.)
 */
public final class ClientConfig {
    /** Les couleurs proposées pour les lignes de tooltip (les 16 couleurs de Minecraft, sans les styles). */
    public static final List<ChatFormatting> TOOLTIP_COLORS = Arrays.stream(ChatFormatting.values()).filter(ChatFormatting::isColor).toList();

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_COMBO_HUD;
    private static final Map<TooltipCategory, ModConfigSpec.EnumValue<TooltipPlacement>> TOOLTIP_PLACEMENTS = new EnumMap<>(TooltipCategory.class);
    private static final Map<TooltipCategory, ModConfigSpec.EnumValue<ChatFormatting>> TOOLTIP_COLOR_VALUES = new EnumMap<>(TooltipCategory.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SHOW_COMBO_HUD = builder
                .comment("Show the combo progress under the crosshair.")
                .define("show_combo_hud", true);

        builder.comment("Layout of weapon tooltips: where each kind of line goes, and its color.").push("tooltip");
        for (TooltipCategory category : TooltipCategory.values()) {
            builder.push(category.id());
            TOOLTIP_PLACEMENTS.put(category, builder.defineEnum("placement", category.defaultPlacement()));
            TOOLTIP_COLOR_VALUES.put(category, builder.defineEnum("color", category.defaultColor(), TOOLTIP_COLORS));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    /** Préréglages de la page Tooltips de l'éditeur. */
    public enum TooltipPreset {
        DEFAULT("default"),
        ALL_VISIBLE("all_visible"),
        MINIMAL("minimal");

        private final String id;

        TooltipPreset(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        private TooltipPlacement placementOf(TooltipCategory category) {
            return switch (this) {
                case DEFAULT -> category.defaultPlacement();
                case ALL_VISIBLE -> category == TooltipCategory.HANDS ? TooltipPlacement.ABOVE_STATS : TooltipPlacement.STATS;
                case MINIMAL -> category.isMainStat() ? TooltipPlacement.STATS : TooltipPlacement.HIDDEN;
            };
        }
    }

    public static ModConfigSpec.EnumValue<TooltipPlacement> placement(TooltipCategory category) {
        return TOOLTIP_PLACEMENTS.get(category);
    }

    public static ModConfigSpec.EnumValue<ChatFormatting> color(TooltipCategory category) {
        return TOOLTIP_COLOR_VALUES.get(category);
    }

    /** Applique un préréglage (placements et couleurs) et l'enregistre. */
    public static void applyPreset(TooltipPreset preset) {
        for (TooltipCategory category : TooltipCategory.values()) {
            placement(category).set(preset.placementOf(category));
            color(category).set(category.defaultColor());
        }
        SPEC.save();
    }

    /** Valeur de la config, ou sa valeur par défaut si le fichier n'est pas encore chargé. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
