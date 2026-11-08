package com.betterweaponry.data;

import com.betterweaponry.compat.CombatCompat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Règles globales du mod, lues dans {@code better_weaponry/rules.json}.
 * Même fonctionnement que {@link WeaponDefinition} : un champ déclaré ici est lu, validé et documenté automatiquement.
 * <p>
 * Chaque système de combat a un interrupteur, et {@code combat_changes_enabled} les coupe tous. Le code n'utilise
 * jamais les interrupteurs directement mais les méthodes {@code ...Active()} : elles tiennent compte de l'interrupteur
 * général et de la présence de Better Combat ou d'Epic Fight, qui remplacent nos systèmes.
 * <p>
 * Ce record a atteint les 16 champs maximum d'un codec Minecraft : un nouveau réglage devra aller dans un sous-objet.
 *
 * @param formatVersion                   version du format de fichier (1)
 * @param combatChangesEnabled            false = combat vanilla : tous les interrupteurs ci-dessous sont coupés
 * @param dualWieldSpeedMultiplier        multiplicateur de vitesse d'attaque avec une arme dans chaque main
 * @param dualWieldInvulnerabilitySeconds temps pendant lequel une cible ignore les nouveaux coups après un coup
 *                                        porté en dual wield (vanilla : 0.5 s)
 * @param reachEnabled                    portée d'attaque propre à chaque arme
 * @param sweepEnabled                    réglages de coup balayé propres à chaque arme
 * @param armorPenetrationEnabled         pénétration d'armure
 * @param onHitEffectsEnabled             effets donnés à la cible touchée
 * @param shieldDisablingEnabled          armes qui désactivent les boucliers
 * @param twoHandedEnabled                armes à deux mains (main secondaire bloquée)
 * @param dualWieldEnabled                une arme dans chaque main
 * @param parryEnabled                    parade
 * @param perfectParryEnabled             parade parfaite
 * @param guardBreakEnabled               garde brisée
 * @param dashEnabled                     dash
 * @param combosEnabled                   combos
 */
public record RulesDefinition(int formatVersion, boolean combatChangesEnabled,
                              double dualWieldSpeedMultiplier, double dualWieldInvulnerabilitySeconds,
                              boolean reachEnabled, boolean sweepEnabled, boolean armorPenetrationEnabled, boolean onHitEffectsEnabled,
                              boolean shieldDisablingEnabled, boolean twoHandedEnabled, boolean dualWieldEnabled,
                              boolean parryEnabled, boolean perfectParryEnabled, boolean guardBreakEnabled,
                              boolean dashEnabled, boolean combosEnabled) {

    public static final int FORMAT_VERSION = 1;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    public static final DataField<Boolean> COMBAT_CHANGES_ENABLED = DataField.bool("combat_changes_enabled", true);
    // 1.5 : le dual wield vaut alors le bonus d'environ 1.5× des armes à deux mains.
    public static final DataField<Double> DUAL_WIELD_SPEED_MULTIPLIER = DataField.number("dual_wield_speed_multiplier", 1.0D, 5.0D, 1.5D);
    // 0.25 s : une dague en dual wield (3 coups/s, un coup toutes les 0.33 s) ne perd plus aucun coup.
    public static final DataField<Double> DUAL_WIELD_INVULNERABILITY = DataField.number("dual_wield_invulnerability_seconds", 0.0D, 1.0D, 0.25D);
    public static final DataField<Boolean> REACH_ENABLED = DataField.bool("reach_enabled", true);
    public static final DataField<Boolean> SWEEP_ENABLED = DataField.bool("sweep_enabled", true);
    public static final DataField<Boolean> ARMOR_PENETRATION_ENABLED = DataField.bool("armor_penetration_enabled", true);
    public static final DataField<Boolean> ON_HIT_EFFECTS_ENABLED = DataField.bool("on_hit_effects_enabled", true);
    public static final DataField<Boolean> SHIELD_DISABLING_ENABLED = DataField.bool("shield_disabling_enabled", true);
    public static final DataField<Boolean> TWO_HANDED_ENABLED = DataField.bool("two_handed_enabled", true);
    public static final DataField<Boolean> DUAL_WIELD_ENABLED = DataField.bool("dual_wield_enabled", true);
    public static final DataField<Boolean> PARRY_ENABLED = DataField.bool("parry_enabled", true);
    public static final DataField<Boolean> PERFECT_PARRY_ENABLED = DataField.bool("perfect_parry_enabled", true);
    public static final DataField<Boolean> GUARD_BREAK_ENABLED = DataField.bool("guard_break_enabled", true);
    public static final DataField<Boolean> DASH_ENABLED = DataField.bool("dash_enabled", true);
    public static final DataField<Boolean> COMBOS_ENABLED = DataField.bool("combos_enabled", true);

    public static final FieldSet FIELDS = new FieldSet("rules",
            List.of(FORMAT, COMBAT_CHANGES_ENABLED, DUAL_WIELD_SPEED_MULTIPLIER, DUAL_WIELD_INVULNERABILITY,
                    REACH_ENABLED, SWEEP_ENABLED, ARMOR_PENETRATION_ENABLED, ON_HIT_EFFECTS_ENABLED,
                    SHIELD_DISABLING_ENABLED, TWO_HANDED_ENABLED, DUAL_WIELD_ENABLED,
                    PARRY_ENABLED, PERFECT_PARRY_ENABLED, GUARD_BREAK_ENABLED, DASH_ENABLED, COMBOS_ENABLED),
            Map.ofEntries(
                    Map.entry("dual_wield_multiplier", "dual_wield_speed_multiplier"),
                    Map.entry("dual_wield_invulnerability", "dual_wield_invulnerability_seconds"),
                    Map.entry("invulnerability_seconds", "dual_wield_invulnerability_seconds"),
                    Map.entry("combat_changes", "combat_changes_enabled"),
                    Map.entry("enabled", "combat_changes_enabled"),
                    Map.entry("reach", "reach_enabled"),
                    Map.entry("sweep", "sweep_enabled"),
                    Map.entry("armor_penetration", "armor_penetration_enabled"),
                    Map.entry("on_hit_effects", "on_hit_effects_enabled"),
                    Map.entry("shield_disabling", "shield_disabling_enabled"),
                    Map.entry("two_handed", "two_handed_enabled"),
                    Map.entry("dual_wield", "dual_wield_enabled"),
                    Map.entry("parry", "parry_enabled"),
                    Map.entry("enable_parry", "parry_enabled"),
                    Map.entry("perfect_parry", "perfect_parry_enabled"),
                    Map.entry("guard_break", "guard_break_enabled"),
                    Map.entry("dash", "dash_enabled"),
                    Map.entry("enable_dash", "dash_enabled"),
                    Map.entry("combo_enabled", "combos_enabled"),
                    Map.entry("combos", "combos_enabled")));

    public static final Codec<RulesDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(RulesDefinition::formatVersion),
            COMBAT_CHANGES_ENABLED.forGetter(RulesDefinition::combatChangesEnabled),
            DUAL_WIELD_SPEED_MULTIPLIER.forGetter(RulesDefinition::dualWieldSpeedMultiplier),
            DUAL_WIELD_INVULNERABILITY.forGetter(RulesDefinition::dualWieldInvulnerabilitySeconds),
            REACH_ENABLED.forGetter(RulesDefinition::reachEnabled),
            SWEEP_ENABLED.forGetter(RulesDefinition::sweepEnabled),
            ARMOR_PENETRATION_ENABLED.forGetter(RulesDefinition::armorPenetrationEnabled),
            ON_HIT_EFFECTS_ENABLED.forGetter(RulesDefinition::onHitEffectsEnabled),
            SHIELD_DISABLING_ENABLED.forGetter(RulesDefinition::shieldDisablingEnabled),
            TWO_HANDED_ENABLED.forGetter(RulesDefinition::twoHandedEnabled),
            DUAL_WIELD_ENABLED.forGetter(RulesDefinition::dualWieldEnabled),
            PARRY_ENABLED.forGetter(RulesDefinition::parryEnabled),
            PERFECT_PARRY_ENABLED.forGetter(RulesDefinition::perfectParryEnabled),
            GUARD_BREAK_ENABLED.forGetter(RulesDefinition::guardBreakEnabled),
            DASH_ENABLED.forGetter(RulesDefinition::dashEnabled),
            COMBOS_ENABLED.forGetter(RulesDefinition::combosEnabled)
    ).apply(instance, RulesDefinition::new));

    public static final RulesDefinition FALLBACK = new RulesDefinition(FORMAT_VERSION, true,
            DUAL_WIELD_SPEED_MULTIPLIER.defaultValue(), DUAL_WIELD_INVULNERABILITY.defaultValue(),
            true, true, true, true, true, true, true, true, true, true, true, true);

    // --- Ce qui est réellement actif (interrupteur général + interrupteur du système + mods de combat) ---

    private boolean active(boolean toggle) {
        return combatChangesEnabled && toggle && !CombatCompat.externalCombatMod();
    }

    public boolean reachActive() {
        return active(reachEnabled);
    }

    public boolean sweepActive() {
        return active(sweepEnabled);
    }

    public boolean armorPenetrationActive() {
        return active(armorPenetrationEnabled);
    }

    public boolean onHitEffectsActive() {
        return active(onHitEffectsEnabled);
    }

    public boolean shieldDisablingActive() {
        return active(shieldDisablingEnabled);
    }

    public boolean twoHandedActive() {
        return active(twoHandedEnabled);
    }

    public boolean dualWieldActive() {
        return active(dualWieldEnabled);
    }

    public boolean parryActive() {
        return active(parryEnabled);
    }

    /** La parade parfaite et la garde brisée n'existent que si la parade est active. */
    public boolean perfectParryActive() {
        return parryActive() && perfectParryEnabled;
    }

    public boolean guardBreakActive() {
        return parryActive() && guardBreakEnabled;
    }

    public boolean dashActive() {
        return active(dashEnabled);
    }

    public boolean combosActive() {
        return active(combosEnabled);
    }
}
