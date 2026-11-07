package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Réglages du dash (objet "dash" d'un fichier d'arme), utilisés quand "ability" vaut "dash".
 * Un clic droit projette le joueur, puis le type d'arme a un temps de recharge (barre d'objets vanilla).
 *
 * @param distance        longueur approximative du dash sur sol plat, en blocs
 * @param cooldownSeconds temps avant le dash suivant, commun à tous les matériaux du type
 * @param direction       vers le regard (fente) ou vers les touches de déplacement (esquive)
 * @param allowAirborne   le dash marche aussi pendant un saut ou une chute
 * @param hungerCost      nourriture consommée, en points (1 = un demi-pilon de la barre de faim)
 */
public record DashDefinition(double distance, double cooldownSeconds, DashDirection direction, boolean allowAirborne,
                             double hungerCost) {

    public static final DataField<Double> DISTANCE = DataField.number("distance", 0.5D, 16.0D, 4.0D);
    public static final DataField<Double> COOLDOWN = DataField.number("cooldown_seconds", 0.1D, 60.0D, 2.0D);
    public static final DataField<DashDirection> DIRECTION = DataField.choice("direction", DashDirection.values(), DashDirection.LOOK);
    public static final DataField<Boolean> ALLOW_AIRBORNE = DataField.bool("allow_airborne", false);
    // 0.1 point = l'équivalent de deux sauts en sprintant.
    public static final DataField<Double> HUNGER_COST = DataField.number("hunger_cost", 0.0D, 20.0D, 0.1D);

    public static final FieldSet FIELDS = new FieldSet("weapon.dash",
            List.of(DISTANCE, COOLDOWN, DIRECTION, ALLOW_AIRBORNE, HUNGER_COST),
            Map.of("strength", "distance",
                    "range", "distance",
                    "cooldown", "cooldown_seconds",
                    "airborne", "allow_airborne",
                    "allow_air", "allow_airborne",
                    "hunger", "hunger_cost",
                    "food_cost", "hunger_cost"));

    public static final Codec<DashDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DISTANCE.forGetter(DashDefinition::distance),
            COOLDOWN.forGetter(DashDefinition::cooldownSeconds),
            DIRECTION.forGetter(DashDefinition::direction),
            ALLOW_AIRBORNE.forGetter(DashDefinition::allowAirborne),
            HUNGER_COST.forGetter(DashDefinition::hungerCost)
    ).apply(instance, DashDefinition::new));

    public static final DashDefinition DEFAULT = new DashDefinition(DISTANCE.defaultValue(), COOLDOWN.defaultValue(),
            DIRECTION.defaultValue(), ALLOW_AIRBORNE.defaultValue(), HUNGER_COST.defaultValue());
}
