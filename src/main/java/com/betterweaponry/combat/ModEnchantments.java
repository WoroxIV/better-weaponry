package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Les enchantements du mod. Ils sont déclarés dans data/better_weaponry/enchantment/ (coût, niveaux, objets acceptés)
 * et leurs effets sont ici et dans {@link EnchantmentEvents}.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> REFLECTIVE = key("reflective");
    public static final ResourceKey<Enchantment> VAMPIRISM = key("vampirism");
    public static final ResourceKey<Enchantment> RIPOSTE = key("riposte");
    public static final ResourceKey<Enchantment> MOMENTUM = key("momentum");
    public static final ResourceKey<Enchantment> EXECUTION = key("execution");
    public static final ResourceKey<Enchantment> BLEEDING = key("bleeding");

    private static final double REFLECT_PER_LEVEL = 0.1D;
    private static final double VAMPIRISM_PER_LEVEL = 0.1D;
    /** Plus la valeur est petite, plus le soin plafonne vite sur les gros coups. */
    private static final double VAMPIRISM_CURVE = 2.0D;
    public static final double RIPOSTE_PERCENT_PER_LEVEL = 25.0D;
    private static final double MOMENTUM_PER_LEVEL = 0.15D;
    public static final double EXECUTION_PER_LEVEL = 0.1D;
    public static final double EXECUTION_HEALTH_THRESHOLD = 0.3D;
    /** Recul de base d'un coup renvoyé, comme un coup normal. */
    private static final double REFLECT_KNOCKBACK = 0.4D;
    private static final int REFLECT_POISON_TICKS = 140;
    private static final int REFLECT_WITHER_TICKS = 200;
    private static final int REFLECT_HUNGER_TICKS = 140;
    private static final int TRAIL_POINTS = 8;

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, path));
    }

    /** Niveau de cet enchantement sur l'objet, 0 s'il ne l'a pas (ou si un datapack a retiré l'enchantement). */
    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(enchantment)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }

    /**
     * Enchantements liés à une capacité : c'est l'arme telle que configurée qui décide, pas un tag figé. Donner "parry"
     * à une arme dans l'éditeur lui ouvre Reflective et Riposte.
     *
     * @return vide si l'enchantement ne dépend d'aucune capacité (le tag de l'enchantement décide alors)
     */
    public static Optional<Boolean> supportedByAbility(WeaponDefinition definition, Holder<Enchantment> enchantment) {
        RulesDefinition rules = WeaponData.rules();
        boolean parries = definition.ability() == WeaponAbility.PARRY;
        if (enchantment.is(REFLECTIVE)) return Optional.of(parries && rules.parryActive());
        if (enchantment.is(RIPOSTE)) return Optional.of(parries && rules.perfectParryActive());
        if (enchantment.is(MOMENTUM)) return Optional.of(definition.ability() == WeaponAbility.DASH && rules.dashActive());
        return Optional.empty();
    }

    /** Soin de Vampirisme : proportionnel sur les petits coups, puis logarithmique pour ne pas rendre un joueur intuable. */
    public static double vampirismHeal(double damage, int level) {
        return VAMPIRISM_CURVE * Math.log1p(damage * level * VAMPIRISM_PER_LEVEL / VAMPIRISM_CURVE);
    }

    public static double momentumMultiplier(int level) {
        return 1.0D - level * MOMENTUM_PER_LEVEL;
    }

    /** L'attaquant s'il a frappé lui-même au corps à corps : ni projectile, ni dégâts renvoyés. */
    @Nullable
    public static LivingEntity meleeAttacker(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && !source.is(DamageTypes.THORNS) ? attacker : null;
    }

    /**
     * Reflective : renvoie une part du coup à l'attaquant, avec les propriétés de son attaque (effets de son arme,
     * Aura de feu, poison naturel) et du recul. Passe par des dégâts d'épines, qui ne peuvent pas être renvoyés à leur tour.
     *
     * @param guard objet qui a paré (arme ou bouclier)
     */
    public static void reflect(Player defender, ItemStack guard, DamageSource source, float damage) {
        LivingEntity attacker = meleeAttacker(source);
        if (attacker == null || !(defender.level() instanceof ServerLevel world)) return;
        int enchantmentLevel = level(world, guard, REFLECTIVE);
        if (enchantmentLevel <= 0) return;

        ItemStack attackerWeapon = attacker.getMainHandItem();
        DamageSource reflected = defender.damageSources().thorns(defender);
        attacker.hurt(reflected, (float) (damage * enchantmentLevel * REFLECT_PER_LEVEL));
        WeaponItem.applyOnHitEffects(attackerWeapon, attacker, defender);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(world, attacker, reflected, attackerWeapon);
        applyNaturalEffect(attacker, defender);
        pushBack(attacker, defender, REFLECT_KNOCKBACK + attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK));

        trail(world, ParticleTypes.END_ROD, defender.getEyePosition(), attacker.getEyePosition());
        world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.3F);
    }

    /** Les mobs dont la morsure ou le coup empoisonne reçoivent leur propre effet. */
    private static void applyNaturalEffect(LivingEntity attacker, LivingEntity defender) {
        if (attacker instanceof CaveSpider) attacker.addEffect(new MobEffectInstance(MobEffects.POISON, REFLECT_POISON_TICKS), defender);
        else if (attacker instanceof WitherSkeleton) attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, REFLECT_WITHER_TICKS), defender);
        else if (attacker instanceof Husk) attacker.addEffect(new MobEffectInstance(MobEffects.HUNGER, REFLECT_HUNGER_TICKS), defender);
    }

    /** Pousse {@code target} à l'opposé de {@code from}. */
    public static void pushBack(LivingEntity target, LivingEntity from, double strength) {
        target.knockback(strength, from.getX() - target.getX(), from.getZ() - target.getZ());
        // Le mouvement d'un joueur est géré par son client : il faut le prévenir.
        if (target instanceof ServerPlayer) target.hurtMarked = true;
    }

    /** Traînée de particules entre deux points (ex : la vie qui revient vers le joueur). */
    public static void trail(ServerLevel world, ParticleOptions particle, Vec3 from, Vec3 to) {
        for (int i = 0; i <= TRAIL_POINTS; i++) {
            Vec3 point = from.lerp(to, (double) i / TRAIL_POINTS);
            world.sendParticles(particle, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}
