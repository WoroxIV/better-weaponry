package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Règles globales du mod, lues dans {@code better_weaponry/rules.json}.
 * Même fonctionnement que {@link WeaponDefinition} : un champ déclaré ici est lu, validé et documenté automatiquement.
 *
 * @param formatVersion                   version du format de fichier (1)
 * @param dualWieldSpeedMultiplier        multiplicateur de vitesse d'attaque avec une arme dans chaque main
 * @param dualWieldInvulnerabilitySeconds temps pendant lequel une cible ignore les nouveaux coups après un coup
 *                                        porté en dual wield (vanilla : 0.5 s)
 * @param parryEnabled                    false = aucune arme ne pare, quel que soit son fichier
 * @param dashEnabled                     false = aucune arme ne fait de dash, quel que soit son fichier
 */
public record RulesDefinition(int formatVersion, double dualWieldSpeedMultiplier, double dualWieldInvulnerabilitySeconds,
                              boolean parryEnabled, boolean dashEnabled) {

    public static final int FORMAT_VERSION = 1;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    // 1.5 : le dual wield vaut alors le bonus d'environ 1.5× des armes à deux mains.
    public static final DataField<Double> DUAL_WIELD_SPEED_MULTIPLIER = DataField.number("dual_wield_speed_multiplier", 1.0D, 5.0D, 1.5D);
    // 0.25 s : une dague en dual wield (3 coups/s, un coup toutes les 0.33 s) ne perd plus aucun coup.
    public static final DataField<Double> DUAL_WIELD_INVULNERABILITY = DataField.number("dual_wield_invulnerability_seconds", 0.0D, 1.0D, 0.25D);
    public static final DataField<Boolean> PARRY_ENABLED = DataField.bool("parry_enabled", true);
    public static final DataField<Boolean> DASH_ENABLED = DataField.bool("dash_enabled", true);

    public static final FieldSet FIELDS = new FieldSet("rules",
            List.of(FORMAT, DUAL_WIELD_SPEED_MULTIPLIER, DUAL_WIELD_INVULNERABILITY, PARRY_ENABLED, DASH_ENABLED),
            Map.of("dual_wield_multiplier", "dual_wield_speed_multiplier",
                    "dual_wield_invulnerability", "dual_wield_invulnerability_seconds",
                    "invulnerability_seconds", "dual_wield_invulnerability_seconds",
                    "parry", "parry_enabled",
                    "enable_parry", "parry_enabled",
                    "dash", "dash_enabled",
                    "enable_dash", "dash_enabled"));

    public static final Codec<RulesDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(RulesDefinition::formatVersion),
            DUAL_WIELD_SPEED_MULTIPLIER.forGetter(RulesDefinition::dualWieldSpeedMultiplier),
            DUAL_WIELD_INVULNERABILITY.forGetter(RulesDefinition::dualWieldInvulnerabilitySeconds),
            PARRY_ENABLED.forGetter(RulesDefinition::parryEnabled),
            DASH_ENABLED.forGetter(RulesDefinition::dashEnabled)
    ).apply(instance, RulesDefinition::new));

    public static final RulesDefinition FALLBACK = new RulesDefinition(FORMAT_VERSION,
            DUAL_WIELD_SPEED_MULTIPLIER.defaultValue(), DUAL_WIELD_INVULNERABILITY.defaultValue(), PARRY_ENABLED.defaultValue(),
            DASH_ENABLED.defaultValue());
}
