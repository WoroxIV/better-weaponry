package com.betterweaponry.client.editor;

import com.betterweaponry.data.DataField;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.editor.EditorDocument;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Données de l'éditeur : pour chaque fichier (règles, armes), les valeurs en cours de modification, les dernières
 * valeurs enregistrées et les valeurs par défaut du mod, toutes complètes ({@link JsonTree#expand}).
 * Les saisies invalides (texte qui n'est pas un nombre, valeur hors plage) ne sont pas écrites dans les valeurs :
 * elles sont seulement notées, et bloquent l'enregistrement.
 */
final class EditorModel {
    private final Map<String, EditorDocument> documents = new LinkedHashMap<>();
    private final Map<String, JsonObject> working = new LinkedHashMap<>();
    private final Map<String, JsonObject> saved = new LinkedHashMap<>();
    private final Map<String, JsonObject> defaults = new LinkedHashMap<>();
    /** Saisies invalides : "id du fichier|chemin" → texte tapé, pour le retrouver quand la ligne est reconstruite. */
    private final Map<String, String> invalid = new HashMap<>();
    /** Augmente à chaque modification (pour recalculer ce qui en dépend, comme le DPS affiché). */
    private int version;

    /** @param current valeurs en vigueur de chaque fichier */
    EditorModel(Function<EditorDocument, JsonObject> current) {
        for (EditorDocument document : EditorDocument.all()) {
            documents.put(document.id(), document);
            JsonObject values = current.apply(document);
            working.put(document.id(), values.deepCopy());
            saved.put(document.id(), values.deepCopy());
            defaults.put(document.id(), document.defaults());
        }
    }

    List<EditorDocument> documents() {
        return List.copyOf(documents.values());
    }

    EditorDocument document(String id) {
        return documents.get(id);
    }

    JsonObject working(String id) {
        return working.get(id);
    }

    JsonObject defaults(String id) {
        return defaults.get(id);
    }

    int version() {
        return version;
    }

    @Nullable
    JsonElement get(String id, FieldPath path) {
        return path.get(working.get(id));
    }

    /** Valeur par défaut du mod à ce chemin, ou celle du champ (ex : élément de liste ajouté par l'utilisateur). */
    @Nullable
    JsonElement defaultAt(String id, FieldPath path, DataField<?> field) {
        JsonElement value = path.get(defaults.get(id));
        if (value != null) return value;
        JsonElement fieldDefault = field.defaultJson();
        return fieldDefault == null ? null : JsonTree.expandValue(field, fieldDefault);
    }

    void set(String id, FieldPath path, JsonElement value) {
        path.set(working.get(id), value);
        invalid.remove(id + "|" + path.key());
        version++;
    }

    /** Remplace toutes les valeurs d'un fichier (coller, réinitialiser, réponse du serveur). */
    void replace(String id, JsonObject values) {
        working.put(id, values.deepCopy());
        invalid.keySet().removeIf(key -> key.startsWith(id + "|"));
        version++;
    }

    void resetToDefaults(String id) {
        replace(id, defaults.get(id));
    }

    void markSaved(String id, JsonObject values) {
        saved.put(id, values.deepCopy());
        version++;
    }

    void markSaved(String id) {
        markSaved(id, working.get(id));
    }

    boolean isDirty(String id) {
        return !JsonTree.sameValue(working.get(id), saved.get(id));
    }

    List<String> dirtyIds() {
        List<String> ids = new ArrayList<>();
        for (String id : documents.keySet()) {
            if (isDirty(id)) ids.add(id);
        }
        return ids;
    }

    void setInvalid(String id, FieldPath path, String typedText) {
        invalid.put(id + "|" + path.key(), typedText);
    }

    boolean isInvalid(String id, FieldPath path) {
        return invalid.containsKey(id + "|" + path.key());
    }

    /** Texte invalide tapé à cet endroit, ou null s'il n'y en a pas. */
    @Nullable
    String invalidText(String id, FieldPath path) {
        return invalid.get(id + "|" + path.key());
    }

    boolean hasInvalid(String id) {
        return invalid.keySet().stream().anyMatch(key -> key.startsWith(id + "|"));
    }

    boolean anyInvalid() {
        return !invalid.isEmpty();
    }

    /** Les saisies invalides d'une liste ne suivent pas ses éléments quand on les déplace : on les oublie. */
    void clearInvalidUnder(String id, FieldPath path) {
        String prefix = id + "|" + path.key();
        invalid.keySet().removeIf(key -> key.startsWith(prefix));
    }
}
