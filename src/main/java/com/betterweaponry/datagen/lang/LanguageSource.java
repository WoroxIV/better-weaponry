package com.betterweaponry.datagen.lang;

import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Une source de traduction : {@code src/main/lang/<langue>.json}.
 * <p>
 * Les 650 noms d'armes (13 types × 50 matériaux) ne sont pas écrits un par un : ils sont construits avec
 * {@code names.pattern}, où {@code {type}} et {@code {material}} sont remplacés par les noms du type et du matériau.
 * <ul>
 *   <li>{@code {type|lower}} / {@code {material|lower}} : première lettre en minuscule (ex : mots composés allemands) ;
 *       {@code |upper} : première lettre en majuscule.</li>
 *   <li>Un type peut avoir un genre : {@code {"name": "Sense", "gender": "f"}}.</li>
 *   <li>Un matériau peut avoir une forme par genre ({@code {"m": "...", "f": "...", "n": "..."}}, la clé étant le genre
 *       du type), une forme commune {@code "name"}, et son propre {@code "pattern"}.</li>
 *   <li>{@code names.overrides} impose le nom complet d'un objet précis (ex : "golden_sai").</li>
 * </ul>
 * Les autres textes sont dans {@code strings}, avec exactement les mêmes clés qu'en anglais.
 */
public record LanguageSource(String locale,
                             String pattern,
                             Map<String, TypeName> types,
                             Map<String, JsonElement> materials,
                             Map<String, String> overrides,
                             Map<String, String> strings) {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(type|material)(?:\\|(lower|upper))?}");
    private static final String NAME = "name";
    private static final String PATTERN = "pattern";

    /** Nom d'un type d'arme, avec son genre grammatical si la langue en a besoin (accord des adjectifs). */
    public record TypeName(String name, @Nullable String gender) {
    }

    /** Lit une source. Le nom du fichier donne la langue ("de_de.json" → "de_de"). */
    public static LanguageSource read(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        String locale = fileName.substring(0, fileName.length() - ".json".length());
        JsonObject root = JsonUtil.parseLenient(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject names = root.getAsJsonObject("names");
        if (names == null || !names.has(PATTERN)) {
            throw new IllegalArgumentException(fileName + ": missing \"names\" or \"names.pattern\"");
        }

        Map<String, TypeName> types = new LinkedHashMap<>();
        for (var entry : names.getAsJsonObject("types").entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive()) {
                types.put(entry.getKey(), new TypeName(value.getAsString(), null));
            } else {
                JsonObject object = value.getAsJsonObject();
                JsonElement gender = object.get("gender");
                types.put(entry.getKey(), new TypeName(object.get(NAME).getAsString(), gender == null ? null : gender.getAsString()));
            }
        }

        Map<String, JsonElement> materials = new LinkedHashMap<>();
        names.getAsJsonObject("materials").entrySet().forEach(entry -> materials.put(entry.getKey(), entry.getValue()));

        Map<String, String> overrides = new LinkedHashMap<>();
        JsonObject overridesJson = names.getAsJsonObject("overrides");
        if (overridesJson != null) overridesJson.entrySet().forEach(entry -> overrides.put(entry.getKey(), entry.getValue().getAsString()));

        Map<String, String> strings = new LinkedHashMap<>();
        root.getAsJsonObject("strings").entrySet().forEach(entry -> strings.put(entry.getKey(), entry.getValue().getAsString()));

        return new LanguageSource(locale, names.get(PATTERN).getAsString(), types, materials, overrides, strings);
    }

    /**
     * Nom traduit de l'arme {@code type} dans le matériau {@code materialId}.
     *
     * @throws IllegalArgumentException si le type ou le matériau manque, ou si le matériau n'a pas de forme pour ce genre
     */
    public String itemName(WeaponType type, String materialId) {
        String override = overrides.get(materialId + "_" + type.id());
        if (override != null) return override;

        TypeName typeName = types.get(type.id());
        JsonElement material = materials.get(materialId);
        if (typeName == null) throw new IllegalArgumentException("no name for the weapon type \"" + type.id() + "\"");
        if (material == null) throw new IllegalArgumentException("no name for the material \"" + materialId + "\"");

        String usedPattern = pattern;
        String materialForm;
        if (material.isJsonPrimitive()) {
            materialForm = material.getAsString();
        } else {
            JsonObject forms = material.getAsJsonObject();
            JsonElement form = typeName.gender() != null ? forms.get(typeName.gender()) : null;
            if (form == null) form = forms.get(NAME);
            if (form == null) {
                throw new IllegalArgumentException("the material \"" + materialId + "\" has no form for the gender \""
                        + typeName.gender() + "\" of \"" + type.id() + "\", and no \"name\"");
            }
            materialForm = form.getAsString();
            if (forms.has(PATTERN)) usedPattern = forms.get(PATTERN).getAsString();
        }
        return fill(usedPattern, typeName.name(), materialForm);
    }

    private static String fill(String pattern, String typeName, String materialName) {
        Matcher matcher = PLACEHOLDER.matcher(pattern);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = matcher.group(1).equals("type") ? typeName : materialName;
            matcher.appendReplacement(result, Matcher.quoteReplacement(changeFirstLetter(value, matcher.group(2))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String changeFirstLetter(String text, @Nullable String mode) {
        if (mode == null || text.isEmpty()) return text;
        String first = text.substring(0, 1);
        return (mode.equals("lower") ? first.toLowerCase(Locale.ROOT) : first.toUpperCase(Locale.ROOT)) + text.substring(1);
    }
}
