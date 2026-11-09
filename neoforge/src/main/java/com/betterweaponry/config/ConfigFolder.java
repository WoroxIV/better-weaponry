package com.betterweaponry.config;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Le dossier {@code config/better_weaponry/}, préparé à chaque lancement :
 * <pre>
 * config/better_weaponry/
 * ├── README.txt            mode d'emploi, dans la langue du jeu
 * ├── weapon.schema.json    autocomplétion et aide au survol dans VS Code
 * ├── rules.schema.json
 * ├── gear.schema.json
 * ├── rules.json            règles globales modifiées par l'utilisateur (jamais écrasé)
 * ├── gear.json             réglages de l'équipement modifiés par l'utilisateur (jamais écrasé)
 * ├── weapons/&lt;type&gt;.json   stats modifiées par l'utilisateur (jamais écrasés)
 * └── defaults/             copie des valeurs par défaut du mod, réécrite à chaque lancement
 * </pre>
 * Les fichiers de l'utilisateur ne contiennent que ce qu'il change : le reste suit les valeurs du mod,
 * même après une mise à jour.
 */
public final class ConfigFolder {
    public static final String README_FILE = "README.txt";
    public static final String WEAPON_SCHEMA = "weapon.schema.json";
    public static final String RULES_SCHEMA = "rules.schema.json";
    public static final String GEAR_SCHEMA = "gear.schema.json";
    private static final String DEFAULTS_DIRECTORY = "defaults";

    private ConfigFolder() {
    }

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve(BetterWeaponry.MODID);
    }

    public static Path weaponFile(WeaponType type) {
        return root().resolve(WeaponDataLoader.weaponPath(type));
    }

    public static Path rulesFile() {
        return root().resolve(WeaponDataLoader.RULES_FILE);
    }

    public static Path gearFile() {
        return root().resolve(WeaponDataLoader.GEAR_FILE);
    }

    /** Chemin affiché dans les messages, relatif au dossier du jeu, ex : "config/better_weaponry/weapons/katana.json". */
    public static String displayPath(Path file) {
        Path gameDirectory = FMLPaths.GAMEDIR.get();
        Path shown = file.startsWith(gameDirectory) ? gameDirectory.relativize(file) : file;
        return shown.toString().replace('\\', '/');
    }

    /**
     * Crée ou met à jour le dossier. Appelé une fois au démarrage, avant le chargement de tout monde.
     *
     * @param languageCode langue du README et des schémas (ex : "fr_fr")
     */
    public static void prepare(String languageCode) {
        try {
            Files.createDirectories(root().resolve(WeaponDataLoader.WEAPONS_DIRECTORY));
            // Avant de créer les fichiers vides : la migration y écrit les anciennes valeurs modifiées.
            LegacyConfigMigration.migrateIfPresent();
            createUserFiles();
            writeDefaults();

            Docs docs = Docs.load(languageCode);
            writeJson(root().resolve(WEAPON_SCHEMA), SchemaWriter.schema(WeaponDefinition.FIELDS,
                    docs.text(BetterWeaponry.MODID + ".doc.schema.weapon"), docs));
            writeJson(root().resolve(RULES_SCHEMA), SchemaWriter.schema(RulesDefinition.FIELDS,
                    docs.text(BetterWeaponry.MODID + ".doc.schema.rules"), docs));
            writeJson(root().resolve(GEAR_SCHEMA), SchemaWriter.schema(GearDefinition.FIELDS,
                    docs.text(BetterWeaponry.MODID + ".doc.schema.gear"), docs));
            writeText(root().resolve(README_FILE), ReadmeWriter.write(docs));
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("Could not prepare the config folder {}", root(), e);
        }
    }

    /** Un fichier vide par arme, relié au schéma : il suffit d'y ajouter les champs à changer. Jamais écrasés. */
    private static void createUserFiles() throws IOException {
        for (WeaponType type : WeaponType.values()) {
            createIfAbsent(weaponFile(type), withSchema(new JsonObject(), "../" + WEAPON_SCHEMA));
        }
        createIfAbsent(rulesFile(), withSchema(new JsonObject(), "./" + RULES_SCHEMA));
        createIfAbsent(gearFile(), withSchema(new JsonObject(), "./" + GEAR_SCHEMA));
    }

    /** Copie des valeurs par défaut du jar, réécrite à chaque lancement pour rester à jour. */
    private static void writeDefaults() throws IOException {
        Path defaults = root().resolve(DEFAULTS_DIRECTORY);
        for (WeaponType type : WeaponType.values()) {
            String path = WeaponDataLoader.weaponPath(type);
            Optional<JsonObject> json = WeaponDataLoader.builtinJson(path);
            if (json.isPresent()) writeJson(defaults.resolve(path), withSchema(json.get(), "../../" + WEAPON_SCHEMA));
        }
        Optional<JsonObject> rules = WeaponDataLoader.builtinJson(WeaponDataLoader.RULES_FILE);
        if (rules.isPresent()) writeJson(defaults.resolve(WeaponDataLoader.RULES_FILE), withSchema(rules.get(), "../" + RULES_SCHEMA));
        Optional<JsonObject> gear = WeaponDataLoader.builtinJson(WeaponDataLoader.GEAR_FILE);
        if (gear.isPresent()) writeJson(defaults.resolve(WeaponDataLoader.GEAR_FILE), withSchema(gear.get(), "../" + GEAR_SCHEMA));
    }

    /** Copie de {@code content} avec le lien vers le schéma en premier (c'est ce qui active l'aide dans VS Code). */
    static JsonObject withSchema(JsonObject content, String schemaPath) {
        JsonObject result = new JsonObject();
        result.addProperty(FieldSet.SCHEMA_KEY, schemaPath);
        for (var entry : content.entrySet()) {
            if (!entry.getKey().equals(FieldSet.SCHEMA_KEY)) result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

    static void writeJson(Path file, JsonObject json) throws IOException {
        writeText(file, JsonUtil.PRETTY.toJson(json) + "\n");
    }

    /** N'écrit que si le contenu change, pour ne pas toucher aux fichiers inutilement à chaque lancement. */
    static void writeText(Path file, String content) throws IOException {
        if (Files.isRegularFile(file) && Files.readString(file, StandardCharsets.UTF_8).equals(content)) return;
        Files.createDirectories(file.getParent());
        // Fichier temporaire puis déplacement : une coupure pendant l'écriture ne peut pas laisser un fichier tronqué.
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void createIfAbsent(Path file, JsonObject json) throws IOException {
        if (!Files.exists(file)) writeJson(file, json);
    }
}
