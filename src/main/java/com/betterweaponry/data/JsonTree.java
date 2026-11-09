package com.betterweaponry.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Outils pour l'éditeur : compléter un fichier avec toutes les valeurs, et ne garder que les différences.
 * Ils suivent les déclarations des champs ({@link FieldSet}) : objets champ par champ, listes en entier.
 */
public final class JsonTree {

    private JsonTree() {
    }

    /** Copie complète : chaque champ absent prend sa valeur par défaut, en profondeur (objets et éléments des listes). */
    public static JsonObject expand(FieldSet set, JsonObject json) {
        JsonObject result = new JsonObject();
        for (DataField<?> field : set.fields()) {
            JsonElement value = json.get(field.name());
            if (value == null) value = field.defaultJson();
            if (value == null) continue;
            result.add(field.name(), expandValue(field, value));
        }
        return result;
    }

    /** Valeur complète d'un champ (sous-champs complétés s'il s'agit d'un objet ou d'une liste d'objets). */
    public static JsonElement expandValue(DataField<?> field, JsonElement value) {
        FieldSet children = field.children();
        if (children == null) return value.deepCopy();
        if (field.kind() == DataField.Kind.OBJECT && value.isJsonObject()) return expand(children, value.getAsJsonObject());
        if (field.kind() == DataField.Kind.OBJECT_LIST && value.isJsonArray()) {
            JsonArray list = new JsonArray();
            for (JsonElement element : value.getAsJsonArray()) {
                list.add(element.isJsonObject() ? expand(children, element.getAsJsonObject()) : element.deepCopy());
            }
            return list;
        }
        return value.deepCopy();
    }

    /**
     * Les champs de {@code edited} qui diffèrent de {@code base} (les deux complets). Un objet ne garde que ses
     * sous-champs modifiés ; une liste modifiée est gardée en entier, comme lors de la fusion des couches.
     */
    public static JsonObject diff(FieldSet set, JsonObject edited, JsonObject base) {
        JsonObject result = new JsonObject();
        for (DataField<?> field : set.fields()) {
            JsonElement value = edited.get(field.name());
            if (value == null) continue;
            JsonElement reference = base.get(field.name());
            if (field.kind() == DataField.Kind.OBJECT && value.isJsonObject() && reference != null && reference.isJsonObject()) {
                JsonObject changed = diff(field.children(), value.getAsJsonObject(), reference.getAsJsonObject());
                if (!changed.isEmpty()) result.add(field.name(), changed);
            } else if (!sameValue(value, reference)) {
                result.add(field.name(), value.deepCopy());
            }
        }
        return result;
    }

    /** Copie de {@code base} où les valeurs de {@code changes} remplacent les siennes : objets champ par champ, listes en entier. */
    public static JsonObject overlay(FieldSet set, JsonObject base, JsonObject changes) {
        JsonObject result = base.deepCopy();
        for (DataField<?> field : set.fields()) {
            JsonElement change = changes.get(field.name());
            if (change == null) continue;
            JsonElement current = result.get(field.name());
            if (field.kind() == DataField.Kind.OBJECT && change.isJsonObject() && current != null && current.isJsonObject()) {
                result.add(field.name(), overlay(field.children(), current.getAsJsonObject(), change.getAsJsonObject()));
            } else {
                result.add(field.name(), change.deepCopy());
            }
        }
        return result;
    }

    /** Chemins des valeurs d'un diff, ex : ["attack_speed", "sweep.radius"]. */
    public static List<String> paths(JsonObject diff) {
        List<String> paths = new ArrayList<>();
        collectPaths(diff, "", paths);
        return paths;
    }

    private static void collectPaths(JsonObject object, String prefix, List<String> paths) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String path = prefix + entry.getKey();
            if (entry.getValue().isJsonObject() && !entry.getValue().getAsJsonObject().isEmpty()) {
                collectPaths(entry.getValue().getAsJsonObject(), path + ".", paths);
            } else {
                paths.add(path);
            }
        }
    }

    /** Égalité de valeurs JSON, les nombres étant comparés par leur valeur (2 = 2.0). */
    public static boolean sameValue(@Nullable JsonElement a, @Nullable JsonElement b) {
        if (a == null || b == null) return a == b;
        if (a.isJsonPrimitive() && b.isJsonPrimitive()) {
            JsonPrimitive left = a.getAsJsonPrimitive();
            JsonPrimitive right = b.getAsJsonPrimitive();
            if (left.isNumber() && right.isNumber()) return left.getAsDouble() == right.getAsDouble();
            return left.equals(right);
        }
        if (a.isJsonObject() && b.isJsonObject()) {
            JsonObject left = a.getAsJsonObject();
            JsonObject right = b.getAsJsonObject();
            if (!left.keySet().equals(right.keySet())) return false;
            for (String key : left.keySet()) {
                if (!sameValue(left.get(key), right.get(key))) return false;
            }
            return true;
        }
        if (a.isJsonArray() && b.isJsonArray()) {
            JsonArray left = a.getAsJsonArray();
            JsonArray right = b.getAsJsonArray();
            if (left.size() != right.size()) return false;
            for (int i = 0; i < left.size(); i++) {
                if (!sameValue(left.get(i), right.get(i))) return false;
            }
            return true;
        }
        return a.equals(b);
    }
}
