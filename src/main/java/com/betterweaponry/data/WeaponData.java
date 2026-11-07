package com.betterweaponry.data;

import com.betterweaponry.item.WeaponType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Données des armes actuellement en vigueur (stats de chaque type + règles globales).
 * <p>
 * Côté serveur, elles viennent du chargement des fichiers ({@link WeaponDataLoader}) ; côté client, de la
 * synchronisation envoyée par le serveur. En solo, client et serveur partagent la même instance, ce qui est
 * sans danger : les deux reçoivent les mêmes valeurs. Avant tout chargement (menu principal), ce sont les
 * valeurs par défaut du jar.
 */
public final class WeaponData {
    private static final AtomicInteger GENERATION = new AtomicInteger();

    /** Données en vigueur, null tant qu'on n'a pas eu besoin des valeurs par défaut. */
    private static volatile Versioned current;
    private static volatile Snapshot builtin;

    private WeaponData() {
    }

    /** Un jeu de données complet, immuable : on le remplace en bloc, jamais champ par champ. */
    public record Snapshot(Map<WeaponType, WeaponDefinition> weapons, RulesDefinition rules) {
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(WeaponType.CODEC, WeaponDefinition.CODEC).fieldOf("weapons").forGetter(Snapshot::weapons),
                RulesDefinition.CODEC.fieldOf("rules").forGetter(Snapshot::rules)
        ).apply(instance, Snapshot::new));

        public Snapshot {
            weapons = Map.copyOf(weapons);
        }

        public WeaponDefinition weapon(WeaponType type) {
            return weapons.getOrDefault(type, WeaponDefinition.FALLBACK);
        }
    }

    /**
     * Données + numéro de version, qui augmente à chaque remplacement.
     * Les caches (ex : modificateurs d'attributs des armes) comparent ce numéro pour savoir s'ils sont à jour.
     */
    public record Versioned(Snapshot snapshot, int generation) {
    }

    public static Snapshot current() {
        return versioned().snapshot();
    }

    public static Versioned versioned() {
        Versioned value = current;
        if (value == null) {
            synchronized (WeaponData.class) {
                if (current == null) current = new Versioned(builtin(), GENERATION.incrementAndGet());
                value = current;
            }
        }
        return value;
    }

    public static WeaponDefinition weapon(WeaponType type) {
        return current().weapon(type);
    }

    public static RulesDefinition rules() {
        return current().rules();
    }

    /** Remplace les données en vigueur (chargement serveur ou synchronisation reçue). */
    public static void install(Snapshot snapshot) {
        current = new Versioned(snapshot, GENERATION.incrementAndGet());
    }

    /** Revient aux valeurs par défaut du jar (ex : le client quitte un serveur). */
    public static void resetToBuiltin() {
        install(builtin());
    }

    /** Valeurs par défaut du jar, lues une seule fois. */
    public static Snapshot builtin() {
        Snapshot value = builtin;
        if (value == null) {
            synchronized (WeaponData.class) {
                if (builtin == null) builtin = WeaponDataLoader.loadBuiltin();
                value = builtin;
            }
        }
        return value;
    }
}
