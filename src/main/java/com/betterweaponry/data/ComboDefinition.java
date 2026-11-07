package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Combo d'une arme (objet "combo" d'un fichier d'arme) : une suite de coups qui se répète.
 * Les coups qui touchent font avancer le combo ; un coup dans le vide ne compte pas.
 *
 * @param enabled           combo actif
 * @param requiresDualWield le combo n'existe qu'avec une arme du même type dans l'autre main
 * @param resetAfterSeconds le combo repart du début après ce temps sans toucher
 * @param minCharge         un coup chargé à moins de ce pourcentage remet le combo à zéro (anti-spam)
 * @param steps             les coups, dans l'ordre ; après le dernier, on recommence au premier
 */
public record ComboDefinition(boolean enabled, boolean requiresDualWield, double resetAfterSeconds, double minCharge,
                              List<ComboStep> steps) {

    private static final int MAX_STEPS = 8;

    public static final DataField<Boolean> ENABLED = DataField.bool("enabled", false);
    public static final DataField<Boolean> REQUIRES_DUAL_WIELD = DataField.bool("requires_dual_wield", false);
    public static final DataField<Double> RESET_AFTER = DataField.number("reset_after_seconds", 0.5D, 10.0D, 2.0D);
    public static final DataField<Double> MIN_CHARGE = DataField.number("min_charge", 0.0D, 100.0D, 90.0D);
    public static final DataField<List<ComboStep>> STEPS = DataField.objectList("steps", ComboStep.FIELDS, ComboStep.CODEC, MAX_STEPS);

    public static final FieldSet FIELDS = new FieldSet("weapon.combo",
            List.of(ENABLED, REQUIRES_DUAL_WIELD, RESET_AFTER, MIN_CHARGE, STEPS),
            Map.of("enable", "enabled",
                    "dual_wield", "requires_dual_wield",
                    "requires_dual_wielding", "requires_dual_wield",
                    "reset_after", "reset_after_seconds",
                    "reset_seconds", "reset_after_seconds",
                    "charge", "min_charge",
                    "hits", "steps"));

    public static final Codec<ComboDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENABLED.forGetter(ComboDefinition::enabled),
            REQUIRES_DUAL_WIELD.forGetter(ComboDefinition::requiresDualWield),
            RESET_AFTER.forGetter(ComboDefinition::resetAfterSeconds),
            MIN_CHARGE.forGetter(ComboDefinition::minCharge),
            STEPS.forGetter(ComboDefinition::steps)
    ).apply(instance, ComboDefinition::new));

    public static final ComboDefinition NONE = new ComboDefinition(false, false, RESET_AFTER.defaultValue(),
            MIN_CHARGE.defaultValue(), List.of());

    public ComboDefinition {
        steps = List.copyOf(steps);
    }
}
