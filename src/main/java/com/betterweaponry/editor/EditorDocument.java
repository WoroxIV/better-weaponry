package com.betterweaponry.editor;

import com.betterweaponry.config.ConfigFolder;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un fichier modifiable dans l'éditeur : une arme ({@code weapons/<type>.json}) ou les règles ({@code rules.json}).
 *
 * @param id         identifiant envoyé dans les paquets : l'id du type d'arme, ou "rules"
 * @param type       type d'arme, null pour les règles
 * @param fields     champs du fichier
 * @param schemaPath lien vers le schéma, relatif au fichier dans config/better_weaponry
 */
public record EditorDocument(String id, @Nullable WeaponType type, FieldSet fields, String relativePath, String schemaPath) {
    public static final String RULES_ID = "rules";

    public static EditorDocument weapon(WeaponType type) {
        return new EditorDocument(type.id(), type, WeaponDefinition.FIELDS, WeaponDataLoader.weaponPath(type),
                "../" + ConfigFolder.WEAPON_SCHEMA);
    }

    public static EditorDocument rules() {
        return new EditorDocument(RULES_ID, null, RulesDefinition.FIELDS, WeaponDataLoader.RULES_FILE, "./" + ConfigFolder.RULES_SCHEMA);
    }

    /** Les règles puis les armes, dans l'ordre de l'onglet créatif. */
    public static List<EditorDocument> all() {
        List<EditorDocument> documents = new ArrayList<>();
        documents.add(rules());
        for (WeaponType type : WeaponType.values()) documents.add(weapon(type));
        return documents;
    }

    public static Optional<EditorDocument> byId(String id) {
        if (RULES_ID.equals(id)) return Optional.of(rules());
        return Optional.ofNullable(WeaponType.byId(id)).map(EditorDocument::weapon);
    }

    /** Fichier de l'utilisateur dans config/better_weaponry. */
    public Path configFile() {
        return type == null ? ConfigFolder.rulesFile() : ConfigFolder.weaponFile(type);
    }

    /** Valeurs complètes de ce fichier dans ces données (ex : celles en vigueur, ou celles du jar). */
    public JsonObject json(WeaponData.Snapshot snapshot) {
        JsonElement encoded = type == null
                ? RulesDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.rules()).getOrThrow()
                : WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, snapshot.weapon(type)).getOrThrow();
        return JsonTree.expand(fields, encoded.getAsJsonObject());
    }

    /** Valeurs par défaut du mod (fichiers du jar). */
    public JsonObject defaults() {
        return json(WeaponData.builtin());
    }
}
