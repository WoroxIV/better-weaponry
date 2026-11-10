package com.betterweaponry.gametest;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ModEnchantments;
import com.betterweaponry.combat.MobThrowShurikenGoal;
import com.betterweaponry.compat.RecipeViewerInfo;
import com.betterweaponry.data.DataProblem;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.MobGearDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.entity.ShurikenEntity;
import com.betterweaponry.event.VanillaGearEvents;
import com.betterweaponry.item.ModShieldDecorationRecipe;
import com.betterweaponry.item.RangedWeapon;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.item.VanillaGear;
import com.betterweaponry.item.WeaponBowItem;
import com.betterweaponry.item.WeaponCrossbowItem;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponShieldItem;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModCreativeTabs;
import com.betterweaponry.registry.ModStats;
import com.betterweaponry.registry.ModEffects;
import com.betterweaponry.registry.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.locale.Language;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Tests automatiques du combat, lancés par {@code gradlew runGameTestServer} (ou {@code /test runall} en jeu, en dev).
 * Ils lisent les stats actuelles des armes : ils restent valables si les valeurs par défaut changent.
 * Ils ne tournent jamais dans une partie normale (les GameTests ne sont actifs qu'en développement).
 */
//@LOADER_TEST_ANNOTATIONS@
public final class CombatGameTests {
    /** Structure vide (data/better_weaponry/structure/empty.nbt) qui sert de terrain. */
    private static final String ARENA = "@ARENA@";
    private static final double EPSILON = 0.01D;
    private static final float TEST_HEALTH = 100.0F;
    private static final float RAW_DAMAGE = 10.0F;
    private static final int VANILLA_INVULNERABILITY_TICKS = 20;

    public CombatGameTests() {
    }

    /** Durée réelle d'une recharge : le coup est à pleine force dès que le compteur de ticks + 0.5 atteint la recharge. */
    private static int attackTicks(double delay) {
        return (int) Math.ceil(delay - 0.5D);
    }

    /** Mobs : quelle que soit l'arme du mod, le coup vaut celui d'une épée du même matériau, dans chaque main. */
    @GameTest(template = ARENA)
    public static void mobWeaponsHitLikeSwords(GameTestHelper helper) {
        Zombie hammer = spawnWith(helper, EntityType.ZOMBIE, weapon(WeaponType.HAMMER, WeaponMaterials.NETHERITE));
        Zombie sword = spawnWith(helper, EntityType.ZOMBIE, new ItemStack(Items.NETHERITE_SWORD));
        Zombie pair = spawnWith(helper, EntityType.ZOMBIE, weapon(WeaponType.DAGGER, WeaponMaterials.NETHERITE));
        pair.setItemSlot(EquipmentSlot.OFFHAND, weapon(WeaponType.CHAKRAM, WeaponMaterials.NETHERITE));
        // Les bonus d'arme ne sont appliqués qu'au tick suivant.
        helper.runAfterDelay(2, () -> {
            Pig pig = pigAt(helper, new Vec3(1.5D, 1.0D, 1.5D));
            sword.doHurtTarget(pig);
            double reference = TEST_HEALTH - pig.getHealth();
            helper.assertTrue(reference > 0.0D, "the sword zombie should hit");

            for (Zombie zombie : List.of(hammer, pair, pair)) {
                heal(pig);
                zombie.doHurtTarget(pig);
                double dealt = TEST_HEALTH - pig.getHealth();
                helper.assertTrue(Math.abs(dealt - reference) < EPSILON, "a mod weapon should hit like a sword: " + dealt + " instead of " + reference);
            }
            helper.assertFalse(hammer.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().stream().anyMatch(modifier -> modifier.id().getPath().equals("mob_sword_damage")),
                    "the correction should be removed after the hit");
            helper.succeed();
        });
    }

    /** Joueur factice à 100 PV, regardant vers le sud (+Z), qui tient cette arme. */
    private static Player parrier(GameTestHelper helper, ItemStack stack, Vec3 position) {
        Player defender = player(helper, stack, ItemStack.EMPTY);
        defender.setPos(position);
        defender.setYRot(0.0F);
        defender.setYHeadRot(0.0F);
        setBase(defender.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        defender.setHealth(TEST_HEALTH);
        return defender;
    }

    private static void startParry(GameTestHelper helper, Player player, ItemStack stack) {
        helper.assertTrue(stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "the parry should start");
    }

    private static List<String> texts(List<Component> tooltip) {
        return tooltip.stream().map(Component::getString).toList();
    }

    /** Boucliers : durabilité (bois et or relevés, les autres comme leurs outils), réparation avec le matériau. */
    @GameTest(template = ARENA)
    public static void shieldDurability(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(ModItems.shield(WeaponMaterials.WOODEN).get()).getMaxDamage() == 118, "wooden shield durability");
        helper.assertTrue(new ItemStack(ModItems.shield(WeaponMaterials.GOLDEN).get()).getMaxDamage() == 64, "golden shield durability");
        helper.assertTrue(new ItemStack(ModItems.shield(WeaponMaterials.IRON).get()).getMaxDamage() == 250, "iron shield durability");
        helper.assertTrue(new ItemStack(ModItems.shield(WeaponMaterials.NETHERITE).get()).getMaxDamage() == 2031, "netherite shield durability");
        ItemStack iron = new ItemStack(ModItems.shield(WeaponMaterials.IRON).get());
        helper.assertTrue(iron.getItem().isValidRepairItem(iron, new ItemStack(Items.IRON_INGOT)), "an iron shield is repaired with iron");
        helper.assertFalse(iron.getItem().isValidRepairItem(iron, new ItemStack(Items.OAK_PLANKS)), "not with planks");
        helper.assertTrue(iron.is(WeaponMaterial.itemTag("c:tools/shield")) && iron.is(ItemTags.DURABILITY_ENCHANTABLE), "shield tags should be loaded");
        helper.succeed();
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    /** Onglet créatif : boucliers, mêlée de la plus courte à la plus longue, arcs, arbalètes, shurikens ; plus rien dans l'onglet Combat. */
    @GameTest(template = ARENA)
    public static void creativeTabOrder(GameTestHelper helper) {
        List<ItemStack> order = ModCreativeTabs.displayOrder();
        int rank = 0;
        double reach = 0.0D;
        for (ItemStack stack : order) {
            int current = stack.getItem() instanceof WeaponShieldItem ? 0 : stack.getItem() instanceof WeaponItem ? 1 : stack.getItem() instanceof WeaponBowItem ? 2
                    : stack.getItem() instanceof WeaponCrossbowItem ? 3 : stack.getItem() instanceof ShurikenItem ? 4 : -1;
            helper.assertTrue(current >= rank, "wrong group order at " + stack);
            rank = current;
            if (stack.getItem() instanceof WeaponItem weapon) {
                helper.assertTrue(weapon.definition().attackReach() >= reach, "melee weapons should be sorted by reach at " + stack);
                reach = weapon.definition().attackReach();
            }
        }
        helper.assertTrue(rank == 4, "the tab should end with the shurikens");
        List<WeaponMaterial> vanillaOrder = List.of(WeaponMaterials.WOODEN, WeaponMaterials.STONE, WeaponMaterials.IRON, WeaponMaterials.GOLDEN, WeaponMaterials.DIAMOND, WeaponMaterials.NETHERITE);
        for (WeaponType type : WeaponType.values()) {
            List<WeaponMaterial> materials = order.stream().filter(stack -> stack.getItem() instanceof WeaponItem weapon && weapon.getType() == type)
                    .map(stack -> ((WeaponItem) stack.getItem()).getMaterial()).toList();
            helper.assertTrue(materials.subList(0, vanillaOrder.size()).equals(vanillaOrder), type + " materials should follow the vanilla order, found " + materials);
        }
        helper.assertTrue(order.size() == modItemCount(), "every item of the mod should be in the tab: " + order.size() + " of " + modItemCount());
        helper.succeed();
    }

    private static int statValue(ServerPlayer player, ResourceLocation stat) {
        return player.getStats().getValue(Stats.CUSTOM.get(stat));
    }

    /** Shuriken : −1 au lancer, +1 au ramassage, le dernier point recrée l'objet, un objet rangé ailleurs ne se copie pas. */
    @GameTest(template = ARENA)
    public static void shurikenDurability(GameTestHelper helper) {
        int[] expected = {19, 43, 83, 10, 520, 677};
        WeaponMaterial[] materials = {WeaponMaterials.WOODEN, WeaponMaterials.STONE, WeaponMaterials.IRON, WeaponMaterials.GOLDEN, WeaponMaterials.DIAMOND, WeaponMaterials.NETHERITE};
        for (int i = 0; i < materials.length; i++) {
            helper.assertTrue(new ItemStack(ModItems.shuriken(materials[i]).get()).getMaxDamage() == expected[i], materials[i] + " shuriken durability");
        }

        ShurikenItem iron = ModItems.shuriken(WeaponMaterials.IRON).get();
        ItemStack stack = new ItemStack(iron);
        Player player = thrower(helper, stack, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(stack.getDamageValue() == 1, "a throw should cost 1 durability");
        ShurikenEntity shot = thrownBy(helper, player).getFirst();
        helper.assertTrue(shot.pickup == AbstractArrow.Pickup.ALLOWED, "a paid throw can be picked up");
        helper.assertFalse(shot.tryRecover(thrower(helper, ItemStack.EMPTY, GameType.SURVIVAL)), "only the thrower can pick it up");
        helper.assertTrue(shot.tryRecover(player) && stack.getDamageValue() == 0, "picking it up should give the point back");
        shot.discard();

        // Le dernier point : l'objet disparaît, le ramassage le recrée avec ses enchantements.
        ItemStack last = new ItemStack(iron);
        last.setDamageValue(last.getMaxDamage() - 1);
        last.enchant(enchantment(helper, Enchantments.POWER), 3);
        Player lastPlayer = thrower(helper, last, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), lastPlayer, InteractionHand.MAIN_HAND, last);
        helper.assertTrue(lastPlayer.getMainHandItem().isEmpty(), "the last point should break the item");
        ShurikenEntity lastShot = thrownBy(helper, lastPlayer).getFirst();
        helper.assertTrue(lastShot.tryRecover(lastPlayer), "the last shuriken should be recoverable");
        ItemStack recreated = lastPlayer.getInventory().getItem(firstShuriken(lastPlayer));
        helper.assertTrue(recreated.getItem() == iron && recreated.getDamageValue() == recreated.getMaxDamage() - 1
                && EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.POWER), recreated) == 3, "the item should come back with 1 durability and its enchantments");
        lastShot.discard();

        // Objet rangé ailleurs : ramasser ne le copie pas.
        ItemStack stored = new ItemStack(iron);
        Player storing = thrower(helper, stored, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), storing, InteractionHand.MAIN_HAND, stored);
        storing.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ShurikenEntity storedShot = thrownBy(helper, storing).getFirst();
        helper.assertFalse(storedShot.tryRecover(storing), "a shuriken whose item was stored elsewhere must not duplicate it");
        helper.assertTrue(firstShuriken(storing) < 0, "nothing should have been created");
        storedShot.discard();

        // Solidité : un lancer gratuit ne se récupère pas, chaque point payé si.
        ItemStack sturdy = new ItemStack(iron);
        sturdy.enchant(enchantment(helper, Enchantments.UNBREAKING), 3);
        Player sturdyPlayer = thrower(helper, sturdy, GameType.SURVIVAL);
        int recoverable = 0;
        int throwsDone = 40;
        for (int i = 0; i < throwsDone; i++) {
            iron.throwFrom(helper.getLevel(), sturdyPlayer, InteractionHand.MAIN_HAND, sturdy);
            for (ShurikenEntity entity : thrownBy(helper, sturdyPlayer)) {
                if (entity.pickup == AbstractArrow.Pickup.ALLOWED) recoverable++;
                entity.discard();
            }
        }
        helper.assertTrue(recoverable == sturdy.getDamageValue() && recoverable < throwsDone, "only paid throws are recoverable: " + recoverable + " for " + sturdy.getDamageValue() + " durability");

        // Mode créatif : ne s'use pas. Il faut un vrai joueur serveur : le jeu de base ne vérifie le mode créatif que pour eux.
        ItemStack creative = new ItemStack(iron);
        ServerPlayer creativePlayer = helper.makeMockServerPlayerInLevel();
        try {
            creativePlayer.setGameMode(GameType.CREATIVE);
            creativePlayer.setPos(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
            creativePlayer.setItemInHand(InteractionHand.MAIN_HAND, creative);
            iron.throwFrom(helper.getLevel(), creativePlayer, InteractionHand.MAIN_HAND, creative);
            ShurikenEntity creativeShot = thrownBy(helper, creativePlayer).getFirst();
            helper.assertTrue(creative.getDamageValue() == 0 && creativeShot.pickup == AbstractArrow.Pickup.CREATIVE_ONLY, "creative throws cost nothing");
            creativeShot.discard();
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(creativePlayer);
        }
        helper.succeed();
    }

    /** Enchantements : ceux des arcs et arbalètes marchent (type flèche), Infinité et Charge rapide refusés, Retour incompatible avec Raccommodage. */
    @GameTest(template = ARENA)
    public static void shurikenEnchantments(GameTestHelper helper) {
        ShurikenItem iron = ModItems.shuriken(WeaponMaterials.IRON).get();
        ItemStack probe = new ItemStack(iron);
        for (ResourceKey<Enchantment> accepted : List.of(Enchantments.POWER, Enchantments.PUNCH, Enchantments.FLAME, Enchantments.PIERCING, Enchantments.MULTISHOT,
                Enchantments.UNBREAKING, Enchantments.MENDING, ModEnchantments.RETURNING)) {
            helper.assertTrue(supports(probe, enchantment(helper, accepted)), "a shuriken should accept " + accepted.location());
        }
        for (ResourceKey<Enchantment> refused : List.of(Enchantments.INFINITY, Enchantments.QUICK_CHARGE)) {
            helper.assertFalse(supports(probe, enchantment(helper, refused)), "a shuriken should refuse " + refused.location());
        }
        helper.assertFalse(Enchantment.areCompatible(enchantment(helper, ModEnchantments.RETURNING), enchantment(helper, Enchantments.MENDING)), "Returning and Mending are incompatible");
        helper.assertFalse(Enchantment.areCompatible(enchantment(helper, Enchantments.PIERCING), enchantment(helper, Enchantments.MULTISHOT)), "Piercing or Multishot, not both");

        ItemStack powered = new ItemStack(iron);
        powered.enchant(enchantment(helper, Enchantments.POWER), 5);
        Player player = thrower(helper, powered, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), player, InteractionHand.MAIN_HAND, powered);
        ShurikenEntity shot = thrownBy(helper, player).getFirst();
        helper.assertTrue(shot.getType().is(EntityTypeTags.ARROWS), "a shuriken should count as an arrow for enchantments");
        Pig pig = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));
        float boosted = EnchantmentHelper.modifyDamage(helper.getLevel(), shot.getWeaponItem(), pig, helper.getLevel().damageSources().arrow(shot, player), (float) shot.getBaseDamage());
        helper.assertTrue(boosted > shot.getBaseDamage() + 1.0F, "Power should raise the damage, got " + boosted);
        shot.discard();

        ItemStack piercing = new ItemStack(iron);
        piercing.enchant(enchantment(helper, Enchantments.PIERCING), 3);
        Player piercer = thrower(helper, piercing, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), piercer, InteractionHand.MAIN_HAND, piercing);
        ShurikenEntity piercingShot = thrownBy(helper, piercer).getFirst();
        helper.assertTrue(piercingShot.getPierceLevel() == 3, "Piercing should apply");
        piercingShot.discard();

        ItemStack flaming = new ItemStack(iron);
        flaming.enchant(enchantment(helper, Enchantments.FLAME), 1);
        Player igniter = thrower(helper, flaming, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), igniter, InteractionHand.MAIN_HAND, flaming);
        ShurikenEntity flamingShot = thrownBy(helper, igniter).getFirst();
        helper.assertTrue(flamingShot.isOnFire(), "Flame should ignite the shuriken");
        flamingShot.discard();

        // Tir multiple : trois projectiles, un seul point payé, un seul récupérable.
        ItemStack multi = new ItemStack(iron);
        multi.enchant(enchantment(helper, Enchantments.MULTISHOT), 1);
        Player multiPlayer = thrower(helper, multi, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), multiPlayer, InteractionHand.MAIN_HAND, multi);
        List<ShurikenEntity> volley = thrownBy(helper, multiPlayer);
        helper.assertTrue(volley.size() == 3 && multi.getDamageValue() == 1, "Multishot throws 3 for 1 durability, found " + volley.size());
        helper.assertTrue(volley.stream().filter(entity -> entity.pickup == AbstractArrow.Pickup.ALLOWED).count() == 1, "only one of the volley can be picked up");
        volley.forEach(ShurikenEntity::discard);
        helper.succeed();
    }

    /** Un shuriken qui touche une cible reste en jeu et tombe au sol, au lieu de disparaître comme une flèche. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void shurikenFallsAfterHit(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.shuriken(WeaponMaterials.IRON).get());
        Player player = thrower(helper, stack, GameType.SURVIVAL);
        Pig pig = pigAt(helper, new Vec3(2.5D, 1.2D, 4.5D));
        ShurikenEntity shot = new ShurikenEntity(helper.getLevel(), player, stack, true);
        Vec3 start = helper.absoluteVec(new Vec3(2.5D, 1.5D, 1.5D));
        shot.setPos(start.x, start.y, start.z);
        shot.shoot(0.0D, 0.0D, 1.0D, 1.5F, 0.0F);
        helper.getLevel().addFreshEntity(shot);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < TEST_HEALTH, "the shuriken should hurt the pig");
            helper.assertFalse(shot.isRemoved(), "the shuriken should stay in the world after a hit");
            helper.assertTrue(shot.isStuck(), "the shuriken should land");
            pig.discard();
            shot.discard();
        });
    }

    /** Retour : planté, il repart vers son lanceur. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void shurikenReturns(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.shuriken(WeaponMaterials.IRON).get());
        stack.enchant(enchantment(helper, ModEnchantments.RETURNING), 1);
        Player player = thrower(helper, stack, GameType.SURVIVAL);
        ShurikenEntity shot = new ShurikenEntity(helper.getLevel(), player, stack, true);
        helper.assertTrue(shot.isReturning(), "Returning should be read from the item");
        Vec3 start = helper.absoluteVec(new Vec3(2.5D, 1.5D, 3.5D));
        shot.setPos(start.x, start.y, start.z);
        shot.shoot(0.0D, -1.0D, 0.0D, 0.5F, 0.0F);
        helper.getLevel().addFreshEntity(shot);
        Player stranger = thrower(helper, ItemStack.EMPTY, GameType.SURVIVAL);
        helper.succeedWhen(() -> {
            helper.assertTrue(shot.isNoPhysics(), "a landed Returning shuriken should fly back");
            helper.assertFalse(shot.tryRecover(stranger), "another player must not catch it");
            shot.discard();
        });
    }

    private static <T extends Mob> T spawnWith(GameTestHelper helper, EntityType<T> type, ItemStack mainHand) {
        T mob = type.create(helper.getLevel());
        mob.setPos(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static Player thrower(GameTestHelper helper, ItemStack stack, GameType mode) {
        Player player = helper.makeMockPlayer(mode);
        player.setPos(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static List<ShurikenEntity> thrownBy(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(ShurikenEntity.class, player.getBoundingBox().inflate(6.0D), entity -> entity.getOwner() == player);
    }

    private static int firstShuriken(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof ShurikenItem) return slot;
        }
        return -1;
    }

    /** Arcs et arbalètes : durabilité, tags, recettes (bâtons pour le bois, matériau sinon), distributeur, chargement de l'or. */
    @GameTest(template = ARENA)
    public static void rangedWeaponsSetup(GameTestHelper helper) {
        int[][] expected = {{384, 465}, {384, 465}, {384, 465}, {49, 60}, {2398, 2903}, {3120, 3778}};
        WeaponMaterial[] materials = {WeaponMaterials.WOODEN, WeaponMaterials.STONE, WeaponMaterials.IRON, WeaponMaterials.GOLDEN, WeaponMaterials.DIAMOND, WeaponMaterials.NETHERITE};
        for (int i = 0; i < materials.length; i++) {
            helper.assertTrue(new ItemStack(ModItems.bow(materials[i]).get()).getMaxDamage() == expected[i][0], materials[i] + " bow durability");
            helper.assertTrue(new ItemStack(ModItems.crossbow(materials[i]).get()).getMaxDamage() == expected[i][1], materials[i] + " crossbow durability");
        }
        ItemStack iron = new ItemStack(ModItems.bow(WeaponMaterials.IRON).get());
        helper.assertTrue(iron.is(WeaponMaterial.itemTag("c:tools/bow")) && iron.is(ItemTags.BOW_ENCHANTABLE) && iron.is(ItemTags.DURABILITY_ENCHANTABLE), "bow tags should be loaded");
        helper.assertTrue(iron.getItem().isValidRepairItem(iron, new ItemStack(Items.IRON_INGOT)), "an iron bow is repaired with iron");

        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (String vanilla : List.of("bow", "crossbow")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.withDefaultNamespace(vanilla)).isEmpty(), "the vanilla recipe should be off: " + vanilla);
        }
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "netherite_bow_smithing")).isPresent(), "netherite bows use the smithing table");
        expectCrafted(helper, Items.STICK, ModItems.bow(WeaponMaterials.WOODEN).get());
        expectCrafted(helper, Items.IRON_INGOT, ModItems.bow(WeaponMaterials.IRON).get());
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        CraftingInput dispenser = CraftingInput.of(3, 3, List.of(cobble, cobble, cobble, cobble, new ItemStack(ModItems.bow(WeaponMaterials.IRON).get()), cobble, cobble, new ItemStack(Items.REDSTONE), cobble));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, dispenser, helper.getLevel()).isPresent(), "a dispenser should accept a bow of the mod");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int wooden = CrossbowItem.getChargeDuration(new ItemStack(ModItems.crossbow(WeaponMaterials.WOODEN).get()), player);
        int golden = CrossbowItem.getChargeDuration(new ItemStack(ModItems.crossbow(WeaponMaterials.GOLDEN).get()), player);
        helper.assertTrue(wooden == 25 && golden == 13, "charge time: wooden " + wooden + ", golden " + golden);
        helper.succeed();
    }

    private static void expectCrafted(GameTestHelper helper, Item side, Item result) {
        ItemStack s = new ItemStack(side);
        ItemStack string = new ItemStack(Items.STRING);
        CraftingInput grid = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, s, string, s, ItemStack.EMPTY, string, ItemStack.EMPTY, s, string));
        var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(recipe.isPresent() && recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(result), "the bow recipe should give " + result);
    }

    private static double averageMobShot(GameTestHelper helper, Skeleton skeleton, LivingEntity target) {
        int shots = 20;
        double total = 0.0D;
        for (int i = 0; i < shots; i++) {
            skeleton.performRangedAttack(target, 1.0F);
            List<AbstractArrow> arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, skeleton.getBoundingBox().inflate(3.0D), arrow -> arrow.getOwner() == skeleton);
            helper.assertTrue(arrows.size() == 1, "one mob arrow expected, found " + arrows.size());
            total += arrows.getFirst().getDeltaMovement().length();
            arrows.getFirst().discard();
        }
        return total / shots;
    }

    /** Seule la vitesse de la flèche dépend du matériau, en tirant vraiment ; l'or atteint la pleine puissance deux fois plus vite. */
    @GameTest(template = ARENA)
    public static void rangedProjectileSpeed(GameTestHelper helper) {
        GearDefinition gear = WeaponData.gear();
        helper.assertTrue(Math.abs(firedSpeed(helper, WeaponMaterials.STONE, 25) - 3.0D) < 0.3D, "a stone bow shoots like the vanilla bow");
        double iron = gear.projectileSpeedMultiplier(WeaponMaterials.IRON);
        helper.assertTrue(Math.abs(firedSpeed(helper, WeaponMaterials.IRON, 25) - 3.0D * iron) < 0.3D, "an iron bow should shoot faster than stone");
        // Dégâts d'une flèche à pleine charge, sans critique : vitesse (3) × multiplicateur × dégâts de base (2), arrondi au-dessus.
        Map<WeaponMaterial, Integer> fullDraw = Map.of(WeaponMaterials.WOODEN, 4, WeaponMaterials.GOLDEN, 3, WeaponMaterials.STONE, 6,
                WeaponMaterials.IRON, 7, WeaponMaterials.DIAMOND, 9, WeaponMaterials.NETHERITE, 10);
        fullDraw.forEach((material, damage) -> helper.assertTrue(Mth.ceil(3.0D * gear.projectileSpeedMultiplier(material) * 2.0D) == damage, material + " bow damage"));
        helper.assertTrue(firedSpeed(helper, WeaponMaterials.GOLDEN, 10) > 1.1D, "a golden bow should reach full power in half the time");
        helper.assertTrue(firedSpeed(helper, WeaponMaterials.STONE, 10) < 1.6D, "a stone bow drawn for 10 ticks is weak");
        helper.succeed();
    }

    private static double firedSpeed(GameTestHelper helper, WeaponMaterial material, int ticksDrawn) {
        ItemStack bow = new ItemStack(ModItems.bow(material).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.getInventory().add(new ItemStack(Items.ARROW, 8));
        bow.releaseUsing(helper.getLevel(), player, bow.getUseDuration(player) - ticksDrawn);
        AABB around = player.getBoundingBox().inflate(4.0D);
        List<AbstractArrow> arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, around, arrow -> arrow.getOwner() == player);
        helper.assertTrue(arrows.size() == 1, "one arrow should be shot, found " + arrows.size());
        double speed = arrows.getFirst().getDeltaMovement().length();
        arrows.getFirst().discard();
        return speed;
    }

    private static void expectDash(GameTestHelper helper, WeaponType type, ItemStack offhand, boolean onGround, boolean expected,
                                   String situation) {
        ItemStack main = weapon(type);
        Player player = dasher(helper, main, offhand.copy(), onGround);
        helper.assertTrue(useMainHand(helper, player, main) == expected,
                type.id() + " " + situation + ": the dash should " + (expected ? "" : "not ") + "start");
    }

    /** Joueur factice placé dans l'arène (pour que "au sol" ait un sens), avec ces objets en main. */
    private static Player dasher(GameTestHelper helper, ItemStack mainHand, ItemStack offHand, boolean onGround) {
        Player player = player(helper, mainHand, offHand);
        player.setPos(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        player.setOnGround(onGround);
        return player;
    }

    private static boolean useMainHand(GameTestHelper helper, Player player, ItemStack stack) {
        return stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction();
    }

    private static void assertDirection(GameTestHelper helper, Vec3 actual, Vec3 expected, String situation) {
        helper.assertTrue(actual.distanceTo(expected) < EPSILON, "dash direction (" + situation + "): " + actual + " instead of " + expected);
    }

    private static void expectParry(GameTestHelper helper, WeaponType type, ItemStack offhand, boolean expected) {
        ItemStack main = weapon(type);
        Player player = player(helper, main, offhand.copy());
        boolean started = main.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction();
        String offhandName = offhand.isEmpty() ? "an empty off hand" : offhand.getItem().toString();
        helper.assertTrue(started == expected && player.isUsingItem() == expected,
                type.id() + " with " + offhandName + ": the parry should " + (expected ? "" : "not ") + "start");
    }

    // --- Corrections avant publication ---

    /** Un shuriken Retour cassé par son dernier lancer revient vers un inventaire plein : il tombe aux pieds du joueur au lieu d'être perdu. */
    @GameTest(template = ARENA)
    public static void returningShurikenIsNeverLost(GameTestHelper helper) {
        ShurikenItem iron = ModItems.shuriken(WeaponMaterials.IRON).get();
        ItemStack last = new ItemStack(iron);
        last.setDamageValue(last.getMaxDamage() - 1);
        Player player = thrower(helper, last, GameType.SURVIVAL);
        iron.throwFrom(helper.getLevel(), player, InteractionHand.MAIN_HAND, last);
        ShurikenEntity shot = thrownBy(helper, player).getFirst();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        }

        helper.assertFalse(shot.tryRecover(player), "lying on the ground, it should wait for a free slot");
        shot.setNoPhysics(true);
        helper.assertTrue(shot.tryRecover(player), "coming back, it should disappear");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3.0D), item -> item.getItem().is(iron));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getDamageValue() == iron.getDefaultInstance().getMaxDamage() - 1, "the shuriken should be dropped with 1 durability: " + drops);
        drops.forEach(ItemEntity::discard);
        shot.discard();
        helper.succeed();
    }

    /** Le saignement garde son rythme d'un coup de 0,5 PV par seconde, même réappliqué sans arrêt. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void bleedingKeepsItsRhythm(GameTestHelper helper) {
        Pig pig = pigAt(helper, new Vec3(2.5D, 1.0D, 2.5D));
        helper.onEachTick(() -> pig.addEffect(new MobEffectInstance(ModEffects.BLEEDING.holder(), 40)));
        helper.runAfterDelay(100L, () -> {
            double lost = TEST_HEALTH - pig.getHealth();
            helper.assertTrue(lost >= 2.0D && lost <= 3.0D, "5 seconds of bleeding should cost about 2.5 health, lost " + lost);
            helper.succeed();
        });
    }

    /** Une recette qui demande un arc, une arbalète ou un bouclier vanilla accepte les versions du mod, quel que soit le matériau. */
    @GameTest(template = ARENA)
    public static void vanillaGearIngredientsAcceptModGear(GameTestHelper helper) {
        assertModTagsLoaded(helper);
        ItemStack netheriteBow = new ItemStack(ModItems.bow(WeaponMaterials.NETHERITE).get());
        Ingredient bow = Ingredient.of(Items.BOW);
        helper.assertTrue(bow.test(new ItemStack(Items.BOW)) && bow.test(netheriteBow), "a bow ingredient should accept the vanilla bow and the bows of the mod");
        helper.assertTrue(bow.getStackingIds().contains(StackedContents.getStackingIndex(netheriteBow)), "the recipe book should see them too");
        helper.assertTrue(Ingredient.of(Items.CROSSBOW).test(new ItemStack(ModItems.crossbow(WeaponMaterials.WOODEN).get())), "crossbow");
        helper.assertTrue(Ingredient.of(Items.SHIELD).test(new ItemStack(ModItems.shield(WeaponMaterials.DIAMOND).get())), "shield");
        helper.assertFalse(bow.test(new ItemStack(ModItems.crossbow(WeaponMaterials.IRON).get())), "a crossbow is not a bow");
        helper.assertFalse(Ingredient.of(Items.STICK).test(netheriteBow), "other ingredients should not change");
        // Reçu d'un serveur : chaque objet est déjà listé, il ne faut pas rajouter le tag.
        Ingredient received = Ingredient.of(new ItemStack(Items.BOW), netheriteBow);
        helper.assertTrue(received.getItems().length == 2, "an ingredient received from a server should stay as it is");
        helper.succeed();
    }

    /** Un matériau de mod sans aucun ingrédient n'est pas proposé ; un matériau vanilla l'est toujours. */
    @GameTest(template = ARENA)
    public static void materialsNeedIngredients(GameTestHelper helper) {
        helper.assertTrue(WeaponMaterials.IRON.hasIngredients(), "vanilla materials always have ingredients");
        WeaponMaterial modded = WeaponMaterials.all().stream().filter(WeaponMaterial::isModded).findFirst().orElseThrow();
        helper.assertFalse(modded.hasIngredients(), modded.id() + " has no ingredient without its mod");
        helper.succeed();
    }

    // --- Outils ---

    private static boolean supports(ItemStack stack, Holder<Enchantment> enchantment) {
        return Platform.get().supportsEnchantment(stack, enchantment);
    }

    private static long modItemCount() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(BetterWeaponry.MODID)).count();
    }

    /**
     * Les tags du mod (générés par runData) doivent être chargés. S'ils manquent, le dossier build/resources/main
     * était incomplet au lancement : souvent l'IDE qui recompile en même temps. Relancer suffit.
     */
    private static void assertModTagsLoaded(GameTestHelper helper) {
        helper.assertTrue(weapon(WeaponType.DAGGER).is(ItemTags.SWORDS), "the mod's item tags are not loaded: build/resources/main "
                + "was incomplete when the server started (an IDE build running at the same time?). Run the tests again.");
    }

    private static Player mockPlayerAt(GameTestHelper helper, Vec3 position) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(position);
        return player;
    }

    private static JsonObject json(String text) {
        return JsonUtil.parseLenient(text).getAsJsonObject();
    }

    private static ItemStack weapon(WeaponType type) {
        return weapon(type, WeaponMaterials.IRON);
    }

    private static ItemStack weapon(WeaponType type, WeaponMaterial material) {
        return new ItemStack(ModItems.get(type, material).get());
    }

    /** Cochon immobile sans armure, 100 PV, à cette position (relative à l'arène, au bloc près). */
    private static Pig pigAt(GameTestHelper helper, Vec3 relative) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, relative);
        pig.setNoGravity(true);
        setBase(pig.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        pig.setHealth(TEST_HEALTH);
        return pig;
    }

    /** Remet un cochon à 100 PV, sans invulnérabilité : prêt pour le coup suivant. */
    private static void heal(Pig pig) {
        pig.setHealth(TEST_HEALTH);
        pig.invulnerableTime = 0;
    }

    private static boolean isHurt(Pig pig) {
        return pig.getHealth() < TEST_HEALTH;
    }

    /** Cochon immobile, 100 PV, armure de diamant complète (20 points, 8 de solidité). */
    private static Pig armoredPig(GameTestHelper helper, BlockPos position) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, position);
        pig.setNoGravity(true);
        setBase(pig.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        setBase(pig.getAttribute(Attributes.ARMOR), 20.0D);
        setBase(pig.getAttribute(Attributes.ARMOR_TOUGHNESS), 8.0D);
        pig.setHealth(TEST_HEALTH);
        return pig;
    }

    /** Joueur factice tenant ces objets, avec leurs bonus d'attributs appliqués (comme quand on les équipe). */
    /** La portée affichée est la portée réelle en blocs, en survie comme en créatif ; les objets vanilla gardent celle du jeu. */
    @GameTest(template = ARENA)
    public static void weaponReachMatchesTheTooltip(GameTestHelper helper) {
        for (WeaponType type : WeaponType.values()) {
            double shown = WeaponData.weapon(type).attackReach();
            for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                double reach = holding(helper, new ItemStack(ModItems.get(type, WeaponMaterials.IRON).get()), mode).entityInteractionRange();
                helper.assertTrue(reach == shown, type + " should reach " + shown + " blocks in " + mode + ", not " + reach);
            }
        }
        helper.assertTrue(holding(helper, new ItemStack(Items.IRON_SWORD), GameType.SURVIVAL).entityInteractionRange() == 3.0D, "a vanilla sword should keep its reach in survival");
        helper.assertTrue(holding(helper, new ItemStack(Items.IRON_SWORD), GameType.CREATIVE).entityInteractionRange() > 3.0D, "a vanilla sword should keep the reach bonus of creative mode");
        helper.succeed();
    }

    private static Player player(GameTestHelper helper, ItemStack mainHand, ItemStack offHand) {
        Player player = holding(helper, mainHand, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        return player;
    }

    private static Player holding(GameTestHelper helper, ItemStack mainHand, GameType mode) {
        Player player = helper.makeMockPlayer(mode);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        mainHand.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null && !instance.hasModifier(modifier.id())) instance.addTransientModifier(modifier);
        });
        // Sur un vrai joueur, c'est le serveur qui pose ce bonus.
        if (mode.isCreative()) {
            player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).addTransientModifier(
                    new AttributeModifier(ResourceLocation.withDefaultNamespace("creative_mode_entity_range"), 2.0D, AttributeModifier.Operation.ADD_VALUE));
        }
        return player;
    }

    private static void setBase(AttributeInstance attribute, double value) {
        if (attribute != null) attribute.setBaseValue(value);
    }
}
