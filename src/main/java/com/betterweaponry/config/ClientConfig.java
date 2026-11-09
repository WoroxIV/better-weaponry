package com.betterweaponry.config;

import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
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
    public static final ModConfigSpec.EnumValue<ComboHudStyle> COMBO_HUD_STYLE;
    public static final ModConfigSpec.BooleanValue SHOW_ATTACK_ZONES;
    public static final ModConfigSpec.BooleanValue TUTORIAL_HINTS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TUTORIALS_SEEN;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> HINTS_SHOWN;
    private static final Map<TooltipCategory, ModConfigSpec.EnumValue<TooltipPlacement>> TOOLTIP_PLACEMENTS = new EnumMap<>(TooltipCategory.class);
    private static final Map<TooltipCategory, ModConfigSpec.EnumValue<ChatFormatting>> TOOLTIP_COLOR_VALUES = new EnumMap<>(TooltipCategory.class);
    private static final Map<HudElement, HudSettings> HUD = new EnumMap<>(HudElement.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COMBO_HUD_STYLE = builder
                .comment("Look of the combo progress: dots, squares, bar, ring (around the crosshair) or counter.")
                .defineEnum("combo_hud_style", ComboHudStyle.DOTS);
        SHOW_ATTACK_ZONES = builder
                .comment("Debug view: draw the reach, the combo zones and the sweep zone of the held weapon in the world.")
                .define("show_attack_zones", false);

        builder.comment("On-screen elements: shown or not, and position of their center (0 = left or top, 1 = right or bottom).").push("hud");
        for (HudElement element : HudElement.values()) {
            builder.push(element.id());
            HUD.put(element, new HudSettings(builder.define("show", true),
                    builder.defineInRange("x", element.defaultX(), 0.0D, 1.0D),
                    builder.defineInRange("y", element.defaultY(), 0.0D, 1.0D)));
            builder.pop();
        }
        builder.pop();

        builder.comment("Layout of weapon tooltips: where each kind of line goes, and its color.").push("tooltip");
        for (TooltipCategory category : TooltipCategory.values()) {
            builder.push(category.id());
            TOOLTIP_PLACEMENTS.put(category, builder.defineEnum("placement", category.defaultPlacement()));
            TOOLTIP_COLOR_VALUES.put(category, builder.defineEnum("color", category.defaultColor(), TOOLTIP_COLORS));
            builder.pop();
        }
        builder.pop();

        builder.comment("Tutorials: suggestions, and what this player has already seen.").push("tutorials");
        TUTORIAL_HINTS = builder.comment("Suggest a tutorial the first time you get a weapon.").define("hints", true);
        TUTORIALS_SEEN = builder.comment("Tutorials already opened.").defineListAllowEmpty("seen", List.of(), () -> "", value -> value instanceof String);
        HINTS_SHOWN = builder.comment("Weapon types and tutorials already suggested.").defineListAllowEmpty("hints_shown", List.of(), () -> "", value -> value instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    /** Réglages d'un élément de l'interface. */
    public record HudSettings(ModConfigSpec.BooleanValue show, ModConfigSpec.DoubleValue x, ModConfigSpec.DoubleValue y) {
    }

    public static HudSettings hud(HudElement element) {
        return HUD.get(element);
    }

    /** Remet tous les éléments de l'interface à leur place d'origine (ils restent affichés ou non). */
    public static void resetHudPositions() {
        for (HudElement element : HudElement.values()) {
            hud(element).x().set(element.defaultX());
            hud(element).y().set(element.defaultY());
        }
        SPEC.save();
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

    public static boolean isTutorialSeen(String scene) {
        return get(TUTORIALS_SEEN).contains(scene);
    }

    public static void markTutorialSeen(String scene) {
        addOnce(TUTORIALS_SEEN, scene);
    }

    /** @param id type d'arme ou tutoriel déjà proposé (les deux listes d'identifiants ne se recoupent pas) */
    public static boolean isHintShown(String id) {
        return get(HINTS_SHOWN).contains(id);
    }

    public static void markHintShown(String id) {
        addOnce(HINTS_SHOWN, id);
    }

    private static void addOnce(ModConfigSpec.ConfigValue<List<? extends String>> list, String id) {
        if (!SPEC.isLoaded() || list.get().contains(id)) return;
        List<String> updated = new ArrayList<>(list.get());
        updated.add(id);
        list.set(updated);
        SPEC.save();
    }

    /** Valeur de la config, ou sa valeur par défaut si le fichier n'est pas encore chargé. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
