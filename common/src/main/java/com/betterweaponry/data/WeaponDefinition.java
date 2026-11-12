package com.betterweaponry.data;

import com.betterweaponry.item.WeaponMaterial;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Les stats d'un type d'arme, lues dans {@code better_weaponry/weapons/<type>.json}.
 * Les valeurs par défaut du mod sont dans {@code src/main/resources/data/better_weaponry/better_weaponry/weapons/}.
 *
 * @param formatVersion  version du format de fichier (1)
 * @param attackSpeed    attaques par seconde à pleine puissance (épée vanilla : 1.6)
 * @param attackReach    portée d'attaque en blocs (joueur vanilla : 3)
 * @param baseDamage     dégâts de la version en bois ; chaque matériau les multiplie par son facteur
 * @param materialDamage dégâts imposés pour certains matériaux (id du matériau → dégâts)
 */
public record WeaponDefinition(int formatVersion, double attackSpeed, double attackReach, double baseDamage, Map<String, Double> materialDamage) {

    public static final int FORMAT_VERSION = 1;
    /** Les dégâts calculés sont arrondis au demi-point, pour des valeurs lisibles. */
    private static final double DAMAGE_STEPS_PER_POINT = 2.0D;
    /** Le mode "équilibrage global" retire de petites parts des dégâts : elles sont arrondies au dixième pour rester visibles. */
    private static final double PENALIZED_STEPS_PER_POINT = 10.0D;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    // Au-delà de 20 attaques/s, la recharge passerait sous un tick ; sous 0.1, une attaque prendrait plus de 10 s.
    public static final DataField<Double> ATTACK_SPEED = DataField.number("attack_speed", 0.1D, 20.0D, 1.6D);
    public static final DataField<Double> ATTACK_REACH = DataField.number("attack_reach", 0.5D, 64.0D, 3.0D);
    public static final DataField<Double> BASE_DAMAGE = DataField.number("base_damage", 0.0D, 1024.0D, 4.0D);
    public static final DataField<Map<String, Double>> MATERIAL_DAMAGE = DataField.numberMap("material_damage", 0.0D, 1024.0D);

    public static final FieldSet FIELDS = new FieldSet("weapon",
            List.of(FORMAT, ATTACK_SPEED, ATTACK_REACH, BASE_DAMAGE, MATERIAL_DAMAGE),
            Map.of("attack_damage", "base_damage", "damage", "base_damage", "speed", "attack_speed", "reach", "attack_reach"));

    public static final Codec<WeaponDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(WeaponDefinition::formatVersion),
            ATTACK_SPEED.forGetter(WeaponDefinition::attackSpeed),
            ATTACK_REACH.forGetter(WeaponDefinition::attackReach),
            BASE_DAMAGE.forGetter(WeaponDefinition::baseDamage),
            MATERIAL_DAMAGE.forGetter(WeaponDefinition::materialDamage)
    ).apply(instance, WeaponDefinition::new));

    /** Utilisé si une arme n'a aucune donnée (fichier du mod introuvable) : une épée vanilla en bois. */
    public static final WeaponDefinition FALLBACK = new WeaponDefinition(FORMAT_VERSION, ATTACK_SPEED.defaultValue(), ATTACK_REACH.defaultValue(), BASE_DAMAGE.defaultValue(), Map.of());

    public WeaponDefinition {
        materialDamage = Map.copyOf(materialDamage);
    }

    /** Dégâts par seconde réels de l'épée en fer vanilla (6 dégâts toutes les 12 ticks), la référence des comparaisons. */
    public static final double VANILLA_IRON_SWORD_DPS = 10.0D;
    private static final double TICKS_PER_SECOND = 20.0D;
    /** Une cible ignore les coups qui se suivent en moins de 10 ticks. */
    private static final int MIN_TICKS_BETWEEN_HITS = 10;

    /** Dégâts par seconde d'une seule arme de ce matériau, chargée à fond (sans armure, critiques ni combos). */
    public double dps(WeaponMaterial material, double abilityPenalty) {
        return damageFor(material, abilityPenalty) * TICKS_PER_SECOND / attackTicks(attackSpeed, MIN_TICKS_BETWEEN_HITS);
    }

    /**
     * Temps réel entre deux coups : le jeu compte en ticks entiers, et un coup est à pleine force dès que le compteur
     * + 0.5 atteint la recharge. Frapper plus vite que la protection de la cible ne sert à rien.
     */
    private static int attackTicks(double attacksPerSecond, int minTicks) {
        return Math.max(Math.max(1, minTicks), (int) Math.ceil(TICKS_PER_SECOND / attacksPerSecond - 0.5D));
    }

    /**
     * Dégâts affichés pour ce matériau : la valeur imposée dans {@code material_damage} s'il y en a une, sinon
     * {@code base_damage × facteur du matériau}, arrondi au demi-point, moins la pénalité d'équilibrage global.
     *
     * @param abilityPenalty part des dégâts retirée, 0 si l'équilibrage global est coupé
     */
    public double damageFor(WeaponMaterial material, double abilityPenalty) {
        Double forced = materialDamage.get(material.key());
        if (forced != null) return forced;
        double damage = Math.round(baseDamage * material.damageMultiplier() * DAMAGE_STEPS_PER_POINT) / DAMAGE_STEPS_PER_POINT;
        return abilityPenalty <= 0.0D ? damage : Math.round(damage * (1.0D - abilityPenalty) * PENALIZED_STEPS_PER_POINT) / PENALIZED_STEPS_PER_POINT;
    }
}
