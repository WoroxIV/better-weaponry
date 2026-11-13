package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModTags;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Les enchantements du mod : coût, niveaux et objets acceptés sont dans {@link #init()},
 * leurs effets sont ici et dans {@link EnchantmentEvents}. Reflective, Riposte et Élan appartiennent à Combat Ascended.
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

    private static final EquipmentSlot[] MAIN_HAND = {EquipmentSlot.MAINHAND};
    private static final ModEnchantment.Costs COMMON_COSTS = new ModEnchantment.Costs(12, 11, 62, 11, 3, false);
    private static final ModEnchantment.Costs TREASURE_COSTS = new ModEnchantment.Costs(25, 25, 75, 25, 3, true);
    private static final ModEnchantment.Costs RETURNING_COSTS = new ModEnchantment.Costs(20, 0, 50, 0, 1, true);

    private ModEnchantments() {
    }

    /** Force la déclaration des enchantements. */
    public static void init() {
        Predicate<ItemStack> sharpWeapons = stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
        register(VAMPIRISM, Enchantment.Rarity.VERY_RARE, MAIN_HAND, TREASURE_COSTS, sharpWeapons, Set.of(BLEEDING.location()));
        register(EXECUTION, Enchantment.Rarity.RARE, MAIN_HAND, COMMON_COSTS, sharpWeapons, Set.of());
        register(BLEEDING, Enchantment.Rarity.RARE, MAIN_HAND, COMMON_COSTS, stack -> stack.is(ModTags.ENCHANTABLE_BLEEDING),
                Set.of(new ResourceLocation("fire_aspect"), VAMPIRISM.location()));
        register(RETURNING, Enchantment.Rarity.RARE, MAIN_HAND, RETURNING_COSTS, stack -> stack.is(ModTags.SHURIKENS), Set.of(new ResourceLocation("mending")));
    }

    private static void register(ResourceKey<Enchantment> key, Enchantment.Rarity rarity, EquipmentSlot[] slots, ModEnchantment.Costs costs,
                                 Predicate<ItemStack> accepted, Set<ResourceLocation> exclusiveWith) {
        Platform.get().register(Registries.ENCHANTMENT, key.location().getPath(), () -> new ModEnchantment(rarity, slots, costs, accepted, exclusiveWith));
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, new ResourceLocation(BetterWeaponry.MODID, path));
    }

    /** Niveau de cet enchantement sur l'objet, 0 s'il ne l'a pas. */
    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        Enchantment value = BuiltInRegistries.ENCHANTMENT.get(enchantment);
        return value == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(value, stack);
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
