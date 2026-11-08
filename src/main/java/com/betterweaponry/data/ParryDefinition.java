package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Réglages de la parade (objet "parry" d'un fichier d'arme), utilisés quand "ability" vaut "parry".
 * Clic droit maintenu : le joueur avance lentement et les coups reçus de face sont réduits, comme en 1.8.
 *
 * @param damageReduction        part des dégâts arrêtée, en % (100 = aucun dégât)
 * @param durabilityPerDamage    durabilité perdue par point de dégât arrêté (au moins 1 par coup paré ; 0 = jamais)
 * @param whileDualWielding      la parade marche aussi avec une arme ambidextre dans l'autre main (saïs)
 * @param perfectWindowSeconds   au début de la parade, durée pendant laquelle un coup est bloqué à 100 % (0 = jamais)
 * @param perfectCooldownSeconds après cette fenêtre, délai avant de pouvoir en obtenir une nouvelle en relevant la garde
 * @param guardBreakSeconds      durée pendant laquelle la parade est impossible quand la garde est brisée (0 = jamais brisée)
 * @param perfectDamageBonus     après une parade parfaite, dégâts en plus sur le coup suivant, en % (0 = aucun)
 * @param perfectBonusSeconds    temps pendant lequel ce bonus reste disponible
 */
public record ParryDefinition(double damageReduction, double durabilityPerDamage, boolean whileDualWielding,
                              double perfectWindowSeconds, double perfectCooldownSeconds, double guardBreakSeconds,
                              double perfectDamageBonus, double perfectBonusSeconds) {

    // 50 % : la parade à l'épée de la 1.8 divisait les dégâts par deux.
    public static final DataField<Double> DAMAGE_REDUCTION = DataField.number("damage_reduction", 0.0D, 100.0D, 50.0D);
    public static final DataField<Double> DURABILITY_PER_DAMAGE = DataField.number("durability_per_damage", 0.0D, 10.0D, 1.0D);
    public static final DataField<Boolean> WHILE_DUAL_WIELDING = DataField.bool("while_dual_wielding", false);
    // 0.25 s = 5 ticks : assez court pour demander du timing, assez long pour rester faisable avec la latence.
    public static final DataField<Double> PERFECT_WINDOW = DataField.number("perfect_window_seconds", 0.0D, 2.0D, 0.25D);
    public static final DataField<Double> PERFECT_COOLDOWN = DataField.number("perfect_cooldown_seconds", 0.0D, 10.0D, 1.0D);
    public static final DataField<Double> GUARD_BREAK = DataField.number("guard_break_seconds", 0.0D, 10.0D, 1.5D);
    public static final DataField<Double> PERFECT_DAMAGE_BONUS = DataField.number("perfect_damage_bonus", 0.0D, 500.0D, 20.0D);
    public static final DataField<Double> PERFECT_BONUS_SECONDS = DataField.number("perfect_bonus_seconds", 0.5D, 30.0D, 3.0D);

    public static final FieldSet FIELDS = new FieldSet("weapon.parry",
            List.of(DAMAGE_REDUCTION, DURABILITY_PER_DAMAGE, WHILE_DUAL_WIELDING, PERFECT_WINDOW, PERFECT_COOLDOWN, GUARD_BREAK,
                    PERFECT_DAMAGE_BONUS, PERFECT_BONUS_SECONDS),
            Map.ofEntries(
                    Map.entry("reduction", "damage_reduction"),
                    Map.entry("damage", "damage_reduction"),
                    Map.entry("durability", "durability_per_damage"),
                    Map.entry("durability_per_hit", "durability_per_damage"),
                    Map.entry("dual_wield", "while_dual_wielding"),
                    Map.entry("parry_while_dual_wielding", "while_dual_wielding"),
                    Map.entry("perfect_window", "perfect_window_seconds"),
                    Map.entry("perfect_cooldown", "perfect_cooldown_seconds"),
                    Map.entry("guard_break", "guard_break_seconds"),
                    Map.entry("damage_bonus", "perfect_damage_bonus"),
                    Map.entry("perfect_bonus", "perfect_damage_bonus")));

    public static final Codec<ParryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DAMAGE_REDUCTION.forGetter(ParryDefinition::damageReduction),
            DURABILITY_PER_DAMAGE.forGetter(ParryDefinition::durabilityPerDamage),
            WHILE_DUAL_WIELDING.forGetter(ParryDefinition::whileDualWielding),
            PERFECT_WINDOW.forGetter(ParryDefinition::perfectWindowSeconds),
            PERFECT_COOLDOWN.forGetter(ParryDefinition::perfectCooldownSeconds),
            GUARD_BREAK.forGetter(ParryDefinition::guardBreakSeconds),
            PERFECT_DAMAGE_BONUS.forGetter(ParryDefinition::perfectDamageBonus),
            PERFECT_BONUS_SECONDS.forGetter(ParryDefinition::perfectBonusSeconds)
    ).apply(instance, ParryDefinition::new));

    public static final ParryDefinition DEFAULT = new ParryDefinition(
            DAMAGE_REDUCTION.defaultValue(), DURABILITY_PER_DAMAGE.defaultValue(), WHILE_DUAL_WIELDING.defaultValue(),
            PERFECT_WINDOW.defaultValue(), PERFECT_COOLDOWN.defaultValue(), GUARD_BREAK.defaultValue(),
            PERFECT_DAMAGE_BONUS.defaultValue(), PERFECT_BONUS_SECONDS.defaultValue());
}
