package com.betterweaponry.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import java.util.List;
import java.util.Set;

/**
 * Préréglages proposés dans la page Règles de l'éditeur. Un préréglage fixe tous les interrupteurs de rules.json et les
 * mobs de gear.json : appliquer l'un après l'autre donne toujours le même résultat.
 */
public enum RulesPreset {
    /** Les armes gardent leurs stats et leur portée, sans aucun nouveau système. */
    NEAR_VANILLA("near_vanilla", Set.of(RulesDefinition.REACH_ENABLED, RulesDefinition.SWEEP_ENABLED, RulesDefinition.SHIELD_DISABLING_ENABLED),
            RulesDefinition.DUAL_WIELD_SPEED_MULTIPLIER.defaultValue(), MobGearDefinition.DEFAULT),
    /** La parade simple de la 1.8, sans portée ni balayage propres aux armes. */
    PVP_1_8("pvp_1_8", Set.of(RulesDefinition.PARRY_ENABLED), 1.0D, MobGearDefinition.DEFAULT),
    /** Tous les systèmes avec l'équilibrage normal des armes, et des mobs mieux armés qui parent plus souvent. */
    HARD("hard", Set.copyOf(Switches.ALL), RulesDefinition.DUAL_WIELD_SPEED_MULTIPLIER.defaultValue(), new MobGearDefinition(15.0D, 75.0D, 10.0D, 15.0D, true, 50.0D));

    private static final String MOBS = GearDefinition.MOBS.name();

    private final String id;
    private final Set<DataField<Boolean>> enabled;
    private final double dualWieldSpeed;
    private final MobGearDefinition mobs;

    RulesPreset(String id, Set<DataField<Boolean>> enabled, double dualWieldSpeed, MobGearDefinition mobs) {
        this.id = id;
        this.enabled = enabled;
        this.dualWieldSpeed = dualWieldSpeed;
        this.mobs = mobs;
    }

    public String id() {
        return id;
    }

    /** Écrit ce préréglage dans les valeurs de rules.json (objet modifié sur place). */
    public void applyToRules(JsonObject rules) {
        rules.addProperty(RulesDefinition.COMBAT_CHANGES_ENABLED.name(), true);
        for (DataField<Boolean> field : Switches.ALL) {
            rules.addProperty(field.name(), enabled.contains(field));
        }
        rules.addProperty(RulesDefinition.DUAL_WIELD_SPEED_MULTIPLIER.name(), dualWieldSpeed);
        rules.addProperty(RulesDefinition.DUAL_WIELD_INVULNERABILITY.name(), RulesDefinition.DUAL_WIELD_INVULNERABILITY.defaultValue());
    }

    /** Écrit ce préréglage dans les valeurs de gear.json (objet modifié sur place). */
    public void applyToGear(JsonObject gear) {
        // Les valeurs égales au défaut ne sont pas encodées : l'éditeur a besoin de l'objet complet.
        JsonObject encoded = DataResults.orThrow(MobGearDefinition.CODEC.encodeStart(JsonOps.INSTANCE, mobs)).getAsJsonObject();
        gear.add(MOBS, JsonTree.expand(MobGearDefinition.FIELDS, encoded));
    }

    /** Liste à part : une constante d'enum ne peut pas lire un champ statique de sa propre classe. */
    private static final class Switches {
        private static final List<DataField<Boolean>> ALL = List.of(RulesDefinition.REACH_ENABLED, RulesDefinition.SWEEP_ENABLED,
                RulesDefinition.ARMOR_PENETRATION_ENABLED, RulesDefinition.ON_HIT_EFFECTS_ENABLED, RulesDefinition.SHIELD_DISABLING_ENABLED,
                RulesDefinition.TWO_HANDED_ENABLED, RulesDefinition.DUAL_WIELD_ENABLED, RulesDefinition.PARRY_ENABLED,
                RulesDefinition.PERFECT_PARRY_ENABLED, RulesDefinition.GUARD_BREAK_ENABLED, RulesDefinition.DASH_ENABLED, RulesDefinition.COMBOS_ENABLED);
    }
}
