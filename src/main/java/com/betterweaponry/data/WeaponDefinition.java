package com.betterweaponry.data;

import com.betterweaponry.item.WeaponMaterial;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Les stats d'un type d'arme, lues dans {@code better_weaponry/weapons/<type>.json}.
 * Les valeurs par défaut du mod sont dans {@code src/main/resources/data/better_weaponry/better_weaponry/weapons/}.
 * <p>
 * Ajouter un champ : le déclarer ci-dessous avec {@link DataField}, l'ajouter au record, au {@link #CODEC} et à
 * {@link #FIELDS}, puis écrire son nom et son aide dans les fichiers de langue
 * ({@code better_weaponry.field.weapon.<champ>} et {@code .help}). La validation, le schéma JSON et le README suivent.
 *
 * @param formatVersion  version du format de fichier (1)
 * @param twoHanded      arme à deux mains : main secondaire bloquée et cachée
 * @param attackSpeed    attaques par seconde à pleine puissance (épée vanilla : 1.6)
 * @param attackReach    portée d'attaque en blocs (joueur vanilla : 3)
 * @param baseDamage     dégâts de la version en bois ; chaque matériau les multiplie par son facteur
 * @param materialDamage dégâts imposés pour certains matériaux (id du matériau → dégâts)
 */
public record WeaponDefinition(int formatVersion,
                               boolean twoHanded,
                               double attackSpeed,
                               double attackReach,
                               double baseDamage,
                               Map<String, Double> materialDamage) {

    public static final int FORMAT_VERSION = 1;
    /** Les dégâts calculés sont arrondis au demi-point pour des tooltips lisibles. */
    private static final double DAMAGE_ROUNDING = 0.5D;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    public static final DataField<Boolean> TWO_HANDED = DataField.bool("two_handed", false);
    // Au-delà de 20 attaques/s, la recharge passerait sous un tick ; sous 0.1, une attaque prendrait plus de 10 s.
    public static final DataField<Double> ATTACK_SPEED = DataField.number("attack_speed", 0.1D, 20.0D, 1.6D);
    public static final DataField<Double> ATTACK_REACH = DataField.number("attack_reach", 0.5D, 64.0D, 3.0D);
    public static final DataField<Double> BASE_DAMAGE = DataField.number("base_damage", 0.0D, 1024.0D, 4.0D);
    public static final DataField<Map<String, Double>> MATERIAL_DAMAGE = DataField.numberMap("material_damage", 0.0D, 1024.0D);

    public static final FieldSet FIELDS = new FieldSet("weapon",
            List.of(FORMAT, TWO_HANDED, ATTACK_SPEED, ATTACK_REACH, BASE_DAMAGE, MATERIAL_DAMAGE),
            // Noms de l'ancienne config TOML et variantes courantes
            Map.of("attack_damage", "base_damage",
                    "damage", "base_damage",
                    "speed", "attack_speed",
                    "reach", "attack_reach",
                    "two_hand", "two_handed"));

    public static final Codec<WeaponDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(WeaponDefinition::formatVersion),
            TWO_HANDED.forGetter(WeaponDefinition::twoHanded),
            ATTACK_SPEED.forGetter(WeaponDefinition::attackSpeed),
            ATTACK_REACH.forGetter(WeaponDefinition::attackReach),
            BASE_DAMAGE.forGetter(WeaponDefinition::baseDamage),
            MATERIAL_DAMAGE.forGetter(WeaponDefinition::materialDamage)
    ).apply(instance, WeaponDefinition::new));

    /** Utilisé si une arme n'a aucune donnée (fichier du mod introuvable) : stats d'une épée vanilla en bois. */
    public static final WeaponDefinition FALLBACK = new WeaponDefinition(FORMAT_VERSION,
            TWO_HANDED.defaultValue(), ATTACK_SPEED.defaultValue(), ATTACK_REACH.defaultValue(),
            BASE_DAMAGE.defaultValue(), Map.of());

    public WeaponDefinition {
        materialDamage = Map.copyOf(materialDamage);
    }

    /**
     * Dégâts affichés pour ce matériau : la valeur imposée dans {@code material_damage} s'il y en a une,
     * sinon {@code base_damage × facteur du matériau}, arrondi au demi-point.
     */
    public double damageFor(WeaponMaterial material) {
        Double forced = materialDamage.get(material.id());
        if (forced != null) return forced;
        double raw = baseDamage * material.damageMultiplier();
        return Math.round(raw / DAMAGE_ROUNDING) * DAMAGE_ROUNDING;
    }
}
