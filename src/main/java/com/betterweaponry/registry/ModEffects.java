package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

/** Effets de potion du mod. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, BetterWeaponry.MODID);
    public static final ResourceKey<DamageType> BLEEDING_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "bleeding"));

    public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);

    private ModEffects() {
    }

    /** Saignement : 0.5 PV par seconde, sans tenir compte de l'armure. Volontairement faible : il complète les coups. */
    public static final class BleedingEffect extends MobEffect {
        private static final int COLOR = 0x8B0000;
        private static final int INTERVAL_TICKS = 20;
        private static final float DAMAGE = 0.5F;
        private static final int PARTICLES = 5;
        private static final DustParticleOptions DROPLET = new DustParticleOptions(new Vector3f(0.6F, 0.0F, 0.0F), 1.0F);

        private BleedingEffect() {
            super(MobEffectCategory.HARMFUL, COLOR);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % INTERVAL_TICKS == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level() instanceof ServerLevel level) {
                // Le saignement ne doit pas protéger la cible contre le coup suivant.
                int invulnerability = entity.invulnerableTime;
                entity.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLEEDING_DAMAGE)), DAMAGE);
                entity.invulnerableTime = invulnerability;
                level.sendParticles(DROPLET, entity.getX(), entity.getY(0.6D), entity.getZ(), PARTICLES, 0.25D, 0.3D, 0.25D, 0.0D);
            }
            return true;
        }
    }
}
