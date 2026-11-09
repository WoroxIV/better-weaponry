package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Réglages des shurikens (objet "shurikens" de gear.json). Le matériau change la vitesse, comme pour les arcs.
 *
 * @param throwSpeed      vitesse de lancer d'un shuriken en pierre (une flèche d'arc bandé à fond : 3)
 * @param baseDamage      dégâts de base, multipliés par la vitesse à l'impact (une flèche : 2)
 * @param cooldownSeconds délai entre deux lancers
 */
public record ShurikenDefinition(double throwSpeed, double baseDamage, double cooldownSeconds) {

    public static final DataField<Double> THROW_SPEED = DataField.number("throw_speed", 0.1D, 5.0D, 2.0D);
    public static final DataField<Double> BASE_DAMAGE = DataField.number("base_damage", 0.0D, 20.0D, 1.5D);
    public static final DataField<Double> COOLDOWN = DataField.number("cooldown_seconds", 0.0D, 10.0D, 0.6D);

    public static final FieldSet FIELDS = new FieldSet("gear.shurikens", List.of(THROW_SPEED, BASE_DAMAGE, COOLDOWN), Map.of());

    public static final Codec<ShurikenDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            THROW_SPEED.forGetter(ShurikenDefinition::throwSpeed),
            BASE_DAMAGE.forGetter(ShurikenDefinition::baseDamage),
            COOLDOWN.forGetter(ShurikenDefinition::cooldownSeconds)
    ).apply(instance, ShurikenDefinition::new));

    public static final ShurikenDefinition DEFAULT = new ShurikenDefinition(THROW_SPEED.defaultValue(), BASE_DAMAGE.defaultValue(), COOLDOWN.defaultValue());
}
