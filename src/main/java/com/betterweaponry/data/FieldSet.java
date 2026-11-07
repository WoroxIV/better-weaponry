package com.betterweaponry.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Les champs d'un type de fichier (arme ou règles) ou d'un objet imbriqué, et la fusion des couches de fichiers.
 *
 * @param scope portée utilisée dans les clés de langue : {@code better_weaponry.field.<scope>.<champ>}
 *              (ex : "weapon", ou "weapon.sweep" pour les sous-champs de "sweep")
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
     * Les objets (ex : sweep, material_damage) sont fusionnés clé par clé ; une couche peut donc ne changer
     * qu'un sous-champ. Les listes (ex : on_hit_effects) sont remplacées en entier.
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
                // Fautes de frappe dans les sous-champs (ex : "sweep": { "radus": 2 }) : signalées, sans bloquer le reste.
                reportUnknownChildren(field, entry.getValue(), key, layer.source(), problems);

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

    /** Signale les clés inconnues d'un objet imbriqué ou des éléments d'une liste, en profondeur. */
    private static void reportUnknownChildren(DataField<?> field, JsonElement value, String path, String source,
                                              List<DataProblem> problems) {
        FieldSet children = field.children();
        if (children == null) return;
        if (field.kind() == DataField.Kind.OBJECT && value.isJsonObject()) {
            children.reportUnknownKeys(value.getAsJsonObject(), path, source, problems);
        } else if (field.kind() == DataField.Kind.OBJECT_LIST && value.isJsonArray()) {
            int index = 0;
            for (JsonElement element : value.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    children.reportUnknownKeys(element.getAsJsonObject(), path + "[" + index + "]", source, problems);
                }
                index++;
            }
        }
    }

    private void reportUnknownKeys(JsonObject object, String path, String source, List<DataProblem> problems) {
        Map<String, DataField<?>> byName = byName();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (isIgnored(key)) continue;
            DataField<?> field = byName.get(key);
            if (field == null) {
                problems.add(new DataProblem(DataProblem.Kind.UNKNOWN_FIELD, source, path + "." + key, suggest(key).orElse(null)));
            } else {
                reportUnknownChildren(field, entry.getValue(), path + "." + key, source, problems);
            }
        }
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
