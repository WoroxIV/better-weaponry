package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Armes et comportements des mobs (objet "mobs" de gear.json). Le nombre de mobs armés ne change pas :
 * seul un mob qui aurait reçu une arme ou un outil vanilla peut recevoir une arme du mod à la place.
 *
 * @param upgradeChance       chance en % de passer au matériau suivant (répétée à chaque matériau, ×1,5 en Difficile)
 * @param moddedWeaponChance  chance en % qu'une arme ou un outil vanilla devienne une arme du mod
 * @param dualWieldChance     parmi ces armes, chance en % d'en avoir une dans chaque main
 * @param ninjaSkeletonChance chance en % qu'un squelette lance des shurikens au lieu de tirer à l'arc
 * @param captainShield       les capitaines de patrouille et de raid portent un bouclier du mod
 * @param guardChance         à chaque occasion, chance en % qu'un mob au corps à corps lève sa garde
 */
public record MobGearDefinition(double upgradeChance, double moddedWeaponChance, double dualWieldChance,
                                double ninjaSkeletonChance, boolean captainShield, double guardChance) {

    // 9.5 % : la chance vanilla d'un mob d'avoir une armure d'un cran supérieur.
    public static final DataField<Double> UPGRADE_CHANCE = DataField.number("upgrade_chance", 0.0D, 100.0D, 9.5D);
    public static final DataField<Double> MODDED_WEAPON_CHANCE = DataField.number("modded_weapon_chance", 0.0D, 100.0D, 50.0D);
    public static final DataField<Double> DUAL_WIELD_CHANCE = DataField.number("dual_wield_chance", 0.0D, 100.0D, 5.0D);
    public static final DataField<Double> NINJA_SKELETON_CHANCE = DataField.number("ninja_skeleton_chance", 0.0D, 100.0D, 5.0D);
    public static final DataField<Boolean> CAPTAIN_SHIELD = DataField.bool("captain_shield", true);
    public static final DataField<Double> GUARD_CHANCE = DataField.number("guard_chance", 0.0D, 100.0D, 25.0D);

    public static final FieldSet FIELDS = new FieldSet("gear.mobs",
            List.of(UPGRADE_CHANCE, MODDED_WEAPON_CHANCE, DUAL_WIELD_CHANCE, NINJA_SKELETON_CHANCE, CAPTAIN_SHIELD, GUARD_CHANCE), Map.of());

    public static final Codec<MobGearDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UPGRADE_CHANCE.forGetter(MobGearDefinition::upgradeChance),
            MODDED_WEAPON_CHANCE.forGetter(MobGearDefinition::moddedWeaponChance),
            DUAL_WIELD_CHANCE.forGetter(MobGearDefinition::dualWieldChance),
            NINJA_SKELETON_CHANCE.forGetter(MobGearDefinition::ninjaSkeletonChance),
            CAPTAIN_SHIELD.forGetter(MobGearDefinition::captainShield),
            GUARD_CHANCE.forGetter(MobGearDefinition::guardChance)
    ).apply(instance, MobGearDefinition::new));

    public static final MobGearDefinition DEFAULT = new MobGearDefinition(UPGRADE_CHANCE.defaultValue(), MODDED_WEAPON_CHANCE.defaultValue(),
            DUAL_WIELD_CHANCE.defaultValue(), NINJA_SKELETON_CHANCE.defaultValue(), CAPTAIN_SHIELD.defaultValue(), GUARD_CHANCE.defaultValue());
}
