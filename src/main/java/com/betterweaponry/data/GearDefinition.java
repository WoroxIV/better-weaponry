package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Réglages de l'équipement (boucliers, butin), lus dans {@code better_weaponry/gear.json}.
 * Même fonctionnement que {@link RulesDefinition} : un champ déclaré ici est lu, validé et documenté automatiquement.
 *
 * @param formatVersion            version du format de fichier (1)
 * @param shieldPerfectWindow      après la levée du bouclier, durée pendant laquelle un coup compte comme parade parfaite
 * @param shieldPerfectCooldown    après cette fenêtre, délai avant d'en obtenir une nouvelle en relevant le bouclier
 * @param shieldPerfectDamageBonus dégâts en plus sur le coup qui suit une parade parfaite au bouclier, en %
 * @param shieldPerfectBonusSeconds temps pendant lequel ce bonus reste disponible
 * @param shieldPerfectKnockback   recul donné à l'attaquant au corps à corps par une parade parfaite au bouclier
 * @param reflectArrows            une parade parfaite au bouclier renvoie les flèches vers le tireur
 * @param replaceVanillaGear       le butin et les villageois donnent les versions du mod au lieu du vanilla
 * @param lootUpgradeChance        dans le butin, chance en % de passer au matériau suivant (répétée à chaque matériau)
 * @param enchantedBookChance      chance en % d'un livre d'enchantement du mod dans les coffres du Nether et de l'End
 */
public record GearDefinition(int formatVersion, double shieldPerfectWindow, double shieldPerfectCooldown,
                             double shieldPerfectDamageBonus, double shieldPerfectBonusSeconds, double shieldPerfectKnockback,
                             boolean reflectArrows, boolean replaceVanillaGear, double lootUpgradeChance, double enchantedBookChance) {

    public static final int FORMAT_VERSION = 1;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    public static final DataField<Double> SHIELD_PERFECT_WINDOW = DataField.number("shield_perfect_window_seconds", 0.0D, 2.0D, 0.25D);
    public static final DataField<Double> SHIELD_PERFECT_COOLDOWN = DataField.number("shield_perfect_cooldown_seconds", 0.0D, 10.0D, 1.0D);
    public static final DataField<Double> SHIELD_PERFECT_DAMAGE_BONUS = DataField.number("shield_perfect_damage_bonus", 0.0D, 500.0D, 20.0D);
    public static final DataField<Double> SHIELD_PERFECT_BONUS_SECONDS = DataField.number("shield_perfect_bonus_seconds", 0.5D, 30.0D, 3.0D);
    public static final DataField<Double> SHIELD_PERFECT_KNOCKBACK = DataField.number("shield_perfect_knockback", 0.0D, 5.0D, 0.4D);
    public static final DataField<Boolean> REFLECT_ARROWS = DataField.bool("reflect_arrows", true);
    public static final DataField<Boolean> REPLACE_VANILLA_GEAR = DataField.bool("replace_vanilla_gear", true);
    public static final DataField<Double> LOOT_UPGRADE_CHANCE = DataField.number("loot_upgrade_chance", 0.0D, 100.0D, 20.0D);
    public static final DataField<Double> ENCHANTED_BOOK_CHANCE = DataField.number("enchanted_book_chance", 0.0D, 100.0D, 20.0D);

    public static final FieldSet FIELDS = new FieldSet("gear",
            List.of(FORMAT, SHIELD_PERFECT_WINDOW, SHIELD_PERFECT_COOLDOWN, SHIELD_PERFECT_DAMAGE_BONUS, SHIELD_PERFECT_BONUS_SECONDS,
                    SHIELD_PERFECT_KNOCKBACK, REFLECT_ARROWS, REPLACE_VANILLA_GEAR, LOOT_UPGRADE_CHANCE, ENCHANTED_BOOK_CHANCE),
            Map.of("perfect_window", "shield_perfect_window_seconds",
                    "perfect_cooldown", "shield_perfect_cooldown_seconds",
                    "knockback", "shield_perfect_knockback",
                    "replace_vanilla", "replace_vanilla_gear",
                    "upgrade_chance", "loot_upgrade_chance",
                    "book_chance", "enchanted_book_chance"));

    public static final Codec<GearDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(GearDefinition::formatVersion),
            SHIELD_PERFECT_WINDOW.forGetter(GearDefinition::shieldPerfectWindow),
            SHIELD_PERFECT_COOLDOWN.forGetter(GearDefinition::shieldPerfectCooldown),
            SHIELD_PERFECT_DAMAGE_BONUS.forGetter(GearDefinition::shieldPerfectDamageBonus),
            SHIELD_PERFECT_BONUS_SECONDS.forGetter(GearDefinition::shieldPerfectBonusSeconds),
            SHIELD_PERFECT_KNOCKBACK.forGetter(GearDefinition::shieldPerfectKnockback),
            REFLECT_ARROWS.forGetter(GearDefinition::reflectArrows),
            REPLACE_VANILLA_GEAR.forGetter(GearDefinition::replaceVanillaGear),
            LOOT_UPGRADE_CHANCE.forGetter(GearDefinition::lootUpgradeChance),
            ENCHANTED_BOOK_CHANCE.forGetter(GearDefinition::enchantedBookChance)
    ).apply(instance, GearDefinition::new));

    public static final GearDefinition FALLBACK = new GearDefinition(FORMAT_VERSION,
            SHIELD_PERFECT_WINDOW.defaultValue(), SHIELD_PERFECT_COOLDOWN.defaultValue(), SHIELD_PERFECT_DAMAGE_BONUS.defaultValue(),
            SHIELD_PERFECT_BONUS_SECONDS.defaultValue(), SHIELD_PERFECT_KNOCKBACK.defaultValue(), REFLECT_ARROWS.defaultValue(),
            REPLACE_VANILLA_GEAR.defaultValue(), LOOT_UPGRADE_CHANCE.defaultValue(), ENCHANTED_BOOK_CHANCE.defaultValue());
}
