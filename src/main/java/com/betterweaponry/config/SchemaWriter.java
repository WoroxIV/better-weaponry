package com.betterweaponry.config;

import com.betterweaponry.data.DataField;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * Écrit les schémas JSON (format "JSON Schema") du dossier de config.
 * Un éditeur comme VS Code s'en sert pour l'autocomplétion, l'aide au survol de chaque champ
 * et pour souligner les erreurs (valeur hors plage, champ mal orthographié), y compris dans les sous-champs.
 */
final class SchemaWriter {
    private static final String DRAFT = "http://json-schema.org/draft-07/schema#";

    private SchemaWriter() {
    }

    static JsonObject schema(FieldSet set, String title, Docs docs) {
        JsonObject schema = objectSchema(set, docs);
        // Le lien vers le schéma est accepté à la racine du fichier.
        JsonObject schemaLink = new JsonObject();
        schemaLink.addProperty("type", "string");
        schema.getAsJsonObject("properties").add(FieldSet.SCHEMA_KEY, schemaLink);

        JsonObject root = new JsonObject();
        root.addProperty("$schema", DRAFT);
        root.addProperty("title", title);
        schema.entrySet().forEach(entry -> root.add(entry.getKey(), entry.getValue()));
        return root;
    }

    /** Un objet et ses champs ; un champ inconnu (faute de frappe) est souligné dans l'éditeur. */
    private static JsonObject objectSchema(FieldSet set, Docs docs) {
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        for (DataField<?> field : set.fields()) {
            properties.add(field.name(), property(set, field, docs));
            if (field.isRequired()) required.add(field.name());
        }

        // Les clés qui commencent par "_" sont libres : elles servent de commentaires.
        JsonObject commentKeys = new JsonObject();
        commentKeys.add("^_", new JsonObject());

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        if (!required.isEmpty()) schema.add("required", required);
        schema.add("patternProperties", commentKeys);
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private static JsonObject property(FieldSet set, DataField<?> field, Docs docs) {
        JsonObject property = switch (field.kind()) {
            case NUMBER -> numberSchema(field, "number");
            case INTEGER -> numberSchema(field, "integer");
            case BOOLEAN -> typed("boolean");
            case NUMBER_MAP -> {
                JsonObject map = typed("object");
                // Les matériaux connus sont proposés par l'autocomplétion ; les autres restent acceptés.
                JsonObject materials = new JsonObject();
                for (WeaponMaterial material : WeaponMaterials.all()) materials.add(material.id(), numberSchema(field, "number"));
                map.add("properties", materials);
                map.add("additionalProperties", numberSchema(field, "number"));
                yield map;
            }
            case ID -> {
                JsonObject id = typed("string");
                JsonArray examples = new JsonArray();
                for (ResourceLocation known : field.knownIds()) examples.add(known.toString());
                id.add("examples", examples);
                yield id;
            }
            case OBJECT -> objectSchema(field.children(), docs);
            case OBJECT_LIST -> {
                JsonObject list = typed("array");
                list.addProperty("maxItems", (int) field.max());
                list.add("items", objectSchema(field.children(), docs));
                yield list;
            }
        };

        String limits = docs.fieldLimits(field);
        property.addProperty("title", docs.fieldName(set, field));
        property.addProperty("description", docs.fieldHelp(set, field) + (limits.isEmpty() ? "" : "\n" + limits));
        Object defaultValue = field.defaultValue();
        if (defaultValue instanceof Number number) property.addProperty("default", number);
        else if (defaultValue instanceof Boolean bool) property.addProperty("default", bool);
        return property;
    }

    private static JsonObject typed(String type) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", type);
        return schema;
    }

    private static JsonObject numberSchema(DataField<?> field, String type) {
        JsonObject value = typed(type);
        value.addProperty("minimum", field.min());
        value.addProperty("maximum", field.max());
        return value;
    }
}
