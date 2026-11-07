package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Le coup balayé d'une arme (vanilla : coup chargé à fond, au sol, sans sprinter, qui touche aussi les ennemis
 * autour de la cible).
 *
 * @param enabled     false = jamais de coup balayé (armes d'estoc)
 * @param radius      taille de la zone autour de la cible, en blocs (vanilla : 1)
 * @param damageRatio dégâts des ennemis balayés, en % des dégâts de l'arme (vanilla : 0 = 1 point de dégât)
 */
public record SweepDefinition(boolean enabled, double radius, double damageRatio) {

    public static final DataField<Boolean> ENABLED = DataField.bool("enabled", true);
    // Au-delà, la zone dépasserait de toute façon la portée d'attaque, qui limite aussi le balayage.
    public static final DataField<Double> RADIUS = DataField.number("radius", 0.0D, 8.0D, 1.0D);
    public static final DataField<Double> DAMAGE_RATIO = DataField.number("damage_ratio", 0.0D, 100.0D, 0.0D);

    public static final FieldSet FIELDS = new FieldSet("weapon.sweep",
            List.of(ENABLED, RADIUS, DAMAGE_RATIO),
            Map.of("enable", "enabled", "size", "radius", "range", "radius", "damage", "damage_ratio"));

    public static final Codec<SweepDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENABLED.forGetter(SweepDefinition::enabled),
            RADIUS.forGetter(SweepDefinition::radius),
            DAMAGE_RATIO.forGetter(SweepDefinition::damageRatio)
    ).apply(instance, SweepDefinition::new));

    /** Comme une épée vanilla. */
    public static final SweepDefinition VANILLA = new SweepDefinition(true, 1.0D, 0.0D);
}
