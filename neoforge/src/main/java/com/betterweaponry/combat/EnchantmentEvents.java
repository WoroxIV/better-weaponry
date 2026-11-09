package com.betterweaponry.combat;

import com.betterweaponry.registry.ModStats;
import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.registry.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

/** Effets de Vampirisme, Exécution et Saignement sur les coups au corps à corps. */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class EnchantmentEvents {
    /** Au plus un soin par demi-seconde : un coup de zone ne soigne pas une fois par ennemi touché. */
    private static final int VAMPIRISM_COOLDOWN_TICKS = 10;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.05F), 1.0F);
    private static final float CHARGED_HIT = 0.9F;
    private static final int TICKS_PER_SECOND = 20;
    /** Saignement : durée en secondes = niveau + 1 (2, 3 ou 4 s). */
    private static final int BLEEDING_BASE_SECONDS = 1;
    private static final int EXECUTION_PARTICLES = 8;

    private EnchantmentEvents() {
    }

    /** Exécution : avant l'armure, comme le reste des bonus de dégâts. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity attacker = ModEnchantments.meleeAttacker(event.getSource());
        LivingEntity target = event.getEntity();
        if (attacker == null || target.getHealth() > target.getMaxHealth() * ModEnchantments.EXECUTION_HEALTH_THRESHOLD) return;
        int level = ModEnchantments.level(attacker.level(), attacker.getMainHandItem(), ModEnchantments.EXECUTION);
        if (level <= 0) return;

        event.setAmount((float) (event.getAmount() * (1.0D + level * ModEnchantments.EXECUTION_PER_LEVEL)));
        if (attacker.level() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(0.6D), target.getZ(), EXECUTION_PARTICLES, 0.3D, 0.4D, 0.3D, 0.02D);
        }
    }

    /** Une fois les dégâts réellement infligés (après armure). */
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(ModEnchantments.meleeAttacker(event.getSource()) instanceof Player attacker) || !(attacker.level() instanceof ServerLevel world)) return;
        ItemStack weapon = attacker.getMainHandItem();
        LivingEntity target = event.getEntity();
        applyVampirism(world, attacker, weapon, target, event.getNewDamage());
        applyBleeding(world, attacker, weapon, target);
    }

    private static void applyVampirism(ServerLevel world, Player attacker, ItemStack weapon, LivingEntity target, float damage) {
        int level = ModEnchantments.level(world, weapon, ModEnchantments.VAMPIRISM);
        PlayerCombatState state = PlayerCombatState.of(attacker);
        long now = world.getGameTime();
        if (level <= 0 || damage <= 0.0F || now < state.vampirismReadyAt) return;
        state.vampirismReadyAt = now + VAMPIRISM_COOLDOWN_TICKS;

        float healthBefore = attacker.getHealth();
        attacker.heal((float) ModEnchantments.vampirismHeal(damage, level));
        // La statistique compte ce qui a vraiment été regagné, en dixièmes de point de vie.
        if (attacker instanceof Player player) player.awardStat(ModStats.HEALTH_STOLEN.get(), Math.round((player.getHealth() - healthBefore) * 10.0F));
        ModEnchantments.trail(world, BLOOD, target.getEyePosition(), attacker.getEyePosition());
        world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    /** Seuls les coups bien chargés font saigner, et un squelette n'a pas de sang. */
    private static void applyBleeding(ServerLevel world, Player attacker, ItemStack weapon, LivingEntity target) {
        int level = ModEnchantments.level(world, weapon, ModEnchantments.BLEEDING);
        if (level <= 0 || PlayerCombatState.of(attacker).lastAttackStrength < CHARGED_HIT || target.getType().is(EntityTypeTags.SKELETONS)) return;
        target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, (BLEEDING_BASE_SECONDS + level) * TICKS_PER_SECOND), attacker);
    }
}
