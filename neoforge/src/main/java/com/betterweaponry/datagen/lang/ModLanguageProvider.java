package com.betterweaponry.datagen.lang;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.GearType;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Génère les fichiers de langue ({@code assets/better_weaponry/lang/<langue>.json}) à partir des sources
 * {@code src/main/lang/<langue>.json} (voir {@link LanguageSource}). Lancé par {@code gradlew runData}.
 * <p>
 * Sert aussi de test : la génération échoue, avec la liste des problèmes, si une langue oublie un texte de
 * l'anglais, contient une clé inconnue, ou n'a pas de nom pour un type d'arme ou un matériau.
 */
public final class ModLanguageProvider implements DataProvider {
    /** Langue de référence : toutes les autres doivent avoir exactement les mêmes textes. */
    private static final String REFERENCE_LOCALE = "en_us";
    /** Dossier des sources, passé par build.gradle (sinon déduit du dossier de sortie). */
    private static final String SOURCES_PROPERTY = "betterweaponry.langSources";

    private final PackOutput output;
    private final Path sourceDirectory;

    public ModLanguageProvider(PackOutput output) {
        this.output = output;
        String property = System.getProperty(SOURCES_PROPERTY);
        // Sortie : <projet>/src/generated/resources → sources : <projet>/src/main/lang
        this.sourceDirectory = property != null
                ? Path.of(property)
                : output.getOutputFolder().getParent().getParent().resolve("main").resolve("lang");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Map<String, LanguageSource> sources = readSources();
        LanguageSource reference = sources.get(REFERENCE_LOCALE);
        if (reference == null) {
            throw new IllegalStateException("Missing reference translation " + REFERENCE_LOCALE + ".json in " + sourceDirectory);
        }

        // On construit et vérifie toutes les langues avant d'écrire quoi que ce soit :
        // une erreur ne laisse jamais de fichier à moitié faux.
        List<String> problems = new ArrayList<>();
        Map<String, JsonObject> files = new TreeMap<>();
        for (LanguageSource source : sources.values()) {
            checkStrings(source, reference, problems);
            boolean namesComplete = checkNames(source, problems);

            JsonObject json = new JsonObject();
            source.strings().forEach(json::addProperty);
            if (namesComplete) addWeaponNames(source, json, problems);
            files.put(source.locale(), json);
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("Translations are incomplete (" + problems.size() + " problem(s)):\n - "
                    + String.join("\n - ", problems));
        }

        List<CompletableFuture<?>> writes = new ArrayList<>();
        files.forEach((locale, json) -> writes.add(DataProvider.saveStable(cache, json,
                output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                        .resolve(BetterWeaponry.MODID).resolve("lang").resolve(locale + ".json"))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Better Weaponry translations";
    }

    /** Toutes les sources du dossier, triées par langue. */
    private Map<String, LanguageSource> readSources() {
        Map<String, LanguageSource> sources = new TreeMap<>();
        try (Stream<Path> files = Files.list(sourceDirectory)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                LanguageSource source = LanguageSource.read(file);
                sources.put(source.locale(), source);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the translation sources in " + sourceDirectory, e);
        }
        return sources;
    }

    /** Mêmes clés que l'anglais : ni oubli, ni clé inconnue (souvent une faute de frappe). */
    private static void checkStrings(LanguageSource source, LanguageSource reference, List<String> problems) {
        for (String key : reference.strings().keySet()) {
            if (!source.strings().containsKey(key)) problems.add(source.locale() + ": missing text \"" + key + "\"");
        }
        for (String key : source.strings().keySet()) {
            if (!reference.strings().containsKey(key)) {
                problems.add(source.locale() + ": text \"" + key + "\" does not exist in " + REFERENCE_LOCALE);
            }
        }
    }

    /** Un nom pour chaque type et chaque matériau, et aucun id inconnu. */
    private static boolean checkNames(LanguageSource source, List<String> problems) {
        int before = problems.size();
        Set<String> typeIds = Stream.concat(Arrays.stream(WeaponType.values()).map(WeaponType::id), Arrays.stream(GearType.values()).map(GearType::id))
                .collect(Collectors.toSet());
        Set<String> materialIds = WeaponMaterials.all().stream().map(WeaponMaterial::id).collect(Collectors.toSet());

        typeIds.stream().filter(id -> !source.types().containsKey(id)).sorted()
                .forEach(id -> problems.add(source.locale() + ": missing weapon type name \"" + id + "\""));
        source.types().keySet().stream().filter(id -> !typeIds.contains(id))
                .forEach(id -> problems.add(source.locale() + ": unknown weapon type \"" + id + "\""));
        materialIds.stream().filter(id -> !source.materials().containsKey(id)).sorted()
                .forEach(id -> problems.add(source.locale() + ": missing material name \"" + id + "\""));
        source.materials().keySet().stream().filter(id -> !materialIds.contains(id))
                .forEach(id -> problems.add(source.locale() + ": unknown material \"" + id + "\""));
        return problems.size() == before;
    }

    private static void addWeaponNames(LanguageSource source, JsonObject json, List<String> problems) {
        for (WeaponType type : WeaponType.values()) {
            for (WeaponMaterial material : WeaponMaterials.all()) {
                try {
                    json.addProperty("item." + BetterWeaponry.MODID + "." + material.itemName(type), source.itemName(type.id(), material.id()));
                } catch (IllegalArgumentException e) {
                    problems.add(source.locale() + ": " + e.getMessage());
                }
            }
        }
        for (GearType type : GearType.values()) {
            for (WeaponMaterial material : WeaponMaterials.all()) {
                try {
                    json.addProperty("item." + BetterWeaponry.MODID + "." + material.itemName(type), source.itemName(type.id(), material.id()));
                } catch (IllegalArgumentException e) {
                    problems.add(source.locale() + ": " + e.getMessage());
                }
            }
        }
    }
}
