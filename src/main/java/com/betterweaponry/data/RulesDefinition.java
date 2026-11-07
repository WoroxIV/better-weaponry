package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Règles globales du mod, lues dans {@code better_weaponry/rules.json}.
 * Même fonctionnement que {@link WeaponDefinition} : un champ déclaré ici est lu, validé et documenté automatiquement.
 *
 * @param formatVersion            version du format de fichier (1)
 * @param dualWieldSpeedMultiplier multiplicateur de vitesse d'attaque avec une arme dans chaque main
 */
public record RulesDefinition(int formatVersion, double dualWieldSpeedMultiplier) {

    public static final int FORMAT_VERSION = 1;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    // 1.5 : le dual wield vaut alors le bonus d'environ 1.5× des armes à deux mains.
    public static final DataField<Double> DUAL_WIELD_SPEED_MULTIPLIER = DataField.number("dual_wield_speed_multiplier", 1.0D, 5.0D, 1.5D);

    public static final FieldSet FIELDS = new FieldSet("rules",
            List.of(FORMAT, DUAL_WIELD_SPEED_MULTIPLIER),
            Map.of("dual_wield_multiplier", "dual_wield_speed_multiplier"));

    public static final Codec<RulesDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(RulesDefinition::formatVersion),
            DUAL_WIELD_SPEED_MULTIPLIER.forGetter(RulesDefinition::dualWieldSpeedMultiplier)
    ).apply(instance, RulesDefinition::new));

    public static final RulesDefinition FALLBACK = new RulesDefinition(FORMAT_VERSION, DUAL_WIELD_SPEED_MULTIPLIER.defaultValue());
}
