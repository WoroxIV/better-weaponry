package com.betterweaponry.config;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.editor.EditorDocument;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reprend la config 1.x ({@code config/better_weaponry-server.toml}) dans le nouveau format JSON.
 * <p>
 * Seules les valeurs différentes des défauts de la 1.1.4 sont copiées dans {@code config/better_weaponry/} :
 * les autres suivront les valeurs du mod, même si elles changent dans une future version.
 * L'ancien fichier est ensuite renommé en {@code .old} pour ne pas être repris deux fois.
 * Les réglages propres à un monde sont repris séparément ({@link #migrateWorld}).
 */
public final class LegacyConfigMigration {
    public static final String LEGACY_FILE_NAME = "better_weaponry-server.toml";
    private static final String SERVER_CONFIG_FOLDER = "serverconfig";
    private static final String LEGACY_CLIENT_FILE_NAME = "better_weaponry-client.toml";
    private static final String OLD_SUFFIX = ".old";

    private record LegacyStats(double attackSpeed, double attackReach, boolean twoHanded, double baseDamage) {
    }

    /** Stats par défaut de la 1.1.4, figées ici : une valeur égale n'avait pas été modifiée par l'utilisateur. */
    private static final Map<String, LegacyStats> DEFAULTS_1_1_4 = Map.ofEntries(
            Map.entry("broadsword", new LegacyStats(1.2, 4.0, false, 4.5)),
            Map.entry("spear", new LegacyStats(1.2, 5.0, false, 4.0)),
            Map.entry("dagger", new LegacyStats(2.0, 2.0, false, 4.0)),
            Map.entry("scythe", new LegacyStats(0.8, 5.0, true, 9.0)),
            Map.entry("rapier", new LegacyStats(1.9, 3.0, false, 3.5)),
            Map.entry("sickle", new LegacyStats(1.4, 2.5, false, 5.5)),
            Map.entry("katana", new LegacyStats(1.6, 3.0, true, 6.0)),
            Map.entry("battleaxe", new LegacyStats(0.9, 4.0, true, 9.0)),
            Map.entry("glaive", new LegacyStats(1.0, 4.5, true, 7.5)),
            Map.entry("hammer", new LegacyStats(0.7, 4.0, true, 11.5)),
            Map.entry("claymore", new LegacyStats(1.1, 4.0, true, 7.5)),
            Map.entry("chakram", new LegacyStats(1.3, 3.0, false, 5.0)),
            Map.entry("sai", new LegacyStats(1.8, 2.5, false, 4.0)));
    private static final double LEGACY_DUAL_WIELD_MULTIPLIER = 1.5D;
    private static final double LEGACY_DAMAGE_ROUNDING = 0.5D;
    private static final double EPSILON = 1.0E-6D;

    private LegacyConfigMigration() {
    }

    /** Les préférences d'affichage étaient dans un fichier TOML : elles passent telles quelles dans le fichier JSON. */
    public static void migrateClientConfig() {
        Path legacy = FMLPaths.CONFIGDIR.get().resolve(LEGACY_CLIENT_FILE_NAME);
        Path converted = FMLPaths.CONFIGDIR.get().resolve(ClientConfig.FILE_NAME);
        if (!Files.isRegularFile(legacy) || Files.exists(converted)) return;
        try (Reader reader = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
            ConfigFolder.writeJson(converted, toJson(new TomlParser().parse(reader)));
        } catch (Exception e) {
            BetterWeaponry.LOGGER.warn("[Better Weaponry] Could not convert {}, the default preferences are used", legacy, e);
            return;
        }
        try {
            Files.move(legacy, legacy.resolveSibling(LEGACY_CLIENT_FILE_NAME + OLD_SUFFIX), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            BetterWeaponry.LOGGER.warn("[Better Weaponry] Could not rename {}", legacy, e);
        }
    }

    /** Les noms et les sections du fichier TOML sont ceux du fichier JSON. */
    private static JsonObject toJson(UnmodifiableConfig config) {
        JsonObject json = new JsonObject();
        config.valueMap().forEach((key, value) -> {
            JsonElement element = toJson(value);
            if (element != null) json.add(key, element);
        });
        return json;
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof UnmodifiableConfig section) return toJson(section);
        if (value instanceof Boolean flag) return new JsonPrimitive(flag);
        if (value instanceof Number number) return new JsonPrimitive(number);
        if (value instanceof Enum<?> constant) return new JsonPrimitive(constant.name());
        if (value instanceof String text) return new JsonPrimitive(text);
        if (value instanceof List<?> list) {
            JsonArray array = new JsonArray();
            for (Object entry : list) {
                JsonElement element = toJson(entry);
                if (element != null) array.add(element);
            }
            return array;
        }
        return null;
    }

    public static void migrateIfPresent() {
        warnAboutDefaultConfigs();
        Path legacy = FMLPaths.CONFIGDIR.get().resolve(LEGACY_FILE_NAME);
        if (!Files.isRegularFile(legacy)) return;

        Optional<Converted> converted = read(legacy);
        if (converted.isEmpty()) return;

        try {
            for (Map.Entry<WeaponType, JsonObject> weapon : converted.get().weapons().entrySet()) {
                mergeInto(ConfigFolder.weaponFile(weapon.getKey()), weapon.getValue(), "../" + ConfigFolder.WEAPON_SCHEMA);
            }
            if (converted.get().rules() != null) {
                mergeInto(ConfigFolder.rulesFile(), converted.get().rules(), "./" + ConfigFolder.RULES_SCHEMA);
            }

            Path renamed = legacy.resolveSibling(LEGACY_FILE_NAME + OLD_SUFFIX);
            Files.move(legacy, renamed, StandardCopyOption.REPLACE_EXISTING);
            BetterWeaponry.LOGGER.info("[Better Weaponry] Converted the 1.x config: {} weapon type(s) and {} global setting(s) "
                            + "different from the defaults were copied to {}. The old file was renamed to {}. "
                            + "Tooltip options are now in better_weaponry-client.json.",
                    converted.get().weapons().size(), converted.get().rules() != null ? 1 : 0, ConfigFolder.displayPath(ConfigFolder.root()), renamed.getFileName());
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("[Better Weaponry] Could not convert the 1.x config {}", legacy, e);
        }
    }

    /**
     * La 1.1.4 gardait ses réglages dans chaque monde ({@code <monde>/serverconfig/}). Ils passent dans le datapack
     * de l'éditeur de ce monde, pour rester propres à ce monde.
     *
     * @return vrai si des réglages ont été écrits : le datapack doit alors être activé
     */
    public static boolean migrateWorld(Path worldRoot) {
        Path legacy = worldRoot.resolve(SERVER_CONFIG_FOLDER).resolve(LEGACY_FILE_NAME);
        if (!Files.isRegularFile(legacy)) return false;
        Optional<Converted> converted = read(legacy);
        if (converted.isEmpty()) return false;

        try {
            for (Map.Entry<WeaponType, JsonObject> weapon : converted.get().weapons().entrySet()) {
                EditorFiles.writeWorld(worldRoot, EditorDocument.weapon(weapon.getKey()), weapon.getValue());
            }
            if (converted.get().rules() != null) EditorFiles.writeWorld(worldRoot, EditorDocument.rules(), converted.get().rules());
            Files.move(legacy, legacy.resolveSibling(LEGACY_FILE_NAME + OLD_SUFFIX), StandardCopyOption.REPLACE_EXISTING);
            BetterWeaponry.LOGGER.info("[Better Weaponry] Converted the 1.x settings of this world into its editor data pack ({} weapon type(s)).",
                    converted.get().weapons().size());
            return !converted.get().weapons().isEmpty() || converted.get().rules() != null;
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("[Better Weaponry] Could not convert the 1.x config {}", legacy, e);
            return false;
        }
    }

    /** Valeurs d'un ancien fichier qui diffèrent des défauts de la 1.1.4. */
    private record Converted(Map<WeaponType, JsonObject> weapons, @Nullable JsonObject rules) {
    }

    private static Optional<Converted> read(Path legacy) {
        CommentedConfig toml;
        try (Reader reader = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
            toml = new TomlParser().parse(reader);
        } catch (Exception e) {
            BetterWeaponry.LOGGER.warn("[Better Weaponry] Could not read the 1.x config {}, it was not converted", legacy, e);
            return Optional.empty();
        }

        Map<WeaponType, JsonObject> weapons = new EnumMap<>(WeaponType.class);
        for (WeaponType type : WeaponType.values()) {
            LegacyStats defaults = DEFAULTS_1_1_4.get(type.id());
            if (defaults == null) continue;
            JsonObject changes = weaponChanges(toml, type, defaults);
            if (changes.size() > 0) weapons.put(type, changes);
        }

        JsonObject rules = null;
        Double multiplier = number(toml, "other", "dual_wield_speed_multiplier");
        if (multiplier != null && !same(multiplier, LEGACY_DUAL_WIELD_MULTIPLIER)) {
            rules = new JsonObject();
            rules.addProperty(RulesDefinition.DUAL_WIELD_SPEED_MULTIPLIER.name(), multiplier);
        }
        return Optional.of(new Converted(weapons, rules));
    }

    /** Valeurs d'une arme qui diffèrent des défauts de la 1.1.4, au format JSON. */
    private static JsonObject weaponChanges(CommentedConfig toml, WeaponType type, LegacyStats defaults) {
        JsonObject changes = new JsonObject();
        Double speed = number(toml, "weapon_stats", type.id(), "attack_speed");
        if (speed != null && !same(speed, defaults.attackSpeed())) {
            changes.addProperty(WeaponDefinition.ATTACK_SPEED.name(), speed);
        }
        Double reach = number(toml, "weapon_stats", type.id(), "attack_reach");
        if (reach != null && !same(reach, defaults.attackReach())) {
            changes.addProperty(WeaponDefinition.ATTACK_REACH.name(), reach);
        }
        Object twoHanded = toml.get(List.of("weapon_stats", type.id(), "two_handed"));
        if (twoHanded instanceof Boolean value && value != defaults.twoHanded()) {
            changes.addProperty(WeaponDefinition.TWO_HANDED.name(), value);
        }

        JsonObject damages = new JsonObject();
        for (WeaponMaterial material : WeaponMaterials.all()) {
            Double damage = number(toml, "weapon_stats", type.id(), material.itemName(type), "attack_damage");
            if (damage != null && !same(damage, legacyDamage(defaults.baseDamage(), material))) {
                damages.addProperty(material.id(), damage);
            }
        }
        if (damages.size() > 0) changes.add(WeaponDefinition.MATERIAL_DAMAGE.name(), damages);
        return changes;
    }

    /** Ajoute {@code changes} au fichier de l'utilisateur (sans effacer ce qu'il contient déjà). */
    private static void mergeInto(Path file, JsonObject changes, String schemaPath) throws IOException {
        JsonObject existing = new JsonObject();
        if (Files.isRegularFile(file)) {
            try {
                JsonElement element = JsonUtil.parseLenient(Files.readString(file, StandardCharsets.UTF_8));
                if (element.isJsonObject()) existing = element.getAsJsonObject();
            } catch (JsonParseException e) {
                BetterWeaponry.LOGGER.warn("[Better Weaponry] {} is not valid JSON, it will be replaced by the converted values", file);
            }
        }
        JsonObject merged = JsonUtil.deepMerge(existing, changes).getAsJsonObject();
        ConfigFolder.writeJson(file, ConfigFolder.withSchema(merged, schemaPath));
    }

    /** Un modpack peut fournir l'ancienne config dans defaultconfigs/ : elle n'est plus utilisée. */
    private static void warnAboutDefaultConfigs() {
        Path defaultConfig = FMLPaths.GAMEDIR.get().resolve("defaultconfigs").resolve(LEGACY_FILE_NAME);
        if (Files.isRegularFile(defaultConfig)) {
            BetterWeaponry.LOGGER.warn("[Better Weaponry] {} is a 1.x config and is no longer used. "
                    + "Put the changed values in config/better_weaponry/ instead.", defaultConfig);
        }
    }

    private static double legacyDamage(double baseDamage, WeaponMaterial material) {
        return Math.round(baseDamage * material.damageMultiplier() / LEGACY_DAMAGE_ROUNDING) * LEGACY_DAMAGE_ROUNDING;
    }

    private static Double number(CommentedConfig toml, String... path) {
        Object value = toml.get(Arrays.asList(path));
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static boolean same(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }
}
