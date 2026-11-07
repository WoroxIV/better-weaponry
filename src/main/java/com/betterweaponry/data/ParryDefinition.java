package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Réglages de la parade (objet "parry" d'un fichier d'arme), utilisés quand "ability" vaut "parry".
 * Clic droit maintenu : le joueur avance lentement et les coups reçus de face sont réduits, comme en 1.8.
 *
 * @param damageReduction     part des dégâts arrêtée, en % (100 = aucun dégât)
 * @param durabilityPerDamage durabilité perdue par point de dégât arrêté (au moins 1 par coup paré ; 0 = jamais)
 * @param whileDualWielding   la parade marche aussi avec une arme en main secondaire (saïs)
 */
public record ParryDefinition(double damageReduction, double durabilityPerDamage, boolean whileDualWielding) {

    // 50 % : la parade à l'épée de la 1.8 divisait les dégâts par deux.
    public static final DataField<Double> DAMAGE_REDUCTION = DataField.number("damage_reduction", 0.0D, 100.0D, 50.0D);
    public static final DataField<Double> DURABILITY_PER_DAMAGE = DataField.number("durability_per_damage", 0.0D, 10.0D, 1.0D);
    public static final DataField<Boolean> WHILE_DUAL_WIELDING = DataField.bool("while_dual_wielding", false);

    public static final FieldSet FIELDS = new FieldSet("weapon.parry",
            List.of(DAMAGE_REDUCTION, DURABILITY_PER_DAMAGE, WHILE_DUAL_WIELDING),
            Map.of("reduction", "damage_reduction",
                    "damage", "damage_reduction",
                    "durability", "durability_per_damage",
                    "durability_per_hit", "durability_per_damage",
                    "dual_wield", "while_dual_wielding",
                    "parry_while_dual_wielding", "while_dual_wielding"));

    public static final Codec<ParryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DAMAGE_REDUCTION.forGetter(ParryDefinition::damageReduction),
            DURABILITY_PER_DAMAGE.forGetter(ParryDefinition::durabilityPerDamage),
            WHILE_DUAL_WIELDING.forGetter(ParryDefinition::whileDualWielding)
    ).apply(instance, ParryDefinition::new));

    public static final ParryDefinition DEFAULT = new ParryDefinition(
            DAMAGE_REDUCTION.defaultValue(), DURABILITY_PER_DAMAGE.defaultValue(), WHILE_DUAL_WIELDING.defaultValue());
}
