package com.betterweaponry.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Un champ d'un fichier JSON (arme ou règles) : son nom, son type, sa plage de valeurs et sa valeur par défaut.
 * <p>
 * Déclarer un champ avec cette classe suffit pour qu'il soit lu, validé (message clair si la valeur est
 * hors plage), décrit dans le schéma JSON et listé dans le README du dossier de config.
 * Sa description vient des fichiers de langue : {@code better_weaponry.field.<portée>.<nom>} et {@code .help}.
 *
 * @param <T> type Java de la valeur
 */
public final class DataField<T> {
    /** Nature du champ, utilisée pour le schéma JSON et le README. */
    public enum Kind { NUMBER, INTEGER, BOOLEAN, NUMBER_MAP }

    private final String name;
    private final Kind kind;
    private final Codec<T> codec;
    private final T defaultValue;
    private final double min;
    private final double max;

    private DataField(String name, Kind kind, Codec<T> codec, T defaultValue, double min, double max) {
        this.name = name;
        this.kind = kind;
        this.codec = codec;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    /** Nombre décimal compris entre {@code min} et {@code max}. */
    public static DataField<Double> number(String name, double min, double max, double defaultValue) {
        return new DataField<>(name, Kind.NUMBER, Codec.doubleRange(min, max), defaultValue, min, max);
    }

    /** Nombre entier compris entre {@code min} et {@code max}. */
    public static DataField<Integer> integer(String name, int min, int max, int defaultValue) {
        return new DataField<>(name, Kind.INTEGER, Codec.intRange(min, max), defaultValue, min, max);
    }

    public static DataField<Boolean> bool(String name, boolean defaultValue) {
        return new DataField<>(name, Kind.BOOLEAN, Codec.BOOL, defaultValue, 0, 1);
    }

    /** Objet { "clé": nombre, ... }, chaque nombre compris entre {@code min} et {@code max}. Vide par défaut. */
    public static DataField<Map<String, Double>> numberMap(String name, double min, double max) {
        Codec<Map<String, Double>> codec = Codec.unboundedMap(Codec.STRING, Codec.doubleRange(min, max));
        return new DataField<>(name, Kind.NUMBER_MAP, codec, Map.of(), min, max);
    }

    /** À utiliser dans un {@link RecordCodecBuilder} : le champ est facultatif et prend sa valeur par défaut s'il manque. */
    public <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter) {
        MapCodec<T> mapCodec = codec.optionalFieldOf(name, defaultValue);
        return mapCodec.forGetter(getter);
    }

    /** Vérifie une valeur JSON. Renvoie le message d'erreur si elle est invalide. */
    public Optional<String> validate(JsonElement value) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, value);
        return result.error().map(DataResult.Error::message);
    }

    public String name() {
        return name;
    }

    public Kind kind() {
        return kind;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }
}
