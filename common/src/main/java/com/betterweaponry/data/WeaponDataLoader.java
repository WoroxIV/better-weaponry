package com.betterweaponry.data;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.compat.CompatDefinition;
import com.betterweaponry.config.ConfigFolder;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Charge les stats des armes, au chargement d'un monde et à chaque {@code /reload}.
 * <p>
 * Pour chaque fichier, les couches sont fusionnées dans cet ordre (la dernière gagne) :
 * <ol>
 *   <li>les valeurs par défaut du mod ;</li>
 *   <li>le profil d'équilibrage choisi selon les mods chargés ({@code balance/<profil>/}) ;</li>
 *   <li>le dossier {@code config/better_weaponry/} (tous les mondes) ;</li>
 *   <li>les datapacks du monde ({@code <monde>/datapacks}).</li>
 * </ol>
 * Chemin dans un datapack : {@code data/better_weaponry/better_weaponry/weapons/<type>.json}.
 */
public final class WeaponDataLoader extends SimplePreparableReloadListener<WeaponDataLoader.Result> {
    /** Dossier des fichiers du mod dans un datapack : data/better_weaponry/better_weaponry/... */
    public static final String DATA_DIRECTORY = "better_weaponry";
    public static final String WEAPONS_DIRECTORY = "weapons";
    public static final String COMPAT_FILE = "compat.json";
    public static final String GEAR_FILE = "gear.json";
    private static final String BALANCE_DIRECTORY = "balance";

    /** Les datapacks placés dans le dossier du monde ont un identifiant qui commence par "file/". */
    private static final String WORLD_DATAPACK_PREFIX = "file/";
    /**
     * Réglages de combat des fichiers écrits avec Better Weaponry 2.0 : ils appartiennent maintenant à Combat Ascended.
     * Ignorés sans message, ils ne sont pas des fautes de frappe.
     */
    private static final Set<String> FORMER_COMBAT_FIELDS = Set.of("two_handed", "dual_wielded", "armor_penetration", "disables_shield", "on_hit_effects", "sweep",
            "ability", "parry", "dash", "combo", "items", "balance_abilities", "shield_perfect_window_seconds", "shield_perfect_cooldown_seconds",
            "shield_perfect_damage_bonus", "shield_perfect_bonus_seconds", "shield_perfect_knockback", "reflect_arrows", "reflective_cooldown_seconds");

    /** Problèmes du dernier chargement, montrés aux opérateurs qui se connectent. */
    private static volatile List<DataProblem> lastProblems = List.of();

    public record Result(WeaponData.Snapshot snapshot, List<DataProblem> problems) {
    }

    public static List<DataProblem> lastProblems() {
        return lastProblems;
    }

    @Override
    protected Result prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return load(resourceManager);
    }

    @Override
    protected void apply(Result result, ResourceManager resourceManager, ProfilerFiller profiler) {
        install(result);
    }

    /** Lit et fusionne tous les fichiers. */
    public static Result load(ResourceManager resourceManager) {
        List<DataProblem> problems = new ArrayList<>();
        Map<WeaponType, WeaponDefinition> weapons = new EnumMap<>(WeaponType.class);
        for (WeaponType type : WeaponType.values()) {
            List<FieldSet.Layer> layers = collectLayers(resourceManager, weaponPath(type), ConfigFolder.weaponFile(type), type, problems);
            weapons.put(type, decodeWeapon(type, layers, problems));
        }
        CompatDefinition compat = decode(CompatDefinition.CODEC, CompatDefinition.FIELDS.merge(collectLayers(resourceManager, COMPAT_FILE, ConfigFolder.compatFile(), null, problems), problems),
                CompatDefinition.FALLBACK, COMPAT_FILE, problems);
        GearDefinition gear = decode(GearDefinition.CODEC, GearDefinition.FIELDS.merge(collectLayers(resourceManager, GEAR_FILE, ConfigFolder.gearFile(), null, problems), problems),
                GearDefinition.FALLBACK, GEAR_FILE, problems);
        return new Result(new WeaponData.Snapshot(weapons, compat, gear), List.copyOf(problems));
    }

    /** Met en vigueur un chargement et signale ses problèmes dans le log. */
    public static void install(Result result) {
        lastProblems = result.problems();
        for (DataProblem problem : result.problems()) {
            BetterWeaponry.LOGGER.warn("[Better Weaponry] {}", problem.toLogString());
        }
        WeaponData.install(result.snapshot());
        BetterWeaponry.LOGGER.info("Better Weaponry: weapon data loaded ({} weapon types, {} problem(s))",
                result.snapshot().weapons().size(), result.problems().size());
    }

    /** Lit uniquement les fichiers du jar : sert de valeurs de départ, côté client comme côté serveur. */
    static WeaponData.Snapshot loadBuiltin() {
        List<DataProblem> problems = new ArrayList<>();
        Map<WeaponType, WeaponDefinition> weapons = new EnumMap<>(WeaponType.class);
        for (WeaponType type : WeaponType.values()) {
            List<FieldSet.Layer> layers = new ArrayList<>(builtinLayer(weaponPath(type), problems));
            String profile = BwCompat.balanceProfile();
            if (profile != null) layers.addAll(builtinLayer(BALANCE_DIRECTORY + "/" + profile + "/" + type.id() + ".json", problems));
            weapons.put(type, decodeWeapon(type, layers, problems));
        }
        CompatDefinition compat = decode(CompatDefinition.CODEC, CompatDefinition.FIELDS.merge(builtinLayer(COMPAT_FILE, problems), problems), CompatDefinition.FALLBACK, COMPAT_FILE, problems);
        GearDefinition gear = decode(GearDefinition.CODEC, GearDefinition.FIELDS.merge(builtinLayer(GEAR_FILE, problems), problems), GearDefinition.FALLBACK, GEAR_FILE, problems);
        for (DataProblem problem : problems) {
            BetterWeaponry.LOGGER.error("[Better Weaponry] Problem in the mod's own default data: {}", problem.toLogString());
        }
        return new WeaponData.Snapshot(weapons, compat, gear);
    }

    /** Contenu JSON brut d'un fichier par défaut du jar, ex : {@code builtinJson("weapons/katana.json")}. */
    public static Optional<JsonObject> builtinJson(String relativePath) {
        return ModFiles.read(builtinPathParts(relativePath)).flatMap(text -> {
            try {
                JsonElement element = JsonUtil.parseLenient(text);
                return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
            } catch (JsonParseException e) {
                return Optional.empty();
            }
        });
    }

    public static String weaponPath(WeaponType type) {
        return WEAPONS_DIRECTORY + "/" + type.id() + ".json";
    }

    private static List<FieldSet.Layer> collectLayers(ResourceManager resourceManager, String relativePath, @Nullable Path configFile, @Nullable WeaponType balancedType, List<DataProblem> problems) {
        // getResourceStack renvoie les fichiers du moins prioritaire au plus prioritaire.
        List<Resource> stack = resourceManager.getResourceStack(resourceId(relativePath));
        List<FieldSet.Layer> layers = new ArrayList<>();
        for (Resource resource : stack) {
            if (!isWorldDatapack(resource)) readResource(resource, relativePath, problems).ifPresent(layers::add);
        }
        String profile = BwCompat.balanceProfile();
        if (balancedType != null && profile != null) {
            String file = BALANCE_DIRECTORY + "/" + profile + "/" + balancedType.id() + ".json";
            resourceManager.getResourceStack(resourceId(file)).forEach(resource -> readResource(resource, file, problems).ifPresent(layers::add));
        }
        if (configFile != null) readConfigFile(configFile, problems).ifPresent(layers::add);
        for (Resource resource : stack) {
            if (isWorldDatapack(resource)) readResource(resource, relativePath, problems).ifPresent(layers::add);
        }
        return layers;
    }

    private static ResourceLocation resourceId(String relativePath) {
        return ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, DATA_DIRECTORY + "/" + relativePath);
    }

    private static List<FieldSet.Layer> builtinLayer(String relativePath, List<DataProblem> problems) {
        String source = "mod default > " + relativePath;
        return ModFiles.read(builtinPathParts(relativePath))
                .flatMap(text -> parse(source, text, problems))
                .map(List::of)
                .orElse(List.of());
    }

    private static String[] builtinPathParts(String relativePath) {
        List<String> parts = new ArrayList<>(List.of("data", BetterWeaponry.MODID, DATA_DIRECTORY));
        parts.addAll(List.of(relativePath.split("/")));
        return parts.toArray(String[]::new);
    }

    private static boolean isWorldDatapack(Resource resource) {
        return resource.sourcePackId().startsWith(WORLD_DATAPACK_PREFIX);
    }

    private static Optional<FieldSet.Layer> readResource(Resource resource, String relativePath, List<DataProblem> problems) {
        String source = resource.sourcePackId() + " > " + relativePath;
        try (Reader reader = resource.openAsReader()) {
            return parse(source, readAll(reader), problems);
        } catch (IOException e) {
            problems.add(new DataProblem(DataProblem.Kind.IO, source, null, e.getMessage()));
            return Optional.empty();
        }
    }

    private static Optional<FieldSet.Layer> readConfigFile(Path file, List<DataProblem> problems) {
        if (!Files.isRegularFile(file)) return Optional.empty();
        String source = ConfigFolder.displayPath(file);
        try {
            return parse(source, Files.readString(file, StandardCharsets.UTF_8), problems);
        } catch (IOException e) {
            problems.add(new DataProblem(DataProblem.Kind.IO, source, null, e.getMessage()));
            return Optional.empty();
        }
    }

    private static Optional<FieldSet.Layer> parse(String source, String text, List<DataProblem> problems) {
        try {
            JsonElement element = JsonUtil.parseLenient(text);
            if (!element.isJsonObject()) {
                problems.add(new DataProblem(DataProblem.Kind.NOT_AN_OBJECT, source, null, null));
                return Optional.empty();
            }
            JsonObject object = element.getAsJsonObject();
            FORMER_COMBAT_FIELDS.forEach(object::remove);
            return Optional.of(new FieldSet.Layer(source, object));
        } catch (JsonParseException e) {
            String detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            problems.add(new DataProblem(DataProblem.Kind.SYNTAX, source, null, detail));
            return Optional.empty();
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) builder.append(buffer, 0, read);
        return builder.toString();
    }

    private static WeaponDefinition decodeWeapon(WeaponType type, List<FieldSet.Layer> layers, List<DataProblem> problems) {
        String file = weaponPath(type);
        JsonObject merged = WeaponDefinition.FIELDS.merge(layers, problems);
        WeaponDefinition definition = decode(WeaponDefinition.CODEC, merged, WeaponDefinition.FALLBACK, file, problems);
        reportUnknownMaterials(file, definition, problems);
        return definition;
    }

    /** Toutes les valeurs ont déjà été validées une par une : le décodage complet ne devrait jamais échouer. */
    private static <T> T decode(Codec<T> codec, JsonObject merged, T fallback, String file, List<DataProblem> problems) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, merged);
        Optional<DataResult.Error<T>> error = result.error();
        if (error.isPresent()) {
            problems.add(new DataProblem(DataProblem.Kind.INVALID, file, "*", error.get().message()));
            return fallback;
        }
        return result.getOrThrow();
    }

    /** Un matériau inconnu n'empêche rien (il est simplement inutilisé), mais c'est souvent une faute de frappe. */
    private static void reportUnknownMaterials(String file, WeaponDefinition definition, List<DataProblem> problems) {
        Set<String> known = WeaponMaterials.all().stream().map(WeaponMaterial::key).collect(Collectors.toSet());
        for (String material : definition.materialDamage().keySet()) {
            if (!known.contains(material)) {
                problems.add(new DataProblem(DataProblem.Kind.UNKNOWN_MATERIAL, file, WeaponDefinition.MATERIAL_DAMAGE.name(), material));
            }
        }
    }
}
