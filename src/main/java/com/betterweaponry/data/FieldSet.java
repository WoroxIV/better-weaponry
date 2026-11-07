package com.betterweaponry.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Les champs d'un type de fichier (arme ou règles), et la fusion des couches de fichiers.
 *
 * @param scope portée utilisée dans les clés de langue : {@code better_weaponry.field.<scope>.<champ>}
 * @param hints anciens noms ou noms courants mal choisis, associés au bon champ (ex : "attack_damage" → "base_damage")
 */
public record FieldSet(String scope, List<DataField<?>> fields, Map<String, String> hints) {
    /** Clé facultative qui relie un fichier à son schéma dans VS Code : jamais signalée comme inconnue. */
    public static final String SCHEMA_KEY = "$schema";

    public FieldSet {
        fields = List.copyOf(fields);
        hints = Map.copyOf(hints);
    }

    /** Une couche de fichier, de la moins prioritaire à la plus prioritaire. */
    public record Layer(String source, JsonObject json) {
    }

    /**
     * Fusionne les couches dans l'ordre (la dernière gagne), champ par champ.
     * Une valeur invalide est signalée puis ignorée : on garde celle des couches précédentes.
     * Les objets (ex : material_damage) sont fusionnés clé par clé ; une couche peut donc ne changer qu'un matériau.
     *
     * @return l'objet JSON fusionné, qui ne contient que des valeurs valides
     */
    public JsonObject merge(List<Layer> layers, List<DataProblem> problems) {
        Map<String, DataField<?>> byName = byName();
        JsonObject result = new JsonObject();
        for (Layer layer : layers) {
            for (Map.Entry<String, JsonElement> entry : layer.json().entrySet()) {
                String key = entry.getKey();
                if (isIgnored(key)) continue;

                DataField<?> field = byName.get(key);
                if (field == null) {
                    problems.add(new DataProblem(DataProblem.Kind.UNKNOWN_FIELD, layer.source(), key, suggest(key).orElse(null)));
                    continue;
                }

                JsonElement candidate = JsonUtil.deepMerge(result.get(key), entry.getValue());
                Optional<String> error = field.validate(candidate);
                if (error.isPresent()) {
                    problems.add(new DataProblem(DataProblem.Kind.INVALID, layer.source(), key, error.get()));
                    continue;
                }
                result.add(key, candidate);
            }
        }
        return result;
    }

    public Map<String, DataField<?>> byName() {
        Map<String, DataField<?>> map = new LinkedHashMap<>();
        for (DataField<?> field : fields) map.put(field.name(), field);
        return map;
    }

    /** "$schema" relie le fichier au schéma ; les clés qui commencent par "_" servent de commentaires. */
    private static boolean isIgnored(String key) {
        return key.equals(SCHEMA_KEY) || key.startsWith("_");
    }

    private Optional<String> suggest(String key) {
        String hint = hints.get(key);
        if (hint != null) return Optional.of(hint);
        return JsonUtil.closest(key, byName().keySet());
    }
}
