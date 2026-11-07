package com.betterweaponry.client.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Chemin d'une valeur dans un fichier JSON : noms de champs et numéros d'éléments de liste,
 * ex : combo → steps → 2 → radius ("combo.steps[2].radius").
 */
record FieldPath(List<Object> parts) {
    static final FieldPath ROOT = new FieldPath(List.of());

    FieldPath {
        parts = List.copyOf(parts);
    }

    FieldPath child(String name) {
        return with(name);
    }

    FieldPath child(int index) {
        return with(index);
    }

    private FieldPath with(Object part) {
        List<Object> extended = new ArrayList<>(parts);
        extended.add(part);
        return new FieldPath(extended);
    }

    @Nullable
    FieldPath parent() {
        return parts.isEmpty() ? null : new FieldPath(parts.subList(0, parts.size() - 1));
    }

    @Nullable
    Object last() {
        return parts.isEmpty() ? null : parts.getLast();
    }

    /** Valeur à ce chemin dans {@code root}, ou null si elle n'existe pas. */
    @Nullable
    JsonElement get(JsonElement root) {
        JsonElement current = root;
        for (Object part : parts) {
            if (part instanceof String name && current != null && current.isJsonObject()) {
                current = current.getAsJsonObject().get(name);
            } else if (part instanceof Integer index && current != null && current.isJsonArray()
                    && index >= 0 && index < current.getAsJsonArray().size()) {
                current = current.getAsJsonArray().get(index);
            } else {
                return null;
            }
        }
        return current;
    }

    /** Remplace la valeur à ce chemin (l'objet ou la liste qui la contient doit exister). */
    void set(JsonElement root, JsonElement value) {
        FieldPath parentPath = parent();
        if (parentPath == null) return;
        JsonElement container = parentPath.get(root);
        Object key = last();
        if (key instanceof String name && container instanceof JsonObject object) {
            object.add(name, value);
        } else if (key instanceof Integer index && container instanceof JsonArray array && index >= 0 && index < array.size()) {
            array.set(index, value);
        }
    }

    /** Texte unique du chemin, ex : "combo.steps[2].radius". */
    String key() {
        StringBuilder builder = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof Integer index) {
                builder.append('[').append(index).append(']');
            } else {
                if (!builder.isEmpty()) builder.append('.');
                builder.append(part);
            }
        }
        return builder.toString();
    }
}
