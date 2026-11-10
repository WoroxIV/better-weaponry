package com.betterweaponry.compat;

import com.betterweaponry.data.DataField;
import com.betterweaponry.data.FieldSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Réglages lus dans {@code better_weaponry/compat.json}.
 *
 * @param formatVersion        version du format de fichier (1)
 * @param reachEnabled         portée d'attaque propre à chaque arme
 * @param balanceAbilities     avec Combat Reforged, les armes à capacités font un peu moins de dégâts
 * @param reachDisabledByMods  mods qui ont leur propre système de portée : s'ils sont chargés, celle des armes se coupe
 * @param combatReforgedNotice message unique en jeu qui indique où trouver le système de combat
 */
public record CompatDefinition(int formatVersion, boolean reachEnabled, boolean balanceAbilities, List<String> reachDisabledByMods, boolean combatReforgedNotice) {
    public static final int FORMAT_VERSION = 1;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    public static final DataField<Boolean> REACH_ENABLED = DataField.bool("reach_enabled", true);
    public static final DataField<Boolean> BALANCE_ABILITIES = DataField.bool("balance_abilities", false);
    public static final DataField<List<String>> REACH_DISABLED_BY_MODS = DataField.itemList("reach_disabled_by_mods", 64, "bettercombat", "combat_reforged", "epicfight");
    public static final DataField<Boolean> COMBAT_REFORGED_NOTICE = DataField.bool("combat_reforged_notice", true);

    public static final FieldSet FIELDS = new FieldSet("compat",
            List.of(FORMAT, REACH_ENABLED, BALANCE_ABILITIES, REACH_DISABLED_BY_MODS, COMBAT_REFORGED_NOTICE),
            Map.of("reach", "reach_enabled", "balance", "balance_abilities", "notice", "combat_reforged_notice"));

    public static final Codec<CompatDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(CompatDefinition::formatVersion),
            REACH_ENABLED.forGetter(CompatDefinition::reachEnabled),
            BALANCE_ABILITIES.forGetter(CompatDefinition::balanceAbilities),
            REACH_DISABLED_BY_MODS.forGetter(CompatDefinition::reachDisabledByMods),
            COMBAT_REFORGED_NOTICE.forGetter(CompatDefinition::combatReforgedNotice)
    ).apply(instance, CompatDefinition::new));

    public static final CompatDefinition FALLBACK = new CompatDefinition(FORMAT_VERSION, true, false, List.of("bettercombat", "combat_reforged", "epicfight"), true);
}
