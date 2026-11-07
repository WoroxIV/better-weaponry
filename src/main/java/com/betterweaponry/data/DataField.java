package com.betterweaponry.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Un champ d'un fichier JSON (arme ou règles) : son nom, son type, sa plage de valeurs et sa valeur par défaut.
 * <p>
 * Déclarer un champ avec cette classe suffit pour qu'il soit lu, validé (message clair si la valeur est
 * hors plage), décrit dans le schéma JSON et listé dans le README du dossier de config.
 * Sa description vient des fichiers de langue : {@code better_weaponry.field.<portée>.<nom>} et {@code .help}.
 * <p>
 * Un champ peut contenir d'autres champs : un objet ({@link #object}, ex : "sweep") ou une liste d'objets
 * ({@link #objectList}, ex : "on_hit_effects"). Leurs sous-champs sont décrits par un {@link FieldSet}.
 *
 * @param <T> type Java de la valeur
 */
public final class DataField<T> {
    /** Nature du champ, utilisée pour le schéma JSON et le README. */
    public enum Kind { NUMBER, INTEGER, BOOLEAN, CHOICE, NUMBER_MAP, ID, OBJECT, OBJECT_LIST }

    private final String name;
    private final Kind kind;
    private final Codec<T> codec;
    @Nullable private final T defaultValue;
    private final double min;
    private final double max;
    /** Sous-champs d'un objet ou des éléments d'une liste, sinon null. */
    @Nullable private final FieldSet children;
    /** Identifiants acceptés (champ ID), pour la validation et l'autocomplétion. */
    @Nullable private final Supplier<Collection<ResourceLocation>> knownIds;
    /** Quelques valeurs typiques (champ ID) ou toutes les valeurs acceptées (champ CHOICE), citées dans la documentation. */
    private final List<String> examples;

    private DataField(String name, Kind kind, Codec<T> codec, @Nullable T defaultValue, double min, double max,
                      @Nullable FieldSet children, @Nullable Supplier<Collection<ResourceLocation>> knownIds,
                      List<String> examples) {
        this.name = name;
        this.kind = kind;
        this.codec = codec;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.children = children;
        this.knownIds = knownIds;
        this.examples = List.copyOf(examples);
    }

    /** Nombre décimal compris entre {@code min} et {@code max}. */
    public static DataField<Double> number(String name, double min, double max, double defaultValue) {
        return new DataField<>(name, Kind.NUMBER, Codec.doubleRange(min, max), defaultValue, min, max, null, null, List.of());
    }

    /** Nombre entier compris entre {@code min} et {@code max}. */
    public static DataField<Integer> integer(String name, int min, int max, int defaultValue) {
        return new DataField<>(name, Kind.INTEGER, Codec.intRange(min, max), defaultValue, min, max, null, null, List.of());
    }

    public static DataField<Boolean> bool(String name, boolean defaultValue) {
        return new DataField<>(name, Kind.BOOLEAN, Codec.BOOL, defaultValue, 0, 1, null, null, List.of());
    }

    /**
     * Un mot parmi une liste, ex : "ability": "parry". Les valeurs sont celles de l'enum ({@link StringRepresentable}).
     */
    public static <E extends Enum<E> & StringRepresentable> DataField<E> choice(String name, E[] values, E defaultValue) {
        List<String> names = Arrays.stream(values).map(StringRepresentable::getSerializedName).toList();
        Codec<E> codec = Codec.STRING.comapFlatMap(text -> Arrays.stream(values)
                        .filter(value -> value.getSerializedName().equals(text))
                        .findFirst()
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "unknown value \"" + text + "\" (accepted: " + String.join(", ", names) + ")")),
                StringRepresentable::getSerializedName);
        return new DataField<>(name, Kind.CHOICE, codec, defaultValue, 0, 0, null, null, names);
    }

    /** Objet { "clé": nombre, ... }, chaque nombre compris entre {@code min} et {@code max}. Vide par défaut. */
    public static DataField<Map<String, Double>> numberMap(String name, double min, double max) {
        Codec<Map<String, Double>> codec = Codec.unboundedMap(Codec.STRING, Codec.doubleRange(min, max));
        return new DataField<>(name, Kind.NUMBER_MAP, codec, Map.of(), min, max, null, null, List.of());
    }

    /**
     * Identifiant obligatoire, ex : "minecraft:slowness". Refusé s'il n'est pas dans {@code knownIds}.
     *
     * @param what     nom de ce qui est désigné, pour le message d'erreur (ex : "effect")
     * @param examples valeurs typiques citées dans la documentation
     */
    public static DataField<ResourceLocation> id(String name, String what, Supplier<Collection<ResourceLocation>> knownIds,
                                                 String... examples) {
        Codec<ResourceLocation> codec = ResourceLocation.CODEC.validate(id -> knownIds.get().contains(id)
                ? DataResult.success(id)
                : DataResult.error(() -> "unknown " + what + " \"" + id + "\""));
        return new DataField<>(name, Kind.ID, codec, null, 0, 0, null, knownIds, List.of(examples));
    }

    /** Objet dont les sous-champs sont décrits par {@code children}, ex : "sweep": { "radius": 2 }. */
    public static <T> DataField<T> object(String name, FieldSet children, Codec<T> codec, T defaultValue) {
        return new DataField<>(name, Kind.OBJECT, codec, defaultValue, 0, 0, children, null, List.of());
    }

    /** Liste d'objets (au plus {@code maxSize}), ex : "on_hit_effects": [ { ... }, { ... } ]. Vide par défaut. */
    public static <T> DataField<List<T>> objectList(String name, FieldSet children, Codec<T> elementCodec, int maxSize) {
        Codec<List<T>> codec = elementCodec.listOf().validate(list -> list.size() <= maxSize
                ? DataResult.success(list)
                : DataResult.error(() -> "at most " + maxSize + " entries (" + list.size() + " given)"));
        return new DataField<>(name, Kind.OBJECT_LIST, codec, List.of(), 0, maxSize, children, null, List.of());
    }

    /**
     * À utiliser dans un {@link RecordCodecBuilder}. Un champ obligatoire (identifiant) doit être présent ;
     * les autres sont facultatifs et prennent leur valeur par défaut s'ils manquent.
     */
    public <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter) {
        if (isRequired()) return codec.fieldOf(name).forGetter(getter);
        return codec.optionalFieldOf(name, defaultValue).forGetter(getter);
    }

    /** Vérifie une valeur JSON. Renvoie le message d'erreur si elle est invalide. */
    public Optional<String> validate(JsonElement value) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, value);
        return result.error().map(DataResult.Error::message);
    }

    /** Valeur par défaut au format JSON, ou null pour un champ obligatoire (sans valeur par défaut). */
    @Nullable
    public JsonElement defaultJson() {
        if (defaultValue == null) return null;
        return codec.encodeStart(JsonOps.INSTANCE, defaultValue).result().orElse(null);
    }

    /** Un champ sans valeur par défaut doit être écrit (ex : l'effet d'un effet à l'impact). */
    public boolean isRequired() {
        return defaultValue == null;
    }

    public String name() {
        return name;
    }

    public Kind kind() {
        return kind;
    }

    @Nullable
    public T defaultValue() {
        return defaultValue;
    }

    public double min() {
        return min;
    }

    /** Valeur maximale, ou nombre maximal d'éléments pour une liste. */
    public double max() {
        return max;
    }

    @Nullable
    public FieldSet children() {
        return children;
    }

    /** Valeurs typiques d'un champ ID, ou valeurs acceptées d'un champ CHOICE, pour la documentation. */
    public List<String> examples() {
        return examples;
    }

    /** Identifiants acceptés par un champ ID, triés. Vide pour les autres champs. */
    public List<ResourceLocation> knownIds() {
        return knownIds == null ? List.of() : knownIds.get().stream().sorted().toList();
    }
}
