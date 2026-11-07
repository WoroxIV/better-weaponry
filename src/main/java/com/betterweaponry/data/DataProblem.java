package com.betterweaponry.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Un problème trouvé en lisant un fichier d'arme ou de règles : la valeur fautive est ignorée,
 * le jeu continue avec la valeur précédente. Affiché en chat aux opérateurs et écrit dans le log.
 *
 * @param source fichier concerné, ex : "config/better_weaponry/weapons/katana.json"
 * @param field  champ concerné, ou null pour un problème qui touche tout le fichier
 * @param detail précision (message d'erreur, nom suggéré...), ou null
 */
public record DataProblem(Kind kind, String source, @Nullable String field, @Nullable String detail) {

    public enum Kind {
        /** Le fichier n'est pas du JSON valide : il est ignoré. */
        SYNTAX,
        /** Le fichier ne contient pas un objet { ... } : il est ignoré. */
        NOT_AN_OBJECT,
        /** La valeur d'un champ est invalide (type, plage...) : elle est ignorée. */
        INVALID,
        /** Champ inconnu (faute de frappe ?) : il est ignoré. {@code detail} = nom suggéré, ou null. */
        UNKNOWN_FIELD,
        /** Matériau inconnu dans material_damage : il est ignoré. */
        UNKNOWN_MATERIAL,
        /** Le fichier n'a pas pu être lu. */
        IO
    }

    private static final String KEY_PREFIX = "better_weaponry.data.problem.";

    /** Message traduit pour le chat. */
    public Component toComponent() {
        Component message = switch (kind) {
            case SYNTAX -> Component.translatable(KEY_PREFIX + "syntax", source, detail);
            case NOT_AN_OBJECT -> Component.translatable(KEY_PREFIX + "not_an_object", source);
            case INVALID -> Component.translatable(KEY_PREFIX + "invalid", source, field, detail);
            case UNKNOWN_FIELD -> detail != null
                    ? Component.translatable(KEY_PREFIX + "unknown_field.suggestion", source, field, detail)
                    : Component.translatable(KEY_PREFIX + "unknown_field", source, field);
            case UNKNOWN_MATERIAL -> Component.translatable(KEY_PREFIX + "unknown_material", source, detail);
            case IO -> Component.translatable(KEY_PREFIX + "io", source, detail);
        };
        return message.copy().withStyle(ChatFormatting.YELLOW);
    }

    /** Message en anglais pour le log (le serveur dédié n'a pas forcément les traductions). */
    public String toLogString() {
        return switch (kind) {
            case SYNTAX -> source + ": JSON syntax error, file ignored (" + detail + ")";
            case NOT_AN_OBJECT -> source + ": the file must contain a JSON object { ... }, file ignored";
            case INVALID -> source + " > " + field + ": " + detail + " (value ignored)";
            case UNKNOWN_FIELD -> source + " > unknown field \"" + field + "\" (ignored)"
                    + (detail != null ? ". Did you mean \"" + detail + "\"?" : "");
            case UNKNOWN_MATERIAL -> source + " > material_damage: unknown material \"" + detail + "\" (ignored)";
            case IO -> source + ": the file could not be read (" + detail + ")";
        };
    }
}
