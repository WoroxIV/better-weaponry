package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Les enchantements du mod. Ils sont déclarés dans data/better_weaponry/enchantment/ (coût, niveaux, objets acceptés)
 * et leurs effets sont ici et dans {@link EnchantmentEvents}. Reflective, Riposte et Élan restent déclarés, sans être
 * obtenables : ils appartiennent à Combat Ascended, et les objets qui les portent doivent rester intacts sans ce mod.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> VAMPIRISM = key("vampirism");
    public static final ResourceKey<Enchantment> EXECUTION = key("execution");
    public static final ResourceKey<Enchantment> BLEEDING = key("bleeding");
    public static final ResourceKey<Enchantment> RETURNING = key("returning");

    public static final List<ResourceKey<Enchantment>> ALL = List.of(VAMPIRISM, EXECUTION, BLEEDING, RETURNING);

    private static final double VAMPIRISM_PER_LEVEL = 0.1D;
    /** Plus la valeur est petite, plus le soin plafonne vite sur les gros coups. */
    private static final double VAMPIRISM_CURVE = 2.0D;
    public static final double EXECUTION_PER_LEVEL = 0.1D;
    public static final double EXECUTION_HEALTH_THRESHOLD = 0.3D;
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

    /** Soin de Vampirisme : proportionnel sur les petits coups, puis logarithmique pour ne pas rendre un joueur intuable. */
    public static double vampirismHeal(double damage, int level) {
        return VAMPIRISM_CURVE * Math.log1p(damage * level * VAMPIRISM_PER_LEVEL / VAMPIRISM_CURVE);
    }

    /** L'attaquant s'il a frappé lui-même au corps à corps : ni projectile, ni dégâts renvoyés. */
    @Nullable
    public static LivingEntity meleeAttacker(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && !source.is(DamageTypes.THORNS) ? attacker : null;
    }

    /** Traînée de particules entre deux points (ex : la vie qui revient vers le joueur). */
    public static void trail(ServerLevel world, ParticleOptions particle, Vec3 from, Vec3 to) {
        for (int i = 0; i <= TRAIL_POINTS; i++) {
            Vec3 point = from.lerp(to, (double) i / TRAIL_POINTS);
            world.sendParticles(particle, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}
