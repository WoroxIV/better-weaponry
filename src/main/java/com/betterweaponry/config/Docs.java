package com.betterweaponry.config;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.DataField;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.ModFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Textes d'aide du mod dans une langue donnée, lus directement dans les fichiers de langue du jar.
 * Sert à écrire le README et les schémas JSON du dossier de config dans la langue du joueur
 * (en anglais sur un serveur dédié). Une clé absente prend le texte anglais.
 */
public final class Docs {
    private static final String FALLBACK_LANGUAGE = "en_us";
    private static final String FIELD_PREFIX = BetterWeaponry.MODID + ".field.";

    private final Map<String, String> texts;

    private Docs(Map<String, String> texts) {
        this.texts = texts;
    }

    public static Docs load(String languageCode) {
        Map<String, String> texts = new HashMap<>(readLanguage(FALLBACK_LANGUAGE));
        if (!FALLBACK_LANGUAGE.equals(languageCode)) {
            texts.putAll(readLanguage(languageCode.toLowerCase(Locale.ROOT)));
        }
        return new Docs(texts);
    }

    /** Texte d'une clé de langue, ou la clé elle-même si elle n'existe dans aucune langue. */
    public String text(String key) {
        return texts.getOrDefault(key, key);
    }

    /** Texte avec des paramètres, au format des fichiers de langue ("%s", "%1$s"). */
    public String format(String key, Object... args) {
        try {
            return String.format(Locale.ROOT, text(key), args);
        } catch (java.util.IllegalFormatException e) {
            return text(key);
        }
    }

    public String fieldName(FieldSet set, DataField<?> field) {
        return text(FIELD_PREFIX + set.scope() + "." + field.name());
    }

    public String fieldHelp(FieldSet set, DataField<?> field) {
        return text(FIELD_PREFIX + set.scope() + "." + field.name() + ".help");
    }

    /** Ex : "Plage : 0,1 à 20 · Défaut : 1,6", selon le type du champ. */
    public String fieldLimits(DataField<?> field) {
        return switch (field.kind()) {
            case NUMBER, INTEGER -> format(BetterWeaponry.MODID + ".doc.range", number(field.min()), number(field.max()))
                    + " · " + format(BetterWeaponry.MODID + ".doc.default", formatValue(field.defaultValue()));
            case BOOLEAN -> format(BetterWeaponry.MODID + ".doc.default", formatValue(field.defaultValue()));
            case NUMBER_MAP -> format(BetterWeaponry.MODID + ".doc.range", number(field.min()), number(field.max()));
        };
    }

    /** Format des nombres dans les textes : "1.6", "20", sans zéros inutiles. (DecimalFormat n'est pas partageable entre threads.) */
    public static String number(double value) {
        return new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }

    private static String formatValue(Object value) {
        return value instanceof Number number ? number(number.doubleValue()) : String.valueOf(value);
    }

    private static Map<String, String> readLanguage(String code) {
        Map<String, String> result = new HashMap<>();
        ModFiles.read("assets", BetterWeaponry.MODID, "lang", code + ".json").ifPresent(text -> {
            try {
                JsonElement element = JsonUtil.parseLenient(text);
                if (!element.isJsonObject()) return;
                for (var entry : element.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) result.put(entry.getKey(), entry.getValue().getAsString());
                }
            } catch (JsonParseException e) {
                BetterWeaponry.LOGGER.warn("Could not read the {} language file for the config documentation", code, e);
            }
        });
        return result;
    }
}
