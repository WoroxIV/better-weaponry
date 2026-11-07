package com.betterweaponry.config;

import com.betterweaponry.data.DataField;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.google.gson.JsonObject;

/**
 * Écrit les schémas JSON (format "JSON Schema") du dossier de config.
 * Un éditeur comme VS Code s'en sert pour l'autocomplétion, l'aide au survol de chaque champ
 * et pour souligner les erreurs (valeur hors plage, champ mal orthographié).
 */
final class SchemaWriter {
    private static final String DRAFT = "http://json-schema.org/draft-07/schema#";

    private SchemaWriter() {
    }

    static JsonObject schema(FieldSet set, String title, Docs docs) {
        JsonObject properties = new JsonObject();

        JsonObject schemaLink = new JsonObject();
        schemaLink.addProperty("type", "string");
        properties.add(FieldSet.SCHEMA_KEY, schemaLink);

        for (DataField<?> field : set.fields()) {
            properties.add(field.name(), property(set, field, docs));
        }

        // Les clés qui commencent par "_" sont libres : elles servent de commentaires.
        JsonObject commentKeys = new JsonObject();
        commentKeys.add("^_", new JsonObject());

        JsonObject schema = new JsonObject();
        schema.addProperty("$schema", DRAFT);
        schema.addProperty("title", title);
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("patternProperties", commentKeys);
        // Un champ inconnu (faute de frappe) est souligné dans l'éditeur.
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private static JsonObject property(FieldSet set, DataField<?> field, Docs docs) {
        JsonObject property = new JsonObject();
        property.addProperty("title", docs.fieldName(set, field));
        property.addProperty("description", docs.fieldHelp(set, field) + "\n" + docs.fieldLimits(field));

        switch (field.kind()) {
            case NUMBER -> {
                property.addProperty("type", "number");
                property.addProperty("minimum", field.min());
                property.addProperty("maximum", field.max());
                property.addProperty("default", (Double) field.defaultValue());
            }
            case INTEGER -> {
                property.addProperty("type", "integer");
                property.addProperty("minimum", (int) field.min());
                property.addProperty("maximum", (int) field.max());
                property.addProperty("default", (Integer) field.defaultValue());
            }
            case BOOLEAN -> {
                property.addProperty("type", "boolean");
                property.addProperty("default", (Boolean) field.defaultValue());
            }
            case NUMBER_MAP -> {
                property.addProperty("type", "object");
                JsonObject value = numberSchema(field);
                // Les matériaux connus sont proposés par l'autocomplétion ; les autres restent acceptés.
                JsonObject materials = new JsonObject();
                for (WeaponMaterial material : WeaponMaterials.all()) {
                    materials.add(material.id(), numberSchema(field));
                }
                property.add("properties", materials);
                property.add("additionalProperties", value);
            }
        }
        return property;
    }

    private static JsonObject numberSchema(DataField<?> field) {
        JsonObject value = new JsonObject();
        value.addProperty("type", "number");
        value.addProperty("minimum", field.min());
        value.addProperty("maximum", field.max());
        return value;
    }
}
