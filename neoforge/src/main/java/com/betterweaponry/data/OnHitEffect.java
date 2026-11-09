package com.betterweaponry.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Un effet donné à la cible touchée, ex : { "effect": "minecraft:slowness", "duration_seconds": 2, "level": 1 }.
 *
 * @param effect          identifiant de l'effet (n'importe quel effet du jeu ou d'un mod)
 * @param durationSeconds durée en secondes
 * @param level           niveau, comme affiché en jeu (1 = Lenteur I)
 * @param chance          chance de le donner à chaque coup, en % (100 = toujours)
 */
public record OnHitEffect(ResourceLocation effect, double durationSeconds, int level, double chance) {
    private static final double TICKS_PER_SECOND = 20.0D;

    public static final DataField<ResourceLocation> EFFECT = DataField.id("effect", "effect", BuiltInRegistries.MOB_EFFECT::keySet,
            "minecraft:slowness", "minecraft:weakness", "minecraft:poison", "minecraft:wither");
    public static final DataField<Double> DURATION = DataField.number("duration_seconds", 0.05D, 3600.0D, 2.0D);
    // 255 = niveau maximal du jeu (amplificateur 254)
    public static final DataField<Integer> LEVEL = DataField.integer("level", 1, 255, 1);
    public static final DataField<Double> CHANCE = DataField.number("chance", 0.0D, 100.0D, 100.0D);

    public static final FieldSet FIELDS = new FieldSet("weapon.on_hit_effects",
            List.of(EFFECT, DURATION, LEVEL, CHANCE),
            Map.of("id", "effect", "duration", "duration_seconds", "seconds", "duration_seconds",
                    "amplifier", "level", "probability", "chance"));

    public static final Codec<OnHitEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EFFECT.forGetter(OnHitEffect::effect),
            DURATION.forGetter(OnHitEffect::durationSeconds),
            LEVEL.forGetter(OnHitEffect::level),
            CHANCE.forGetter(OnHitEffect::chance)
    ).apply(instance, OnHitEffect::new));

    /** L'effet du registre, vide s'il a disparu (mod retiré depuis la validation). */
    public Optional<Holder<MobEffect>> holder() {
        return BuiltInRegistries.MOB_EFFECT.getHolder(effect).map(reference -> reference);
    }

    public int durationTicks() {
        return Math.max(1, (int) Math.round(durationSeconds * TICKS_PER_SECOND));
    }

    /** Effet prêt à appliquer (le jeu compte les niveaux à partir de 0). */
    public Optional<MobEffectInstance> createInstance() {
        return holder().map(holder -> new MobEffectInstance(holder, durationTicks(), level - 1));
    }
}
