package com.betterweaponry.config;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.platform.Platform;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Préférences propres à chaque joueur : config/better_weaponry-client.json.
 * (Les stats des armes et les règles sont dans les fichiers JSON de {@link ConfigFolder}.)
 */
public final class ClientConfig {
    /** Les couleurs proposées pour les lignes de tooltip (les 16 couleurs de Minecraft, sans les styles). */
    public static final List<ChatFormatting> TOOLTIP_COLORS = Arrays.stream(ChatFormatting.values()).filter(ChatFormatting::isColor).toList();
    public static final String FILE_NAME = "better_weaponry-client.json";

    private static final List<Value<?>> VALUES = new ArrayList<>();
    private static boolean loaded;

    public static final Value<ComboHudStyle> COMBO_HUD_STYLE = enumValue(ComboHudStyle.class, ComboHudStyle.DOTS, null, "combo_hud_style");
    public static final Value<Boolean> SHOW_ATTACK_ZONES = bool(false, "show_attack_zones");
    public static final Value<Boolean> TUTORIAL_HINTS = bool(true, "tutorials", "hints");
    private static final Value<List<String>> TUTORIALS_SEEN = strings("tutorials", "seen");
    private static final Value<List<String>> HINTS_SHOWN = strings("tutorials", "hints_shown");
    private static final Map<TooltipCategory, Value<TooltipPlacement>> TOOLTIP_PLACEMENTS = new EnumMap<>(TooltipCategory.class);
    private static final Map<TooltipCategory, Value<ChatFormatting>> TOOLTIP_COLOR_VALUES = new EnumMap<>(TooltipCategory.class);
    private static final Map<HudElement, HudSettings> HUD = new EnumMap<>(HudElement.class);

    static {
        for (HudElement element : HudElement.values()) {
            HUD.put(element, new HudSettings(bool(true, "hud", element.id(), "show"),
                    fraction(element.defaultX(), "hud", element.id(), "x"),
                    fraction(element.defaultY(), "hud", element.id(), "y")));
        }
        for (TooltipCategory category : TooltipCategory.values()) {
            TOOLTIP_PLACEMENTS.put(category, enumValue(TooltipPlacement.class, category.defaultPlacement(), null, "tooltip", category.id(), "placement"));
            TOOLTIP_COLOR_VALUES.put(category, enumValue(ChatFormatting.class, category.defaultColor(), TOOLTIP_COLORS, "tooltip", category.id(), "color"));
        }
    }

    private ClientConfig() {
    }

    /** Une préférence : sa valeur ne s'écrit dans le fichier qu'à l'appel de {@link #save()}. */
    public static final class Value<T> {
        private final String[] path;
        private final T defaultValue;
        private final Function<JsonElement, T> reader;
        private final Function<T, JsonElement> writer;
        private T current;

        private Value(T defaultValue, Function<JsonElement, T> reader, Function<T, JsonElement> writer, String... path) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.reader = reader;
            this.writer = writer;
            this.current = defaultValue;
            VALUES.add(this);
        }

        public T get() {
            ensureLoaded();
            return current;
        }

        public T getDefault() {
            return defaultValue;
        }

        public void set(T value) {
            ensureLoaded();
            current = value;
        }

        private void readFrom(JsonObject root) {
            JsonElement element = root;
            for (String part : path) {
                element = element != null && element.isJsonObject() ? element.getAsJsonObject().get(part) : null;
            }
            T parsed = element == null ? null : reader.apply(element);
            current = parsed != null ? parsed : defaultValue;
        }

        private void writeTo(JsonObject root) {
            JsonObject parent = root;
            for (int i = 0; i < path.length - 1; i++) {
                if (!parent.has(path[i])) parent.add(path[i], new JsonObject());
                parent = parent.getAsJsonObject(path[i]);
            }
            parent.add(path[path.length - 1], writer.apply(current));
        }
    }

    private static Value<Boolean> bool(boolean defaultValue, String... path) {
        return new Value<>(defaultValue, element -> element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() ? element.getAsBoolean() : null,
                JsonPrimitive::new, path);
    }

    /** Une position à l'écran : entre 0 et 1. */
    private static Value<Double> fraction(double defaultValue, String... path) {
        return new Value<>(defaultValue, element -> {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return null;
            double value = element.getAsDouble();
            return value >= 0.0D && value <= 1.0D ? value : null;
        }, JsonPrimitive::new, path);
    }

    /** @param allowed valeurs acceptées, null = toutes */
    private static <E extends Enum<E>> Value<E> enumValue(Class<E> type, E defaultValue, @Nullable List<E> allowed, String... path) {
        return new Value<>(defaultValue, element -> {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return null;
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equals(element.getAsString())) return allowed == null || allowed.contains(constant) ? constant : null;
            }
            return null;
        }, constant -> new JsonPrimitive(constant.name()), path);
    }

    private static Value<List<String>> strings(String... path) {
        return new Value<>(List.of(), element -> {
            if (!element.isJsonArray()) return null;
            List<String> list = new ArrayList<>();
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) list.add(entry.getAsString());
            }
            return List.copyOf(list);
        }, list -> {
            JsonArray array = new JsonArray();
            list.forEach(array::add);
            return array;
        }, path);
    }

    private static Path file() {
        return Platform.get().configDir().resolve(FILE_NAME);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        JsonObject root = new JsonObject();
        Path file = file();
        if (Files.isRegularFile(file)) {
            try {
                JsonElement element = JsonUtil.parseLenient(Files.readString(file, StandardCharsets.UTF_8));
                if (element.isJsonObject()) root = element.getAsJsonObject();
            } catch (IOException | JsonParseException e) {
                BetterWeaponry.LOGGER.warn("[Better Weaponry] Could not read {}, the default preferences are used", file, e);
            }
        }
        for (Value<?> value : VALUES) value.readFrom(root);
    }

    /** Écrit toutes les préférences dans le fichier. */
    public static void save() {
        ensureLoaded();
        JsonObject root = new JsonObject();
        for (Value<?> value : VALUES) value.writeTo(root);
        try {
            ConfigFolder.writeJson(file(), root);
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("[Better Weaponry] Could not write {}", file(), e);
        }
    }

    /** Réglages d'un élément de l'interface. */
    public record HudSettings(Value<Boolean> show, Value<Double> x, Value<Double> y) {
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
        save();
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

    public static Value<TooltipPlacement> placement(TooltipCategory category) {
        return TOOLTIP_PLACEMENTS.get(category);
    }

    public static Value<ChatFormatting> color(TooltipCategory category) {
        return TOOLTIP_COLOR_VALUES.get(category);
    }

    /** Applique un préréglage (placements et couleurs) et l'enregistre. */
    public static void applyPreset(TooltipPreset preset) {
        for (TooltipCategory category : TooltipCategory.values()) {
            placement(category).set(preset.placementOf(category));
            color(category).set(category.defaultColor());
        }
        save();
    }

    public static boolean isTutorialSeen(String scene) {
        return TUTORIALS_SEEN.get().contains(scene);
    }

    public static void markTutorialSeen(String scene) {
        addOnce(TUTORIALS_SEEN, scene);
    }

    /** @param id type d'arme ou tutoriel déjà proposé (les deux listes d'identifiants ne se recoupent pas) */
    public static boolean isHintShown(String id) {
        return HINTS_SHOWN.get().contains(id);
    }

    public static void markHintShown(String id) {
        addOnce(HINTS_SHOWN, id);
    }

    private static void addOnce(Value<List<String>> list, String id) {
        if (list.get().contains(id)) return;
        List<String> updated = new ArrayList<>(list.get());
        updated.add(id);
        list.set(List.copyOf(updated));
        save();
    }

    /** Valeur d'une préférence (raccourci, le fichier est lu à la première demande). */
    public static <T> T get(Value<T> value) {
        return value.get();
    }
}
