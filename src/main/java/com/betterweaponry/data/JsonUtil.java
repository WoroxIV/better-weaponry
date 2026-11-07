package com.betterweaponry.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/** Petits outils JSON partagés par le chargement, le dossier de config et la migration. */
public final class JsonUtil {
    /** Écriture lisible : indentée, sans échapper les caractères comme é ou '. */
    public static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final char BYTE_ORDER_MARK = '﻿';

    private JsonUtil() {
    }

    /**
     * Lit du JSON en mode tolérant : les commentaires {@code //} et {@code /* *\/} sont acceptés.
     * La marque d'ordre des octets ajoutée par certains éditeurs (Bloc-notes) est retirée.
     * Un texte vide donne un objet vide.
     *
     * @throws com.google.gson.JsonParseException si le texte n'est pas du JSON
     */
    public static JsonElement parseLenient(String text) {
        String content = !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
        if (content.isBlank()) return new JsonObject();
        // JsonParser.parseString active le mode tolérant de Gson (commentaires, virgules en trop...).
        JsonElement element = JsonParser.parseString(content);
        return element.isJsonNull() ? new JsonObject() : element;
    }

    /**
     * Fusionne {@code override} par-dessus {@code base} : les objets sont fusionnés clé par clé (récursivement),
     * tout le reste (nombres, textes, listes) est remplacé. Ne modifie aucun des deux arguments.
     */
    public static JsonElement deepMerge(@Nullable JsonElement base, JsonElement override) {
        if (base != null && base.isJsonObject() && override.isJsonObject()) {
            JsonObject merged = base.getAsJsonObject().deepCopy();
            for (var entry : override.getAsJsonObject().entrySet()) {
                merged.add(entry.getKey(), deepMerge(merged.get(entry.getKey()), entry.getValue()));
            }
            return merged;
        }
        return override.deepCopy();
    }

    /** Nom le plus proche de {@code key} parmi {@code candidates}, s'il est assez proche pour être une faute de frappe. */
    public static Optional<String> closest(String key, Collection<String> candidates) {
        int maxDistance = Math.max(2, key.length() / 3);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            int distance = levenshtein(key, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return bestDistance <= maxDistance ? Optional.ofNullable(best) : Optional.empty();
    }

    /** Nombre minimal de lettres à ajouter, retirer ou remplacer pour passer de {@code a} à {@code b}. */
    static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
