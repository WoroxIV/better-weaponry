package com.betterweaponry.gametest;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.TooltipLayout;
import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.DashAbility;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.ModEnchantments;
import com.betterweaponry.combat.MobCombat;
import com.betterweaponry.combat.MobGuardGoal;
import com.betterweaponry.combat.MobThrowShurikenGoal;
import com.betterweaponry.combat.PlayerCombatState;
import com.betterweaponry.combat.ShieldSystem;
import com.betterweaponry.combat.TwoHandedSystem;
import com.betterweaponry.combat.WeaponProfiles;
import com.betterweaponry.compat.CombatCompat;
import com.betterweaponry.compat.RecipeViewerInfo;
import com.betterweaponry.config.EditorFiles;
import com.betterweaponry.config.TooltipCategory;
import com.betterweaponry.config.TooltipPlacement;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.ComboStep;
import com.betterweaponry.data.DashDefinition;
import com.betterweaponry.data.DashDirection;
import com.betterweaponry.data.DataProblem;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.MobGearDefinition;
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.RulesPreset;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponComparison;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.editor.EditorDocument;
import com.betterweaponry.entity.ShurikenEntity;
import com.betterweaponry.editor.EditorService;
import com.betterweaponry.editor.EditorTarget;
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
import com.betterweaponry.mixin.LivingEntityAccessor;
import com.betterweaponry.mixin.MobAccessor;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModAttributes;
import com.betterweaponry.tutorial.TutorialScene;
import com.betterweaponry.tutorial.TutorialScenes;
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
import com.betterweaponry.tutorial.TutorialHints;
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

    /** Écrit la photographie du comportement des armes (voir {@link ParitySnapshot}). */
    @GameTest(template = ARENA)
    public static void writeParityReference(GameTestHelper helper) {
        try {
            ParitySnapshot.write(helper.getLevel().registryAccess(), "parity-reference.json");
        } catch (java.io.IOException e) {
            helper.fail("could not write the parity reference: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    /** Fusion des couches : valeur invalide ignorée, sous-champ fusionné, fautes de frappe signalées avec suggestion. */
    @GameTest(template = ARENA)
    public static void dataLayersMerge(GameTestHelper helper) {
        JsonObject defaults = json("{ \"armor_penetration\": 10, \"sweep\": { \"enabled\": true, \"radius\": 1.0, \"damage_ratio\": 0 },"
                + " \"on_hit_effects\": [ { \"effect\": \"minecraft:slowness\" } ] }");
        JsonObject user = json("{ \"armor_penetration\": 150, \"sweep\": { \"radus\": 3, \"radius\": 2.5 },"
                + " \"on_hit_effects\": [ { \"effect\": \"minecraft:slownes\" } ], \"atack_speed\": 2, \"ability\": \"parade\" }");
        List<DataProblem> problems = new ArrayList<>();
        JsonObject merged = WeaponDefinition.FIELDS.merge(List.of(new FieldSet.Layer("defaults", defaults), new FieldSet.Layer("user", user)), problems);
        WeaponDefinition definition = WeaponDefinition.CODEC.parse(JsonOps.INSTANCE, merged).getOrThrow();

        helper.assertTrue(definition.armorPenetration() == 10.0D, "armor_penetration 150 must be ignored, got " + definition.armorPenetration());
        helper.assertTrue(definition.sweep().radius() == 2.5D && definition.sweep().enabled(), "sweep must merge field by field: " + definition.sweep());
        helper.assertTrue(definition.onHitEffects().size() == 1, "an invalid effect list must keep the previous list");
        helper.assertTrue(definition.ability() == WeaponAbility.NONE, "an unknown ability must be ignored, got " + definition.ability());
        helper.assertTrue(problems.size() == 5, "5 problems expected, got " + problems.stream().map(DataProblem::toLogString).toList());
        helper.assertTrue(problems.stream().anyMatch(p -> "sweep.radus".equals(p.field()) && "radius".equals(p.detail())),
                "the typo sweep.radus must suggest radius");
        helper.assertTrue(problems.stream().anyMatch(p -> "atack_speed".equals(p.field()) && "attack_speed".equals(p.detail())),
                "the typo atack_speed must suggest attack_speed");
        helper.succeed();
    }

    /** Avec X % de pénétration, l'armure retire X % de moins de dégâts qu'avec une arme sans pénétration. */
    @GameTest(template = ARENA)
    public static void armorPenetrationReducesArmor(GameTestHelper helper) {
        WeaponType piercing = WeaponType.HAMMER;
        for (WeaponType type : WeaponType.values()) {
            if (WeaponData.weapon(type).armorPenetration() > WeaponData.weapon(piercing).armorPenetration()) piercing = type;
        }
        double penetration = WeaponData.weapon(piercing).armorPenetration() / 100.0D;

        Pig withoutPenetration = armoredPig(helper, new BlockPos(1, 1, 1));
        Pig withPenetration = armoredPig(helper, new BlockPos(3, 1, 3));
        Player unarmed = player(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        Player armed = player(helper, weapon(piercing), ItemStack.EMPTY);
        withoutPenetration.hurt(unarmed.damageSources().playerAttack(unarmed), RAW_DAMAGE);
        withPenetration.hurt(armed.damageSources().playerAttack(armed), RAW_DAMAGE);

        double armorReduction = RAW_DAMAGE - (TEST_HEALTH - withoutPenetration.getHealth());
        double reducedArmorReduction = RAW_DAMAGE - (TEST_HEALTH - withPenetration.getHealth());
        helper.assertTrue(armorReduction > 1.0D, "the armor should absorb damage, absorbed " + armorReduction);
        boolean apothic = Platform.get().isModLoaded("apothic_attributes");
        helper.assertTrue(ModAttributes.armorPenetration().equals(ModAttributes.ARMOR_PENETRATION.holder()) != apothic, "the penetration attribute should depend on Apothic Attributes");
        double expected = armorReduction * (1.0D - penetration);
        // Avec Apothic Attributes, c'est son calcul de l'armure qui s'applique : l'armure protège seulement moins.
        helper.assertTrue(apothic ? reducedArmorReduction < armorReduction : Math.abs(reducedArmorReduction - expected) < EPSILON,
                piercing.id() + " (" + penetration * 100 + "%): armor absorbed " + reducedArmorReduction + " instead of " + expected);
        helper.succeed();
    }

    /** Chaque effet sûr (chance 100 %) est appliqué à la cible, au bon niveau. */
    @GameTest(template = ARENA)
    public static void onHitEffectsAreApplied(GameTestHelper helper) {
        int index = 0;
        for (WeaponType type : WeaponType.values()) {
            for (OnHitEffect effect : WeaponData.weapon(type).onHitEffects()) {
                if (effect.chance() < 100.0D || effect.holder().isEmpty()) continue;
                Pig target = armoredPig(helper, new BlockPos(index % 5, 1, (index / 5) % 5));
                ItemStack stack = weapon(type);
                Player attacker = player(helper, stack, ItemStack.EMPTY);
                stack.getItem().hurtEnemy(stack, target, attacker);

                MobEffectInstance applied = target.getEffect(effect.holder().get());
                helper.assertTrue(applied != null, type.id() + " should give " + effect.effect());
                helper.assertTrue(applied.getAmplifier() == effect.level() - 1, type.id() + ": wrong level for " + effect.effect());
                index++;
            }
        }
        helper.succeed();
    }

    /** En dual wield, la cible n'est protégée que le temps prévu par rules.json ; avec une seule arme, comme en vanilla. */
    @GameTest(template = ARENA)
    public static void dualWieldInvulnerability(GameTestHelper helper) {
        Player dualWielder = player(helper, weapon(WeaponType.DAGGER), weapon(WeaponType.DAGGER));
        Player singleWeapon = player(helper, weapon(WeaponType.DAGGER), ItemStack.EMPTY);
        assertModTagsLoaded(helper);
        helper.assertTrue(DualWieldSystem.isDualWielding(dualWielder), "two daggers should count as dual wielding");

        Pig dualTarget = armoredPig(helper, new BlockPos(1, 1, 1));
        Pig singleTarget = armoredPig(helper, new BlockPos(3, 1, 3));
        dualTarget.hurt(dualWielder.damageSources().playerAttack(dualWielder), 2.0F);
        singleTarget.hurt(singleWeapon.damageSources().playerAttack(singleWeapon), 2.0F);

        int expected = 10 + (int) Math.round(WeaponData.rules().dualWieldInvulnerabilitySeconds() * 20.0D);
        helper.assertTrue(dualTarget.invulnerableTime == expected, "dual wield: invulnerability " + dualTarget.invulnerableTime + " instead of " + expected);
        helper.assertTrue(singleTarget.invulnerableTime == VANILLA_INVULNERABILITY_TICKS, "single weapon: invulnerability " + singleTarget.invulnerableTime);
        helper.succeed();
    }

    /** Zone de balayage agrandie du rayon configuré, et désactivation des boucliers selon les données. */
    @GameTest(template = ARENA)
    public static void sweepZoneAndShields(GameTestHelper helper) {
        Pig target = armoredPig(helper, new BlockPos(2, 1, 2));
        Player attacker = player(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        ItemStack shield = new ItemStack(Items.SHIELD);
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            ItemStack stack = weapon(type);
            WeaponItem item = (WeaponItem) stack.getItem();

            AABB zone = item.getSweepHitBox(stack, attacker, target);
            double expectedWidth = target.getBoundingBox().getXsize() + 2.0D * definition.sweep().radius();
            helper.assertTrue(Math.abs(zone.getXsize() - expectedWidth) < EPSILON, type.id() + ": sweep zone width " + zone.getXsize());
            helper.assertTrue(item.canDisableShield(stack, shield, target, attacker) == definition.disablesShield(),
                    type.id() + ": disables_shield should be " + definition.disablesShield());
        }
        helper.succeed();
    }

    /**
     * Parade : un coup de face est réduit de damage_reduction % et use l'arme ; un coup dans le dos passe en entier.
     * Testé sur chaque arme qui pare.
     */
    @GameTest(template = ARENA)
    public static void parryBlocksFrontalHits(GameTestHelper helper) {
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            ItemStack stack = weapon(type);
            Optional<ParryDefinition> settings = ParrySystem.parryOf(stack);
            if (settings.isEmpty()) continue;
            ParryDefinition parry = settings.get();

            // Le défenseur regarde vers le sud (+Z) : devant lui = Z plus grand.
            Player defender = player(helper, stack, ItemStack.EMPTY);
            Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
            defender.setPos(position);
            defender.setYRot(0.0F);
            defender.setYHeadRot(0.0F);
            setBase(defender.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
            defender.setHealth(TEST_HEALTH);
            helper.assertTrue(stack.use(helper.getLevel(), defender, InteractionHand.MAIN_HAND).getResult().consumesAction()
                    && ParrySystem.isParrying(defender), type.id() + ": a right click should start the parry");
            PlayerCombatState.of(defender).perfectParryUntil = 0L;  // fenêtre de parade parfaite passée

            defender.hurt(defender.damageSources().playerAttack(mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D))), RAW_DAMAGE);
            double blocked = RAW_DAMAGE * parry.damageReduction() / 100.0D;
            double frontDamage = TEST_HEALTH - defender.getHealth();
            helper.assertTrue(Math.abs(frontDamage - (RAW_DAMAGE - blocked)) < EPSILON,
                    type.id() + ": a hit from the front did " + frontDamage + " damage instead of " + (RAW_DAMAGE - blocked));
            int expectedWear = (int) Math.floor(RAW_DAMAGE * parry.durabilityPerDamage());
            helper.assertTrue(stack.getDamageValue() == expectedWear,
                    type.id() + ": the parry wore the weapon by " + stack.getDamageValue() + " instead of " + expectedWear);

            defender.invulnerableTime = 0;
            defender.setHealth(TEST_HEALTH);
            defender.hurt(defender.damageSources().playerAttack(mockPlayerAt(helper, position.add(0.0D, 0.0D, -2.0D))), RAW_DAMAGE);
            double backDamage = TEST_HEALTH - defender.getHealth();
            helper.assertTrue(Math.abs(backDamage - RAW_DAMAGE) < EPSILON,
                    type.id() + ": a hit in the back did " + backDamage + " damage instead of " + RAW_DAMAGE);
            tested++;
        }
        helper.assertTrue(tested > 0, "no weapon has the parry ability");
        helper.succeed();
    }

    /** Usure à la parade : le plancher de la moitié des dégâts du coup ; partagée quand on pare avec une arme dans chaque main. */
    @GameTest(template = ARENA)
    public static void parryWear(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Player attacker = mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D));
        ItemStack main = weapon(WeaponType.SAI);
        ItemStack off = weapon(WeaponType.SAI);
        Player pair = player(helper, main, off);
        pair.setPos(position);
        pair.setYRot(0.0F);
        pair.setYHeadRot(0.0F);
        setBase(pair.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        pair.setHealth(TEST_HEALTH);
        startParry(helper, pair, main);
        PlayerCombatState.of(pair).perfectParryUntil = 0L;

        pair.hurt(pair.damageSources().playerAttack(attacker), 7.0F);
        helper.assertTrue(main.getDamageValue() == 2 && off.getDamageValue() == 1,
                "7 damage should cost 3 durability shared 2 and 1, got " + main.getDamageValue() + " and " + off.getDamageValue());
        pair.invulnerableTime = 0;
        pair.hurt(pair.damageSources().playerAttack(attacker), 1.0F);
        helper.assertTrue(main.getDamageValue() == 2 && off.getDamageValue() == 1, "a hit of 1 damage should cost nothing");

        ItemStack alone = weapon(WeaponType.SAI);
        Player single = parrier(helper, alone, position);
        startParry(helper, single, alone);
        PlayerCombatState.of(single).perfectParryUntil = 0L;
        single.hurt(single.damageSources().playerAttack(attacker), 7.0F);
        helper.assertTrue(alone.getDamageValue() == 3, "a lone weapon should take all the wear, got " + alone.getDamageValue());
        helper.succeed();
    }

    /**
     * Quand le clic droit lance une parade : arme qui pare en main principale, main secondaire vide,
     * arme à deux mains, ou arme ambidextre dans chaque main. Un bouclier ou une torche
     * en main secondaire gardent leur action ; une arme tenue en main secondaire ne pare jamais.
     */
    @GameTest(template = ARENA)
    public static void parryConditions(GameTestHelper helper) {
        assertModTagsLoaded(helper);
        ItemStack shield = new ItemStack(Items.SHIELD);
        ItemStack torch = new ItemStack(Items.TORCH);
        ItemStack dagger = weapon(WeaponType.DAGGER);
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            boolean parries = definition.ability() == WeaponAbility.PARRY && WeaponData.rules().parryEnabled();
            boolean twoHanded = definition.twoHanded();

            expectParry(helper, type, ItemStack.EMPTY, parries);
            expectParry(helper, type, shield, parries && twoHanded);
            expectParry(helper, type, torch, parries && twoHanded);
            expectParry(helper, type, dagger, parries && (twoHanded || definition.dualWielded()));
            expectParry(helper, type, weapon(type), parries && (twoHanded || definition.dualWielded()));

            // Avec une arme ambidextre dans chaque main (saïs), les deux armes prennent la pose de parade.
            ItemStack dualMain = weapon(type);
            Player dualParrier = player(helper, dualMain, dagger.copy());
            dualMain.use(helper.getLevel(), dualParrier, InteractionHand.MAIN_HAND);
            boolean bothHands = parries && !twoHanded && definition.dualWielded();
            helper.assertTrue(ParrySystem.isParryingWithBothHands(dualParrier) == bothHands,
                    type.id() + " with a dagger in the off hand: both hands should " + (bothHands ? "" : "not ") + "parry");

            ItemStack offhandWeapon = weapon(type);
            Player offhandOnly = player(helper, ItemStack.EMPTY, offhandWeapon);
            helper.assertFalse(offhandWeapon.use(helper.getLevel(), offhandOnly, InteractionHand.OFF_HAND).getResult().consumesAction(),
                    type.id() + " held in the off hand must not parry");
        }
        helper.succeed();
    }

    /**
     * Dash : un clic droit au sol lance le dash, met tous les matériaux du type en recharge et coûte de la faim ;
     * pas de second dash pendant la recharge. En l'air, avec une torche ou un bouclier en main secondaire,
     * ou presque affamé, le dash ne part pas (sauf réglages contraires).
     */
    @GameTest(template = ARENA)
    public static void dashConditionsAndCosts(GameTestHelper helper) {
        assertModTagsLoaded(helper);
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            Optional<DashDefinition> settings = DashAbility.dashOf(weapon(type));
            if (settings.isEmpty()) continue;
            DashDefinition dash = settings.get();
            boolean twoHanded = WeaponData.weapon(type).twoHanded();

            ItemStack stack = weapon(type);
            Player player = dasher(helper, stack, ItemStack.EMPTY, true);
            helper.assertTrue(useMainHand(helper, player, stack), type.id() + ": a right click on the ground should dash");
            for (Registered<Item, WeaponItem> item : ModItems.allOfType(type)) {
                helper.assertTrue(player.getCooldowns().isOnCooldown(item.get()),
                        type.id() + ": " + item.get() + " should be on cooldown too");
            }
            float expectedExhaustion = (float) dash.hungerCost() * 4.0F;
            helper.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel() - expectedExhaustion) < EPSILON,
                    type.id() + ": exhaustion " + player.getFoodData().getExhaustionLevel() + " instead of " + expectedExhaustion);
            helper.assertFalse(useMainHand(helper, player, stack), type.id() + ": no second dash during the cooldown");

            expectDash(helper, type, ItemStack.EMPTY, false, dash.allowAirborne(), "in mid-air");
            expectDash(helper, type, weapon(WeaponType.DAGGER), true, true, "with a dagger in the off hand");
            expectDash(helper, type, weapon(type), true, true, "with the same weapon in the off hand");
            expectDash(helper, type, new ItemStack(Items.TORCH), true, twoHanded, "with a torch in the off hand");
            expectDash(helper, type, new ItemStack(Items.SHIELD), true, twoHanded, "with a shield in the off hand");

            ItemStack hungryStack = weapon(type);
            Player hungry = dasher(helper, hungryStack, ItemStack.EMPTY, true);
            hungry.getFoodData().setFoodLevel(6);
            helper.assertTrue(useMainHand(helper, hungry, hungryStack) == (dash.hungerCost() <= 0.0D),
                    type.id() + ": a dash with 3 drumsticks of food should only work if it costs no hunger");
            tested++;
        }
        helper.assertTrue(tested > 0, "no weapon has the dash ability");
        helper.succeed();
    }

    /** Direction du dash : vers le regard ("look") ou vers les touches de déplacement ("input"), et vitesse de départ. */
    @GameTest(template = ARENA)
    public static void dashDirection(GameTestHelper helper) {
        Player player = mockPlayerAt(helper, helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        player.setYRot(0.0F);  // regarde vers le sud (+Z)
        player.xxa = 1.0F;     // touche "gauche" : vers l'est (+X) quand on regarde au sud
        player.zza = 0.0F;
        assertDirection(helper, DashAbility.direction(player, DashDirection.LOOK), new Vec3(0.0D, 0.0D, 1.0D), "look");
        assertDirection(helper, DashAbility.direction(player, DashDirection.INPUT), new Vec3(1.0D, 0.0D, 0.0D), "input, left key");
        player.xxa = 0.0F;
        assertDirection(helper, DashAbility.direction(player, DashDirection.INPUT), new Vec3(0.0D, 0.0D, 1.0D), "input, no key");

        player.setOnGround(true);
        DashDefinition dash = new DashDefinition(4.0D, 1.0D, DashDirection.LOOK, false, 0.0D);
        DashAbility.launch(player, dash);
        Vec3 motion = player.getDeltaMovement();
        double expectedSpeed = DashAbility.initialSpeed(dash.distance());
        helper.assertTrue(Math.abs(motion.horizontalDistance() - expectedSpeed) < EPSILON && motion.z > 0.0D && motion.y > 0.0D,
                "launch: motion " + motion + ", expected a speed of " + expectedSpeed + " toward +Z with a small hop");
        helper.succeed();
    }

    /**
     * Combos d'une arme seule (faux, katana, hache de guerre...) : les coups normaux ne touchent que la cible ;
     * le coup en zone touche aussi l'ennemi proche, mais ni l'ennemi trop loin ni celui hors de l'angle ;
     * la recharge suivante est allongée ; un coup peu chargé remet le combo à zéro.
     */
    @GameTest(template = ARENA)
    public static void comboAreaFinisher(GameTestHelper helper) {
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            ComboDefinition combo = WeaponData.weapon(type).combo();
            int finisher = firstStep(combo, step -> step.hasArea() && !step.bothHands());
            if (!combo.enabled() || WeaponData.weapon(type).dualWielded() || finisher < 0) continue;
            ComboStep area = combo.steps().get(finisher);

            // Le joueur est au bord de l'arène et regarde vers le sud (+Z).
            Vec3 origin = new Vec3(2.5D, 1.0D, 0.5D);
            Player player = player(helper, weapon(type), ItemStack.EMPTY);
            player.setPos(helper.absoluteVec(origin));
            player.setYRot(0.0F);
            player.setYHeadRot(0.0F);
            double sideAngle = Math.toRadians(Math.min(20.0D, area.angle() / 4.0D));
            double sideDistance = area.radius() * 0.7D;
            Pig target = pigAt(helper, origin.add(0.0D, 0.0D, 1.5D));
            Pig nearby = pigAt(helper, origin.add(Math.sin(sideAngle) * sideDistance, 0.0D, Math.cos(sideAngle) * sideDistance));
            Pig tooFar = pigAt(helper, origin.add(0.0D, 0.0D, area.radius() + 1.2D));
            Pig behind = pigAt(helper, origin.add(0.0D, 0.0D, -1.5D));
            List<Pig> pigs = List.of(target, nearby, tooFar, behind);

            for (int hit = 0; hit <= finisher; hit++) {
                ComboStep step = combo.steps().get(hit);
                pigs.forEach(CombatGameTests::heal);
                fullyCharge(player);
                player.attack(target);
                helper.assertTrue(isHurt(target), type.id() + " hit " + (hit + 1) + ": the target should be hurt");
                helper.assertTrue(isHurt(nearby) == step.hasArea(),
                        type.id() + " hit " + (hit + 1) + ": the nearby pig should " + (step.hasArea() ? "" : "not ") + "be hurt");
                helper.assertFalse(isHurt(tooFar), type.id() + " hit " + (hit + 1) + ": a pig out of the radius was hurt");
                if (step.angle() < 360.0D) {
                    helper.assertFalse(isHurt(behind), type.id() + " hit " + (hit + 1) + ": a pig out of the angle was hurt");
                }
            }

            if (area.cooldownMultiplier() != 1.0D) {
                double expectedDelay = 20.0D / player.getAttributeValue(Attributes.ATTACK_SPEED) * area.cooldownMultiplier();
                helper.assertTrue(Math.abs(player.getCurrentItemAttackStrengthDelay() - expectedDelay) < EPSILON,
                        type.id() + ": cooldown after the finisher " + player.getCurrentItemAttackStrengthDelay() + " instead of " + expectedDelay);
            }
            PlayerCombatState state = PlayerCombatState.of(player);
            helper.assertTrue(state.comboStep == (finisher + 1) % combo.steps().size(), type.id() + ": wrong combo step " + state.comboStep);

            // Un coup peu chargé remet le combo à zéro.
            pigs.forEach(CombatGameTests::heal);
            fullyCharge(player);
            player.attack(target);
            target.invulnerableTime = 0;
            ((LivingEntityAccessor) player).betterweaponry$setAttackStrengthTicker(0);
            player.attack(target);
            helper.assertTrue(state.comboStep == 0, type.id() + ": a weak hit should reset the combo, step " + state.comboStep);

            pigs.forEach(Pig::discard);
            tested++;
        }
        helper.assertTrue(tested > 0, "no single-weapon combo with an area hit");
        helper.succeed();
    }

    /**
     * Combo à deux armes (dague, faucille, chakram) : actif seulement avec le même type d'arme dans chaque main ;
     * le coup à deux armes fait la moyenne des dégâts des deux armes et use aussi l'arme secondaire.
     */
    @GameTest(template = ARENA)
    public static void comboBothHandsAverage(GameTestHelper helper) {
        assertModTagsLoaded(helper);
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            ComboDefinition combo = definition.combo();
            int bothHands = firstStep(combo, ComboStep::bothHands);
            if (!combo.enabled() || !definition.dualWielded() || bothHands < 0) continue;

            WeaponType other = type == WeaponType.DAGGER ? WeaponType.SAI : WeaponType.DAGGER;
            helper.assertTrue(ComboSystem.activeCombo(player(helper, weapon(type), ItemStack.EMPTY)).isEmpty(),
                    type.id() + ": no combo with an empty off hand");
            helper.assertTrue(ComboSystem.activeCombo(player(helper, weapon(type), weapon(other))).isEmpty(),
                    type.id() + ": no combo with another weapon type in the off hand");

            ItemStack main = weapon(type, WeaponMaterials.IRON);
            ItemStack off = weapon(type, WeaponMaterials.DIAMOND);
            Player player = player(helper, main, off);
            Vec3 origin = new Vec3(2.5D, 1.0D, 1.5D);
            player.setPos(helper.absoluteVec(origin));
            player.setYRot(0.0F);
            helper.assertTrue(ComboSystem.activeCombo(player).isPresent(), type.id() + ": the combo should be active with a pair");
            Pig target = pigAt(helper, origin.add(0.0D, 0.0D, 1.5D));

            for (int hit = 0; hit <= bothHands; hit++) {
                heal(target);
                fullyCharge(player);
                int offWear = off.getDamageValue();
                player.attack(target);
                if (hit < bothHands) continue;

                double expected = (definition.damageFor(WeaponMaterials.IRON) + definition.damageFor(WeaponMaterials.DIAMOND)) / 2.0D
                        * combo.steps().get(hit).damageMultiplier();
                double dealt = TEST_HEALTH - target.getHealth();
                helper.assertTrue(Math.abs(dealt - expected) < EPSILON,
                        type.id() + ": the two-weapon hit did " + dealt + " damage instead of the average " + expected);
                helper.assertTrue(off.getDamageValue() == offWear + 1, type.id() + ": the off-hand weapon should lose 1 durability");
            }
            target.discard();
            tested++;
        }
        helper.assertTrue(tested > 0, "no two-weapon combo with a both-hands hit");
        helper.succeed();
    }

    /** Durée réelle d'une recharge : le coup est à pleine force dès que le compteur de ticks + 0.5 atteint la recharge. */
    private static int attackTicks(double delay) {
        return (int) Math.ceil(delay - 0.5D);
    }

    private static double attackDelay(WeaponDefinition definition, boolean pair) {
        return 20.0D / (definition.attackSpeed() * (pair ? WeaponData.rules().dualWieldSpeedMultiplier() : 1.0D));
    }

    /**
     * Équilibrage à la netherite : le DPS réel de chaque arme est à moins de 2,5 % de sa cible (valeurs rondes obligent), soit
     * l'épée vanilla (12.8) × mains × portée. Les armes ambidextres sont mesurées en paire. Aucun coup n'est perdu à cause de l'invulnérabilité.
     */
    @GameTest(template = ARENA)
    public static void netheriteBalance(GameTestHelper helper) {
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            double hands = definition.twoHanded() ? 1.4D : definition.dualWielded() ? 1.2D : 1.0D;
            double target = 12.8D * hands * (1.0D - 0.04D * (definition.attackReach() - 3.0D));
            int ticks = attackTicks(attackDelay(definition, definition.dualWielded()));
            double dps = definition.damageFor(WeaponMaterials.NETHERITE) * 20.0D / ticks;
            helper.assertTrue(Math.abs(dps / target - 1.0D) < 0.025D, type.id() + ": " + dps + " DPS instead of " + target);

            helper.assertTrue(attackTicks(attackDelay(definition, false)) >= 10, type.id() + ": too fast, a lone weapon would lose hits");
            if (definition.dualWielded()) {
                helper.assertTrue(ticks >= Math.round(WeaponData.rules().dualWieldInvulnerabilitySeconds() * 20.0D),
                        type.id() + ": too fast, a pair would lose hits");
            }
        }
        helper.succeed();
    }

    /** PvP : après l'armure, un coup au corps à corps entre joueurs est plafonné ; les autres dégâts et les autres cibles ne le sont pas. */
    @GameTest(template = ARENA)
    public static void pvpHitCap(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Player attacker = mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D));
        Player defender = mockPlayerAt(helper, position);
        setBase(defender.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        defender.setHealth(TEST_HEALTH);
        double cap = TEST_HEALTH * WeaponData.rules().pvpMaxHitPercent() / 100.0D;

        defender.hurt(defender.damageSources().playerAttack(attacker), 1000.0F);
        helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - cap) < EPSILON, "the hit should be capped at " + cap + ", took " + (TEST_HEALTH - defender.getHealth()));

        defender.setHealth(TEST_HEALTH);
        defender.invulnerableTime = 0;
        defender.hurt(defender.damageSources().playerAttack(attacker), RAW_DAMAGE);
        helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - RAW_DAMAGE) < EPSILON, "a small hit should not change");

        defender.setHealth(TEST_HEALTH);
        defender.invulnerableTime = 0;
        defender.hurt(defender.damageSources().generic(), 90.0F);
        helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - 90.0D) < EPSILON, "other damage should not be capped");

        Pig pig = pigAt(helper, new Vec3(1.5D, 1.0D, 1.5D));
        pig.hurt(pig.damageSources().playerAttack(attacker), 90.0F);
        helper.assertTrue(Math.abs(TEST_HEALTH - pig.getHealth() - 90.0D) < EPSILON, "a mob should not be protected by the cap");
        helper.succeed();
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

    /** Faux joueur (déployeur de Create...) : une arme simple, ses dégâts et rien d'autre. */
    @GameTest(template = ARENA)
    public static void fakePlayersUsePlainWeapons(GameTestHelper helper) {
        ServerPlayer fake = Platform.get().createFakePlayer(helper.getLevel());
        ItemStack dagger = weapon(WeaponType.DAGGER);
        fake.setItemInHand(InteractionHand.MAIN_HAND, dagger);
        fake.setItemInHand(InteractionHand.OFF_HAND, weapon(WeaponType.DAGGER));
        helper.assertFalse(DualWieldSystem.isDualWielding(fake), "a fake player never dual wields");
        fake.setOnGround(true);
        helper.assertFalse(DashAbility.canDash(fake, InteractionHand.MAIN_HAND, dagger), "a fake player never dashes");
        fake.setOnGround(false);
        helper.assertTrue(ComboSystem.activeCombo(fake).isEmpty(), "a fake player has no combo");

        ItemStack katana = weapon(WeaponType.KATANA);
        fake.setItemInHand(InteractionHand.MAIN_HAND, katana);
        fake.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertFalse(ParrySystem.canParry(fake, InteractionHand.MAIN_HAND, katana), "a fake player never parries");

        ItemStack hammer = weapon(WeaponType.HAMMER);
        fake.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        hammer.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = fake.getAttribute(attribute);
            if (instance != null && !instance.hasModifier(modifier.id())) instance.addTransientModifier(modifier);
        });
        Pig pig = pigAt(helper, new Vec3(1.5D, 1.0D, 1.5D));
        double expected = WeaponData.weapon(WeaponType.HAMMER).damageFor(WeaponMaterials.IRON);
        try {
            for (int hit = 0; hit < WeaponData.weapon(WeaponType.HAMMER).combo().steps().size(); hit++) {
                heal(pig);
                fullyCharge(fake);
                fake.attack(pig);
                double dealt = TEST_HEALTH - pig.getHealth();
                helper.assertTrue(Math.abs(dealt - expected) < EPSILON, "hit " + (hit + 1) + " did " + dealt + " instead of the weapon's " + expected);
            }
            helper.assertFalse(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a fake player should not apply the weapon's effects");
        } finally {
            hammer.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                AttributeInstance instance = fake.getAttribute(attribute);
                if (instance != null) instance.removeModifier(modifier.id());
            });
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    /** Stats lisibles : une décimale au plus pour la vitesse, des demi-points pour les dégâts, quel que soit le matériau. */
    @GameTest(template = ARENA)
    public static void roundStats(GameTestHelper helper) {
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            helper.assertTrue(Math.abs(definition.attackSpeed() * 10.0D - Math.rint(definition.attackSpeed() * 10.0D)) < 1.0E-6D,
                    type.id() + ": attack speed " + definition.attackSpeed() + " has more than one decimal");
            for (WeaponMaterial material : WeaponMaterials.all()) {
                double damage = definition.damageFor(material);
                helper.assertTrue(damage * 2.0D == Math.rint(damage * 2.0D), type.id() + " " + material.id() + ": damage " + damage + " is not a half point");
            }
        }
        helper.assertTrue(WeaponData.weapon(WeaponType.KATANA).attackReach() == 3.5D, "the katana should reach 3.5 blocks");
        helper.succeed();
    }

    /** Un combo ne coûte pas de DPS sur une seule cible : le coup final fait autant de dégâts en plus que sa recharge en plus. */
    @GameTest(template = ARENA)
    public static void comboKeepsSingleTargetDps(GameTestHelper helper) {
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            if (!definition.combo().enabled()) continue;
            double delay = attackDelay(definition, definition.dualWielded());
            double damage = 0.0D;
            int ticks = 0;
            for (ComboStep step : definition.combo().steps()) {
                damage += step.damageMultiplier();
                ticks += attackTicks(delay * step.cooldownMultiplier());
            }
            double ratio = damage / ticks * attackTicks(delay);
            helper.assertTrue(Math.abs(ratio - 1.0D) < 0.015D, type.id() + ": the combo multiplies the DPS by " + ratio);
            tested++;
        }
        helper.assertTrue(tested > 0, "no weapon has a combo");
        helper.succeed();
    }

    /** Mode "équilibrage global" : coupé par défaut, chaque capacité active compte, et le total est plafonné. */
    @GameTest(template = ARENA)
    public static void abilityPenaltyMode(GameTestHelper helper) {
        RulesDefinition balanced = new RulesDefinition(1, true, 1.5D, 0.25D, true, true, true, true, true, true, true, true, true, true, true, true, true, 60.0D);
        RulesDefinition noParry = new RulesDefinition(1, true, 1.5D, 0.25D, true, true, true, true, true, true, true, false, true, true, true, true, true, 60.0D);
        WeaponDefinition katana = WeaponData.weapon(WeaponType.KATANA);
        WeaponDefinition hammer = WeaponData.weapon(WeaponType.HAMMER);
        for (WeaponType type : WeaponType.values()) {
            double penalty = WeaponData.weapon(type).abilityPenalty(balanced);
            helper.assertTrue(WeaponData.weapon(type).abilityPenalty(WeaponData.rules()) == 0.0D, type.id() + ": the mode should be off by default");
            helper.assertTrue(penalty >= 0.0D && penalty <= 0.12D, type.id() + ": penalty " + penalty);
        }
        // Katana : parade 4 %, combo en zone 3 %, pénétration 1 %.
        helper.assertTrue(Math.abs(katana.abilityPenalty(balanced) - 0.08D) < EPSILON, "katana penalty " + katana.abilityPenalty(balanced));
        helper.assertTrue(Math.abs(katana.abilityPenalty(noParry) - 0.04D) < EPSILON, "katana without parry " + katana.abilityPenalty(noParry));
        // Marteau : combo en zone 3 %, effet 2 %, bouclier 2 %, pénétration 3 %.
        helper.assertTrue(Math.abs(hammer.abilityPenalty(balanced) - 0.10D) < EPSILON, "hammer penalty " + hammer.abilityPenalty(balanced));
        helper.succeed();
    }

    /** Outils de l'éditeur : un fichier complété contient tous les champs, et seule une vraie modification est un écart. */
    @GameTest(template = ARENA)
    public static void editorJsonTools(GameTestHelper helper) {
        EditorDocument document = EditorDocument.weapon(WeaponType.KATANA);
        JsonObject defaults = document.defaults();
        for (var field : document.fields().fields()) {
            helper.assertTrue(defaults.has(field.name()), "the expanded file should contain " + field.name());
        }
        helper.assertTrue(JsonTree.diff(document.fields(), defaults, defaults).isEmpty(), "no difference between a file and itself");

        JsonObject edited = defaults.deepCopy();
        edited.getAsJsonObject("sweep").addProperty("radius", defaults.getAsJsonObject("sweep").get("radius").getAsDouble() + 1.5D);
        edited.addProperty("attack_speed", defaults.get("attack_speed").getAsDouble());  // même valeur, écrite autrement
        List<String> changed = JsonTree.paths(JsonTree.diff(document.fields(), edited, defaults));
        helper.assertTrue(changed.equals(List.of("sweep.radius")), "only sweep.radius should differ, got " + changed);
        helper.succeed();
    }

    /**
     * Enregistrement "tous les mondes" : seul le champ modifié est écrit dans config/better_weaponry, il s'applique
     * tout de suite, une valeur invalide est refusée, puis tout est remis comme avant.
     */
    @GameTest(template = ARENA)
    public static void editorSavesOnlyDifferences(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        EditorDocument document = EditorDocument.weapon(WeaponType.GLAIVE);
        JsonObject original = document.json(WeaponData.current());
        double reach = original.get("attack_reach").getAsDouble() + 0.25D;
        List<EditorService.Outcome> outcomes = new ArrayList<>();
        try {
            JsonObject edited = original.deepCopy();
            edited.addProperty("attack_reach", reach);
            EditorService.save(server, document.id(), EditorTarget.ALL_WORLDS, edited.toString(), outcomes::add);
            helper.assertTrue(outcomes.size() == 1 && outcomes.getFirst().success(), "the save should succeed: " + outcomes);
            helper.assertTrue(WeaponData.weapon(WeaponType.GLAIVE).attackReach() == reach, "the new reach should apply at once");

            JsonObject written = JsonUtil.parseLenient(Files.readString(document.configFile())).getAsJsonObject();
            written.remove(FieldSet.SCHEMA_KEY);
            JsonObject defaultsDiff = JsonTree.diff(document.fields(), original, document.defaults());
            JsonObject expected = defaultsDiff.deepCopy();
            expected.addProperty("attack_reach", reach);
            helper.assertTrue(JsonTree.sameValue(written, expected), "the file should only contain the changes, got " + written);

            JsonObject invalid = original.deepCopy();
            invalid.addProperty("attack_speed", 50);
            outcomes.clear();
            EditorService.save(server, document.id(), EditorTarget.ALL_WORLDS, invalid.toString(), outcomes::add);
            helper.assertTrue(outcomes.size() == 1 && !outcomes.getFirst().success(), "an out-of-range value should be refused");
            helper.assertTrue(WeaponData.weapon(WeaponType.GLAIVE).attackReach() == reach, "a refused save should change nothing");
        } catch (java.io.IOException e) {
            helper.fail("could not read " + document.configFile() + ": " + e.getMessage());
        } finally {
            EditorService.save(server, document.id(), EditorTarget.ALL_WORLDS, original.toString(), outcome -> { });
        }
        helper.assertTrue(JsonTree.sameValue(document.json(WeaponData.current()), original), "the original values should be back");
        helper.succeed();
    }

    /**
     * Parade parfaite : au début de la garde, un coup est bloqué en entier sans user l'arme ; après la fenêtre, il est
     * seulement réduit ; relever la garde tout de suite ne redonne pas de fenêtre ; les interrupteurs la coupent.
     */
    @GameTest(template = ARENA)
    public static void perfectParryWindow(GameTestHelper helper) {
        ItemStack stack = weapon(WeaponType.BROADSWORD);
        ParryDefinition parry = ParrySystem.parryOf(stack).orElseThrow();
        helper.assertTrue(parry.perfectWindowSeconds() > 0.0D, "the broadsword should have a perfect parry window");
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Player defender = parrier(helper, stack, position);
        Player attacker = mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D));
        PlayerCombatState state = PlayerCombatState.of(defender);

        startParry(helper, defender, stack);
        helper.assertTrue(ParrySystem.isInPerfectWindow(defender), "the window should open with the parry");
        defender.hurt(defender.damageSources().playerAttack(attacker), RAW_DAMAGE);
        helper.assertTrue(defender.getHealth() == TEST_HEALTH, "a perfect parry should block everything, took " + (TEST_HEALTH - defender.getHealth()));
        helper.assertTrue(stack.getDamageValue() == 0, "a perfect parry should not wear the weapon");

        // Bonus : le coup suivant fait plus de dégâts, une seule fois.
        Pig bonusTarget = pigAt(helper, new Vec3(1.5D, 1.0D, 1.5D));
        bonusTarget.hurt(defender.damageSources().playerAttack(defender), RAW_DAMAGE);
        double withBonus = TEST_HEALTH - bonusTarget.getHealth();
        double expectedBonus = RAW_DAMAGE * (1.0D + parry.perfectDamageBonus() / 100.0D);
        helper.assertTrue(Math.abs(withBonus - expectedBonus) < EPSILON, "the hit after a perfect parry did " + withBonus + " instead of " + expectedBonus);
        heal(bonusTarget);
        bonusTarget.hurt(defender.damageSources().playerAttack(defender), RAW_DAMAGE);
        helper.assertTrue(Math.abs(TEST_HEALTH - bonusTarget.getHealth() - RAW_DAMAGE) < EPSILON, "the bonus should only work once");
        heal(bonusTarget);
        state.perfectBonus = 0.5D;
        state.perfectBonusUntil = 0L;
        bonusTarget.hurt(defender.damageSources().playerAttack(defender), RAW_DAMAGE);
        helper.assertTrue(Math.abs(TEST_HEALTH - bonusTarget.getHealth() - RAW_DAMAGE) < EPSILON, "an expired bonus should do nothing");
        bonusTarget.discard();

        // Fenêtre écoulée : le coup est réduit normalement.
        state.perfectParryUntil = 0L;
        defender.invulnerableTime = 0;
        defender.hurt(defender.damageSources().playerAttack(attacker), RAW_DAMAGE);
        double expected = RAW_DAMAGE * (1.0D - parry.damageReduction() / 100.0D);
        helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - expected) < EPSILON, "after the window the hit should only be reduced");

        // Anti-spam : relever la garde tout de suite ne redonne pas de fenêtre.
        defender.stopUsingItem();
        startParry(helper, defender, stack);
        helper.assertFalse(ParrySystem.isInPerfectWindow(defender), "raising the guard again at once should not open a window");
        defender.stopUsingItem();
        state.perfectParryReadyAt = 0L;
        startParry(helper, defender, stack);
        helper.assertTrue(ParrySystem.isInPerfectWindow(defender), "the window should be available again after the cooldown");

        // Interrupteur coupé : plus de fenêtre, la parade reste.
        WeaponData.Snapshot original = WeaponData.current();
        try {
            WeaponData.install(new WeaponData.Snapshot(original.weapons(),
                    new RulesDefinition(1, true, 1.5D, 0.25D, true, true, true, true, true, true, true, true, false, true, true, true, false, 60.0D), original.gear()));
            defender.stopUsingItem();
            state.perfectParryReadyAt = 0L;
            startParry(helper, defender, stack);
            helper.assertFalse(ParrySystem.isInPerfectWindow(defender), "perfect_parry_enabled false: no window");
        } finally {
            WeaponData.install(original);
        }
        helper.succeed();
    }

    /**
     * Garde brisée : un coup d'une arme qui désactive les boucliers (ou d'une hache) arrête la parade et met l'arme
     * en recharge, mais reste réduit. Une parade parfaite n'est jamais brisée, ni une arme qui ne désactive rien.
     */
    @GameTest(template = ARENA)
    public static void guardBreak(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Vec3 front = position.add(0.0D, 0.0D, 2.0D);

        for (ItemStack breaker : List.of(weapon(WeaponType.HAMMER), new ItemStack(Items.IRON_AXE))) {
            ItemStack stack = weapon(WeaponType.KATANA);
            ParryDefinition parry = ParrySystem.parryOf(stack).orElseThrow();
            Player defender = parrier(helper, stack, position);
            Player attacker = player(helper, breaker, ItemStack.EMPTY);
            attacker.setPos(front);
            startParry(helper, defender, stack);
            PlayerCombatState.of(defender).perfectParryUntil = 0L;

            defender.hurt(defender.damageSources().playerAttack(attacker), RAW_DAMAGE);
            double expected = RAW_DAMAGE * (1.0D - parry.damageReduction() / 100.0D);
            helper.assertTrue(Math.abs(TEST_HEALTH - defender.getHealth() - expected) < EPSILON, breaker + ": the hit that breaks the guard should stay reduced");
            helper.assertFalse(ParrySystem.isParrying(defender), breaker + ": the parry should stop");
            for (Registered<Item, WeaponItem> item : ModItems.allOfType(WeaponType.KATANA)) {
                helper.assertTrue(defender.getCooldowns().isOnCooldown(item.get()), breaker + ": " + item.get() + " should be on cooldown");
            }
            helper.assertFalse(stack.use(helper.getLevel(), defender, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    breaker + ": no parry during the guard break");
        }

        // Parade parfaite : ni dégâts ni garde brisée, même contre un marteau.
        ItemStack perfect = weapon(WeaponType.KATANA);
        Player perfectDefender = parrier(helper, perfect, position);
        Player hammerUser = player(helper, weapon(WeaponType.HAMMER), ItemStack.EMPTY);
        hammerUser.setPos(front);
        startParry(helper, perfectDefender, perfect);
        perfectDefender.hurt(perfectDefender.damageSources().playerAttack(hammerUser), RAW_DAMAGE);
        helper.assertTrue(ParrySystem.isParrying(perfectDefender) && perfectDefender.getHealth() == TEST_HEALTH, "a perfect parry is never broken");

        // Une arme qui ne désactive pas les boucliers ne brise rien.
        ItemStack calm = weapon(WeaponType.KATANA);
        Player calmDefender = parrier(helper, calm, position);
        Player katanaUser = player(helper, weapon(WeaponType.KATANA), ItemStack.EMPTY);
        katanaUser.setPos(front);
        startParry(helper, calmDefender, calm);
        PlayerCombatState.of(calmDefender).perfectParryUntil = 0L;
        calmDefender.hurt(calmDefender.damageSources().playerAttack(katanaUser), RAW_DAMAGE);
        helper.assertTrue(ParrySystem.isParrying(calmDefender), "a katana should not break a guard");
        helper.succeed();
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

    /**
     * Champ "items" : un objet vanilla adopté reprend les comportements de l'arme mais garde ses dégâts ; un id précis
     * passe avant un tag ; une arme du mod n'est jamais adoptée ; un objet revendiqué deux fois est signalé.
     */
    @GameTest(template = ARENA)
    public static void adoptedItems(GameTestHelper helper) {
        assertModTagsLoaded(helper);
        WeaponData.Snapshot original = WeaponData.current();
        Map<WeaponType, WeaponDefinition> weapons = new EnumMap<>(original.weapons());
        weapons.put(WeaponType.BROADSWORD, withItems(original.weapon(WeaponType.BROADSWORD), "minecraft:diamond_sword", "minecraft:iron_sword"));
        // Le marteau revendique aussi tout le tag des épées, et même une arme du mod.
        weapons.put(WeaponType.HAMMER, withItems(original.weapon(WeaponType.HAMMER), "minecraft:golden_sword", "#minecraft:swords", "better_weaponry:iron_dagger"));
        try {
            WeaponData.install(new WeaponData.Snapshot(weapons, original.rules(), original.gear()));

            // Parade reprise, dégâts de l'objet conservés.
            ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
            helper.assertTrue(WeaponProfiles.type(diamondSword).orElse(null) == WeaponType.BROADSWORD, "an exact id should win over the tag of swords");
            Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
            Player defender = parrier(helper, diamondSword, position);
            helper.assertTrue(defender.getAttributeValue(Attributes.ATTACK_DAMAGE) == 7.0D, "the diamond sword should keep its damage");
            helper.assertTrue(defender.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == 3.0D, "the diamond sword should keep its reach");
            helper.assertTrue(diamondSword.getUseAnimation() == UseAnim.BLOCK && diamondSword.getUseDuration(defender) == ParrySystem.USE_DURATION,
                    "an adopted item should have the parry animation");
            helper.assertTrue(ParrySystem.canParry(defender, InteractionHand.MAIN_HAND, diamondSword), "an adopted item should be able to parry");
            ParrySystem.beginParry(defender, diamondSword);
            defender.startUsingItem(InteractionHand.MAIN_HAND);
            PlayerCombatState.of(defender).perfectParryUntil = 0L;
            helper.assertTrue(ParrySystem.isParrying(defender), "the adopted item should be parrying");
            defender.hurt(defender.damageSources().playerAttack(mockPlayerAt(helper, position.add(0.0D, 0.0D, 2.0D))), RAW_DAMAGE);
            helper.assertTrue(TEST_HEALTH - defender.getHealth() < RAW_DAMAGE, "the parry of an adopted item should reduce the damage");

            // Bonus de l'arme sur un objet adopté : pénétration, effets, boucliers, deux mains.
            WeaponDefinition hammer = weapons.get(WeaponType.HAMMER);
            ItemStack goldenSword = new ItemStack(Items.GOLDEN_SWORD);
            Player attacker = player(helper, goldenSword, ItemStack.EMPTY);
            helper.assertTrue(Math.abs(attacker.getAttributeValue(ModAttributes.armorPenetration()) - hammer.armorPenetration() / 100.0D) < EPSILON,
                    "an adopted item should get the armor penetration");
            helper.assertTrue(attacker.canDisableShield() == hammer.disablesShield(), "an adopted item should disable shields like its weapon");
            helper.assertTrue(TwoHandedSystem.isOffhandLocked(attacker) == hammer.twoHanded(), "an adopted item should lock the off hand like its weapon");
            Pig target = armoredPig(helper, new BlockPos(2, 1, 2));
            goldenSword.hurtEnemy(target, attacker);
            for (OnHitEffect effect : hammer.onHitEffects()) {
                if (effect.chance() >= 100.0D) helper.assertTrue(target.hasEffect(effect.holder().orElseThrow()), "an adopted item should give " + effect.effect());
            }

            // Une arme du mod n'est jamais adoptée.
            helper.assertTrue(WeaponProfiles.type(weapon(WeaponType.DAGGER)).orElse(null) == WeaponType.DAGGER, "a weapon of the mod keeps its own profile");
            helper.assertFalse(WeaponProfiles.of(new ItemStack(Items.STICK)).isPresent(), "a stick has no profile");

            Map<WeaponType, WeaponDefinition> conflicting = new EnumMap<>(weapons);
            conflicting.put(WeaponType.KATANA, withItems(original.weapon(WeaponType.KATANA), "minecraft:diamond_sword"));
            List<DataProblem> problems = new ArrayList<>();
            WeaponProfiles.reportConflicts(conflicting, problems);
            helper.assertTrue(problems.size() == 1 && "minecraft:diamond_sword".equals(problems.getFirst().detail()), "the conflict should be reported: " + problems);
        } finally {
            WeaponData.install(original);
        }
        helper.assertFalse(WeaponProfiles.of(new ItemStack(Items.DIAMOND_SWORD)).isPresent(), "nothing is adopted by default");
        helper.succeed();
    }

    /** Copie d'une arme avec ces valeurs dans son champ "items". */
    private static WeaponDefinition withItems(WeaponDefinition definition, String... items) {
        JsonObject json = WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition).getOrThrow().getAsJsonObject();
        JsonArray list = new JsonArray();
        for (String item : items) list.add(item);
        json.add(WeaponDefinition.ITEMS.name(), list);
        return WeaponDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    /**
     * Rangement du tooltip : deux mains juste au-dessus du titre des stats sans indentation, portée placée avant les dégâts,
     * détails remplacés par l'invite tant que la touche n'est pas maintenue, couleur appliquée, catégorie cachée absente.
     */
    @GameTest(template = ARENA)
    public static void tooltipLayout(GameTestHelper helper) {
        TooltipLayout.Settings defaults = new TooltipLayout.Settings(TooltipCategory::defaultPlacement, TooltipCategory::defaultColor);
        Component hint = Component.literal("hint");

        List<Component> closed = arrangedTooltip(defaults, false, hint);
        helper.assertTrue(texts(closed).equals(List.of("Weapon", "", "two", "When in Main Hand:",
                " reach", " Attack Damage", " Attack Speed", "hint", "ID")),
                "wrong layout without the key: " + texts(closed));
        helper.assertTrue(closed.get(2).getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY)),
                "the hands line should be dark gray");
        helper.assertTrue(closed.get(4).getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN)),
                "the stats should be dark green");

        List<Component> open = arrangedTooltip(defaults, true, hint);
        helper.assertTrue(texts(open).equals(List.of("Weapon", "", "two", "When in Main Hand:",
                " reach", " Attack Damage", " Attack Speed", " parry", "ID")),
                "wrong layout with the key: " + texts(open));

        for (TooltipCategory category : List.of(TooltipCategory.SWEEP, TooltipCategory.SHIELD_DISABLING, TooltipCategory.PROJECTILE_SPEED)) {
            helper.assertTrue(category.defaultPlacement() == TooltipPlacement.HIDDEN, category.id() + " should be hidden by default");
        }

        // Tout caché : le titre des stats et sa ligne vide disparaissent aussi.
        TooltipLayout.Settings hidden = new TooltipLayout.Settings(category -> TooltipPlacement.HIDDEN, TooltipCategory::defaultColor);
        List<Component> empty = arrangedTooltip(hidden, true, hint);
        helper.assertTrue(texts(empty).equals(List.of("Weapon", "ID")), "wrong layout with everything hidden: " + texts(empty));
        helper.succeed();
    }

    private static List<Component> arrangedTooltip(TooltipLayout.Settings settings, boolean detailsVisible, Component hint) {
        List<Component> tooltip = new ArrayList<>(List.of(Component.literal("Weapon"), Component.empty(),
                Component.translatable("item.modifiers.mainhand"),
                Component.literal(" ").append(Component.translatable("attribute.name.generic.attack_damage")),
                Component.literal(" ").append(Component.translatable("attribute.name.generic.attack_speed")),
                Component.literal("ID")));
        Map<TooltipCategory, List<Component>> lines = new EnumMap<>(TooltipCategory.class);
        lines.put(TooltipCategory.DAMAGE, TooltipLayout.takeLines(tooltip, "attack_damage"));
        lines.put(TooltipCategory.SPEED, TooltipLayout.takeLines(tooltip, "attack_speed"));
        lines.put(TooltipCategory.HANDS, List.of(Component.literal(" ").append("two")));
        lines.put(TooltipCategory.REACH, List.of(Component.literal(" ").append("reach")));
        lines.put(TooltipCategory.ABILITY, List.of(Component.literal(" ").append("parry")));
        TooltipLayout.arrange(tooltip, lines, settings, detailsVisible, hint);
        return tooltip;
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

    /** Parade parfaite au bouclier : pas d'usure, pas de garde brisée, recul de l'attaquant, flèche renvoyée vers le tireur. */
    @GameTest(template = ARENA)
    public static void shieldPerfectParry(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Vec3 front = position.add(0.0D, 0.0D, 2.0D);
        ItemStack shield = new ItemStack(ModItems.shield(WeaponMaterials.IRON).get());
        Player defender = shieldBearer(helper, shield, position);
        PlayerCombatState state = PlayerCombatState.of(defender);
        Player axeUser = player(helper, new ItemStack(Items.IRON_AXE), ItemStack.EMPTY);
        axeUser.setPos(front);

        helper.assertTrue(defender.isBlocking() && ParrySystem.isInPerfectWindow(defender), "the window should be open");
        defender.hurt(defender.damageSources().playerAttack(axeUser), RAW_DAMAGE);
        helper.assertTrue(defender.getHealth() == TEST_HEALTH && shield.getDamageValue() == 0, "a perfect parry should not hurt nor wear the shield");
        helper.assertTrue(defender.isBlocking(), "a perfect parry should not be broken by an axe");
        helper.assertTrue(axeUser.getDeltaMovement().horizontalDistance() > 0.0D, "the attacker should be pushed back");
        helper.assertTrue(state.perfectBonus > 0.0D, "a perfect parry should give the damage bonus");

        // Hors de la fenêtre : usure et garde brisée, comme en vanilla.
        state.perfectParryUntil = 0L;
        defender.invulnerableTime = 0;
        defender.hurt(defender.damageSources().playerAttack(axeUser), RAW_DAMAGE);
        helper.assertTrue(shield.getDamageValue() > 0, "a normal block should wear the shield");
        helper.assertFalse(defender.isUsingItem(), "an axe should disable a shield outside of the window");

        // Flèche renvoyée vers le tireur.
        ItemStack arrowShield = new ItemStack(ModItems.shield(WeaponMaterials.IRON).get());
        Player archerTarget = shieldBearer(helper, arrowShield, position);
        Pig shooter = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        arrow.setPos(position.add(0.0D, 1.0D, 1.0D));
        arrow.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertTrue(ShieldSystem.tryReflectArrow(arrow, archerTarget), "the arrow should be reflected");
        helper.assertTrue(arrow.getOwner() == archerTarget && arrow.getDeltaMovement().z > 0.0D, "the arrow should fly back toward its shooter");
        PlayerCombatState.of(archerTarget).perfectParryUntil = 0L;
        Arrow late = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        late.setPos(position.add(0.0D, 1.0D, 1.0D));
        helper.assertFalse(ShieldSystem.tryReflectArrow(late, archerTarget), "no reflection outside of the window");
        helper.succeed();
    }

    /** Le bouclier vanilla devient une version du mod : butin, conversion de l'inventaire, usure proportionnelle, enchantements gardés. */
    @GameTest(template = ARENA)
    public static void vanillaShieldReplaced(GameTestHelper helper) {
        ItemStack vanilla = new ItemStack(Items.SHIELD);
        vanilla.setDamageValue(vanilla.getMaxDamage() / 2);
        vanilla.enchant(enchantment(helper, Enchantments.UNBREAKING), 2);
        ItemStack converted = VanillaGear.convert(vanilla, WeaponMaterials.DIAMOND);
        helper.assertTrue(converted.is(ModItems.shield(WeaponMaterials.DIAMOND).get()), "the shield should become a diamond shield");
        helper.assertTrue(Math.abs((double) converted.getDamageValue() / converted.getMaxDamage() - 0.5D) < 0.01D, "the wear should stay proportional");
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.UNBREAKING), converted) == 2, "enchantments should be kept");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(3, new ItemStack(Items.SHIELD));
        player.getEnderChestInventory().setItem(1, new ItemStack(Items.SHIELD));
        VanillaGear.convertInventory(player);
        helper.assertTrue(player.getInventory().getItem(3).is(ModItems.shield(WeaponMaterials.WOODEN).get()), "the inventory should be converted");
        helper.assertTrue(player.getEnderChestInventory().getItem(1).is(ModItems.shield(WeaponMaterials.WOODEN).get()), "the ender chest should be converted");

        List<ItemStack> loot = new ArrayList<>(List.of(new ItemStack(Items.SHIELD), new ItemStack(Items.DIAMOND)));
        VanillaGear.replaceInLoot(loot, helper.getLevel().getRandom(), WeaponData.gear());
        helper.assertTrue(loot.get(0).getItem() instanceof WeaponShieldItem && loot.get(1).is(Items.DIAMOND), "only the shield should be replaced in loot");
        helper.assertTrue(VanillaGear.rollMaterial(helper.getLevel().getRandom(), 0.0D) == WeaponMaterials.WOODEN, "no upgrade at 0%");
        helper.assertTrue(VanillaGear.rollMaterial(helper.getLevel().getRandom(), 100.0D) == WeaponMaterials.NETHERITE, "always upgraded at 100%");

        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 5), new ItemStack(Items.SHIELD), 12, 10, 0.05F);
        helper.assertTrue(VanillaGearEvents.convertOffer(offer, WeaponMaterials.IRON).getResult().is(ModItems.shield(WeaponMaterials.IRON).get()), "the trade should sell an iron shield");

        // Recettes : celle du bouclier vanilla est coupée, celles du mod existent.
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        helper.assertTrue(recipes.byKey(ResourceLocation.withDefaultNamespace("shield")).isEmpty(), "the vanilla shield recipe should be disabled");
        for (String id : List.of("wooden_shield", "iron_shield", "netherite_shield_smithing", "shield_decoration")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, id)).isPresent(), "missing recipe " + id);
        }
        CraftingInput decoration = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.shield(WeaponMaterials.IRON).get()), new ItemStack(Items.RED_BANNER)));
        ModShieldDecorationRecipe bannerRecipe = new ModShieldDecorationRecipe(CraftingBookCategory.MISC);
        helper.assertTrue(bannerRecipe.matches(decoration, helper.getLevel()), "a banner should go on a shield of the mod");
        helper.assertTrue(bannerRecipe.assemble(decoration, helper.getLevel().registryAccess()).get(DataComponents.BASE_COLOR) == DyeColor.RED, "the shield should take the banner's color");

        // Vrai butin : les fichiers de loot_modifiers doivent être lus et leurs conditions justes.
        WeaponData.Snapshot original = WeaponData.current();
        GearDefinition gear = original.gear();
        WeaponData.install(new WeaponData.Snapshot(original.weapons(), original.rules(), new GearDefinition(1, gear.shieldPerfectWindow(), gear.shieldPerfectCooldown(),
                gear.shieldPerfectDamageBonus(), gear.shieldPerfectBonusSeconds(), gear.shieldPerfectKnockback(), gear.reflectArrows(), gear.replaceVanillaGear(),
                gear.lootUpgradeChance(), 100.0D, gear.reflectiveCooldownSeconds(), gear.projectileSpeedMultipliers(), gear.projectileSpeedCap(),
                gear.goldenDrawTime(), gear.shurikens(), gear.mobs())));
        try {
            LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST);
            var tables = helper.getLevel().getServer().reloadableRegistries();
            helper.assertTrue(tables.getLootTable(BuiltInLootTables.NETHER_BRIDGE).getRandomItems(params).stream().anyMatch(item -> item.is(Items.ENCHANTED_BOOK)),
                    "a nether fortress chest should hold a book");
            // Le tirage normal du jeu peut donner un de nos livres dans un donjon. Mais à 100 %, l'ajout dédié en mettrait un dans chaque coffre.
            int chests = 30;
            int chestsWithModBook = 0;
            for (int i = 0; i < chests; i++) {
                boolean modBook = tables.getLootTable(BuiltInLootTables.SIMPLE_DUNGEON).getRandomItems(params).stream().anyMatch(item ->
                        EnchantmentHelper.getEnchantmentsForCrafting(item).keySet().stream().anyMatch(holder -> holder.getRegisteredName().startsWith(BetterWeaponry.MODID)));
                if (modBook) chestsWithModBook++;
            }
            helper.assertTrue(chestsWithModBook < chests, "the mod's dedicated books should not be added to overworld dungeons");
        } finally {
            WeaponData.install(original);
        }
        helper.succeed();
    }

    /** Reflective : renvoie 10 % par niveau d'un coup au corps à corps sur parade parfaite, jamais sur un projectile. */
    @GameTest(template = ARENA)
    public static void reflectiveEnchantment(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        ItemStack shield = new ItemStack(ModItems.shield(WeaponMaterials.IRON).get());
        shield.enchant(enchantment(helper, ModEnchantments.REFLECTIVE), 3);
        Player defender = shieldBearer(helper, shield, position);
        Pig attacker = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));

        defender.hurt(defender.damageSources().mobAttack(attacker), RAW_DAMAGE);
        double reflected = TEST_HEALTH - attacker.getHealth();
        helper.assertTrue(Math.abs(reflected - RAW_DAMAGE * 0.3D) < EPSILON, "reflective III should send back 30%, sent " + reflected);
        helper.assertTrue(defender.getCooldowns().isOnCooldown(shield.getItem()), "the shield should go on cooldown after reflecting");
        helper.assertTrue(defender.getCooldowns().isOnCooldown(ModItems.shield(WeaponMaterials.DIAMOND).get()), "every shield should share the cooldown");
        heal(attacker);
        ModEnchantments.reflect(defender, shield, defender.damageSources().mobAttack(attacker), RAW_DAMAGE);
        helper.assertTrue(attacker.getHealth() == TEST_HEALTH, "no reflection during the cooldown");

        // Sur une arme, la parade s'arrête pendant la recharge.
        ItemStack blade = weapon(WeaponType.BROADSWORD);
        blade.enchant(enchantment(helper, ModEnchantments.REFLECTIVE), 1);
        Player swordsman = parrier(helper, blade, position);
        startParry(helper, swordsman, blade);
        heal(attacker);
        swordsman.hurt(swordsman.damageSources().mobAttack(attacker), RAW_DAMAGE);
        helper.assertTrue(attacker.getHealth() < TEST_HEALTH && swordsman.getCooldowns().isOnCooldown(blade.getItem()), "the weapon should reflect then go on cooldown");
        helper.assertFalse(swordsman.isUsingItem(), "the parry should stop during the cooldown");

        Holder<Enchantment> reflective = enchantment(helper, ModEnchantments.REFLECTIVE);
        helper.assertFalse(Enchantment.areCompatible(reflective, enchantment(helper, ModEnchantments.RIPOSTE)), "reflective and riposte are incompatible");
        helper.assertFalse(Enchantment.areCompatible(enchantment(helper, ModEnchantments.BLEEDING), enchantment(helper, ModEnchantments.VAMPIRISM)), "bleeding and vampirism are incompatible");

        heal(attacker);
        Arrow arrow = new Arrow(helper.getLevel(), attacker, new ItemStack(Items.ARROW), null);
        ModEnchantments.reflect(defender, shield, defender.damageSources().arrow(arrow, attacker), RAW_DAMAGE);
        helper.assertTrue(attacker.getHealth() == TEST_HEALTH, "projectiles should not be reflected by the enchantment");

        // Sans parade parfaite (mod de combat installé), n'importe quel coup bloqué au bouclier compte.
        ItemStack secondShield = shield.copy();
        Player other = parrier(helper, secondShield, position);
        try {
            CombatCompat.simulateForTests(true);
            other.startUsingItem(InteractionHand.MAIN_HAND);
            ((LivingEntityAccessor) other).betterweaponry$setUseItemRemaining(ParrySystem.USE_DURATION - 6);
            heal(attacker);
            other.hurt(other.damageSources().mobAttack(attacker), RAW_DAMAGE);
            helper.assertTrue(attacker.getHealth() < TEST_HEALTH, "with a combat mod, any shield block should reflect");
            helper.assertFalse(supports(weapon(WeaponType.BROADSWORD), enchantment(helper, ModEnchantments.REFLECTIVE)), "with a combat mod, reflective is for shields only");
        } finally {
            CombatCompat.simulateForTests(false);
        }
        helper.succeed();
    }

    /** Vampirisme (courbe, demi-seconde entre deux soins), Exécution, Saignement, et enchantements suivant la capacité de l'arme. */
    @GameTest(template = ARENA)
    public static void weaponEnchantments(GameTestHelper helper) {
        helper.assertTrue(Math.abs(ModEnchantments.vampirismHeal(8.0D, 3) - 1.577D) < EPSILON, "vampirism III on 8 damage");
        helper.assertTrue(Math.abs(ModEnchantments.vampirismHeal(100.0D, 3) - 5.55D) < EPSILON, "vampirism III on 100 damage");

        ItemStack sword = weapon(WeaponType.KATANA);
        sword.enchant(enchantment(helper, ModEnchantments.VAMPIRISM), 3);
        Player player = player(helper, sword, ItemStack.EMPTY);
        setBase(player.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
        player.setHealth(50.0F);
        Pig pig = pigAt(helper, new Vec3(1.5D, 1.0D, 1.5D));
        pig.hurt(player.damageSources().playerAttack(player), 8.0F);
        double healed = player.getHealth() - 50.0D;
        helper.assertTrue(Math.abs(healed - ModEnchantments.vampirismHeal(8.0D, 3)) < EPSILON, "vampirism healed " + healed);
        heal(pig);
        pig.hurt(player.damageSources().playerAttack(player), 8.0F);
        helper.assertTrue(player.getHealth() - 50.0D == healed, "no second heal within half a second");

        // Exécution : seulement sous 30 % de vie.
        ItemStack executioner = weapon(WeaponType.KATANA);
        executioner.enchant(enchantment(helper, ModEnchantments.EXECUTION), 2);
        Player headsman = player(helper, executioner, ItemStack.EMPTY);
        Pig healthy = pigAt(helper, new Vec3(2.5D, 1.0D, 1.5D));
        healthy.hurt(headsman.damageSources().playerAttack(headsman), RAW_DAMAGE);
        helper.assertTrue(TEST_HEALTH - healthy.getHealth() == RAW_DAMAGE, "execution should not work on a healthy target");
        Pig wounded = pigAt(helper, new Vec3(3.5D, 1.0D, 1.5D));
        wounded.setHealth(20.0F);
        wounded.hurt(headsman.damageSources().playerAttack(headsman), RAW_DAMAGE);
        helper.assertTrue(Math.abs(20.0D - wounded.getHealth() - RAW_DAMAGE * 1.2D) < EPSILON, "execution II should add 20% on a wounded target");

        // Saignement : coup chargé seulement, squelettes immunisés.
        ItemStack bleeder = weapon(WeaponType.KATANA);
        bleeder.enchant(enchantment(helper, ModEnchantments.BLEEDING), 1);
        Player butcher = player(helper, bleeder, ItemStack.EMPTY);
        PlayerCombatState butcherState = PlayerCombatState.of(butcher);
        Pig weak = pigAt(helper, new Vec3(1.5D, 1.0D, 3.5D));
        butcherState.lastAttackStrength = 0.2F;
        weak.hurt(butcher.damageSources().playerAttack(butcher), 2.0F);
        helper.assertFalse(weak.hasEffect(ModEffects.BLEEDING.holder()), "a weak hit should not cause bleeding");
        butcherState.lastAttackStrength = 1.0F;
        heal(weak);
        weak.hurt(butcher.damageSources().playerAttack(butcher), 2.0F);
        helper.assertTrue(weak.hasEffect(ModEffects.BLEEDING.holder()), "a charged hit should cause bleeding");
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new BlockPos(3, 1, 3));
        skeleton.hurt(butcher.damageSources().playerAttack(butcher), 2.0F);
        helper.assertFalse(skeleton.hasEffect(ModEffects.BLEEDING.holder()), "skeletons should not bleed");
        Holder<Enchantment> bleeding = enchantment(helper, ModEnchantments.BLEEDING);
        helper.assertFalse(Enchantment.areCompatible(bleeding, enchantment(helper, Enchantments.FIRE_ASPECT)), "bleeding is incompatible with fire aspect");
        helper.assertFalse(supports(weapon(WeaponType.DAGGER), bleeding), "a dagger cannot bleed");

        // Élan raccourcit la recharge du dash.
        helper.assertTrue(Math.abs(ModEnchantments.momentumMultiplier(3) - 0.55D) < EPSILON, "momentum III leaves 55% of the cooldown");

        // Reflective et Riposte suivent la capacité configurée, pas un tag figé.
        Holder<Enchantment> reflective = enchantment(helper, ModEnchantments.REFLECTIVE);
        helper.assertTrue(supports(weapon(WeaponType.BROADSWORD), reflective), "a broadsword parries");
        ItemStack dagger = weapon(WeaponType.DAGGER);
        helper.assertFalse(supports(dagger, reflective), "a dagger does not parry");
        WeaponData.Snapshot original = WeaponData.current();
        Map<WeaponType, WeaponDefinition> weapons = new EnumMap<>(original.weapons());
        JsonObject parrying = WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, original.weapon(WeaponType.DAGGER)).getOrThrow().getAsJsonObject();
        parrying.addProperty("ability", "parry");
        weapons.put(WeaponType.DAGGER, WeaponDefinition.CODEC.parse(JsonOps.INSTANCE, parrying).getOrThrow());
        try {
            WeaponData.install(new WeaponData.Snapshot(weapons, original.rules(), original.gear()));
            helper.assertTrue(supports(dagger, reflective), "a dagger configured to parry should accept reflective");
        } finally {
            WeaponData.install(original);
        }
        helper.succeed();
    }

    /** Joueur factice qui tient un bouclier levé depuis assez longtemps pour qu'il soit efficace. */
    private static Player shieldBearer(GameTestHelper helper, ItemStack shield, Vec3 position) {
        Player player = parrier(helper, shield, position);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        ((LivingEntityAccessor) player).betterweaponry$setUseItemRemaining(ParrySystem.USE_DURATION - 6);
        return player;
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

    /** Statistiques : parades parfaites, shurikens lancés, vie volée. */
    @GameTest(template = ARENA)
    public static void playerStatistics(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            checkStatistics(helper, player);
        } finally {
            // Ce faux joueur n'accepte pas les paquets du mod : il ne doit pas rester connecté pour les autres tests.
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static void checkStatistics(GameTestHelper helper, ServerPlayer player) {
        ItemStack katana = new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get());
        Pig pig = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));
        int parriesBefore = statValue(player, ModStats.PERFECT_PARRIES.get());
        ParrySystem.onPerfectParry(player, katana, player.damageSources().mobAttack(pig), RAW_DAMAGE, 0.0D, 1.0D);
        helper.assertTrue(statValue(player, ModStats.PERFECT_PARRIES.get()) == parriesBefore + 1, "a perfect parry should be counted");

        ShurikenItem iron = ModItems.shuriken(WeaponMaterials.IRON).get();
        ItemStack multi = new ItemStack(iron);
        multi.enchant(enchantment(helper, Enchantments.MULTISHOT), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, multi);
        int thrownBefore = statValue(player, ModStats.SHURIKENS_THROWN.get());
        iron.throwFrom(helper.getLevel(), player, InteractionHand.MAIN_HAND, multi);
        helper.assertTrue(statValue(player, ModStats.SHURIKENS_THROWN.get()) == thrownBefore + 3, "each shuriken of a volley should be counted");
        helper.getLevel().getEntitiesOfClass(ShurikenEntity.class, player.getBoundingBox().inflate(6.0D)).forEach(ShurikenEntity::discard);

        ItemStack vampiric = new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get());
        vampiric.enchant(enchantment(helper, ModEnchantments.VAMPIRISM), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, vampiric);
        player.setHealth(4.0F);
        int stolenBefore = statValue(player, ModStats.HEALTH_STOLEN.get());
        player.getAttributes().addTransientAttributeModifiers(vampiric.getItem().getDefaultAttributeModifiers().modifiers().stream()
                .collect(ImmutableMultimap::<Holder<Attribute>, AttributeModifier>builder, (map, entry) -> map.put(entry.attribute(), entry.modifier()), (left, right) -> left.putAll(right.build())).build());
        pig.hurt(player.damageSources().playerAttack(player), RAW_DAMAGE);
        double regained = player.getHealth() - 4.0F;
        helper.assertTrue(regained > 0.0D, "Vampirism should have healed the player");
        helper.assertTrue(statValue(player, ModStats.HEALTH_STOLEN.get()) - stolenBefore == Math.round(regained * 10.0D), "the health stolen should be counted in tenths");
    }

    private static int statValue(ServerPlayer player, ResourceLocation stat) {
        return player.getStats().getValue(Stats.CUSTOM.get(stat));
    }

    /** Préréglages : chacun fixe tous les interrupteurs, et le tableau comparatif trie comme demandé. */
    @GameTest(template = ARENA)
    public static void presetsAndComparison(GameTestHelper helper) {
        RulesDefinition nearVanilla = presetRules(RulesPreset.NEAR_VANILLA);
        helper.assertTrue(nearVanilla.reachActive() && !nearVanilla.dashActive() && !nearVanilla.combosActive() && !nearVanilla.parryActive() && !nearVanilla.dualWieldActive(), "near vanilla");
        RulesDefinition pvp = presetRules(RulesPreset.PVP_1_8);
        helper.assertTrue(pvp.parryActive() && !pvp.perfectParryActive() && !pvp.guardBreakActive() && !pvp.reachActive() && !pvp.sweepActive() && !pvp.dashActive(), "pvp 1.8");
        RulesDefinition hard = presetRules(RulesPreset.HARD);
        helper.assertTrue(hard.parryActive() && hard.perfectParryActive() && hard.combosActive() && hard.dashActive() && hard.dualWieldActive() && hard.dualWieldSpeedMultiplier() == 1.5D && hard.dualWieldInvulnerabilitySeconds() == 0.25D, "hard");

        JsonObject gear = EditorDocument.gear().defaults();
        RulesPreset.HARD.applyToGear(gear);
        helper.assertTrue(gear.getAsJsonObject("mobs").get("guard_chance").getAsDouble() == 50.0D, "hard should make mobs guard more");
        RulesPreset.NEAR_VANILLA.applyToGear(gear);
        helper.assertTrue(gear.getAsJsonObject("mobs").get("guard_chance").getAsDouble() == MobGearDefinition.DEFAULT.guardChance(), "another preset should bring the mobs back");

        Map<WeaponType, WeaponDefinition> weapons = new EnumMap<>(WeaponType.class);
        for (WeaponType type : WeaponType.values()) weapons.put(type, WeaponData.weapon(type));
        List<WeaponComparison.Row> byDps = WeaponComparison.rows(weapons, WeaponMaterials.IRON, WeaponComparison.Column.DPS, true);
        for (int i = 1; i < byDps.size(); i++) helper.assertTrue(byDps.get(i - 1).dps() >= byDps.get(i).dps(), "the table should be sorted by DPS");
        List<WeaponComparison.Row> byReach = WeaponComparison.rows(weapons, WeaponMaterials.IRON, WeaponComparison.Column.REACH, false);
        for (int i = 1; i < byReach.size(); i++) helper.assertTrue(byReach.get(i - 1).reach() <= byReach.get(i).reach(), "the table should be sorted by reach");
        WeaponComparison.Row katana = byDps.stream().filter(row -> row.type() == WeaponType.KATANA).findFirst().orElseThrow();
        helper.assertTrue(Math.abs(katana.dps() - WeaponData.weapon(WeaponType.KATANA).dps(WeaponMaterials.IRON)) < EPSILON, "the DPS column should use the chosen material");
        helper.assertTrue(katana.pairDps() == 0.0D, "a two-handed weapon has no pair DPS");
        for (WeaponComparison.Row row : byDps) {
            WeaponDefinition definition = WeaponData.weapon(row.type());
            double damage = definition.damageFor(WeaponMaterials.IRON);
            double solo = damage * 20.0D / attackTicks(attackDelay(definition, false));
            helper.assertTrue(Math.abs(row.dps() - solo) < EPSILON, row.type().id() + ": real DPS " + row.dps() + " instead of " + solo);
            double pair = definition.dualWielded() ? damage * 20.0D / attackTicks(attackDelay(definition, true)) : 0.0D;
            helper.assertTrue(Math.abs(row.pairDps() - pair) < EPSILON, row.type().id() + ": pair DPS " + row.pairDps() + " instead of " + pair);
        }
        List<WeaponComparison.Row> byPair = WeaponComparison.rows(weapons, WeaponMaterials.IRON, WeaponComparison.Column.PAIR_DPS, true);
        for (int i = 1; i < byPair.size(); i++) helper.assertTrue(byPair.get(i - 1).pairDps() >= byPair.get(i).pairDps(), "the table should be sorted by pair DPS");
        // Trop rapide pour la protection de la cible : les coups en trop ne comptent pas.
        WeaponDefinition tooFast = WeaponDefinition.CODEC.parse(JsonOps.INSTANCE, json("{ \"attack_speed\": 10, \"base_damage\": 4, \"dual_wielded\": true }")).getOrThrow();
        double fastDamage = tooFast.damageFor(WeaponMaterials.IRON);
        helper.assertTrue(Math.abs(tooFast.dps(WeaponMaterials.IRON) - fastDamage * 2.0D) < EPSILON, "a lone weapon lands at most 2 hits per second, got " + tooFast.dps(WeaponMaterials.IRON));
        helper.assertTrue(Math.abs(tooFast.pairDps(WeaponMaterials.IRON) - fastDamage * 4.0D) < EPSILON, "a pair lands at most 4 hits per second, got " + tooFast.pairDps(WeaponMaterials.IRON));
        helper.assertTrue(WeaponComparison.rows(weapons, WeaponMaterials.NETHERITE, WeaponComparison.Column.DAMAGE, true).getFirst().damage()
                > WeaponComparison.rows(weapons, WeaponMaterials.WOODEN, WeaponComparison.Column.DAMAGE, true).getFirst().damage(), "the material should change the damage");
        helper.succeed();
    }

    private static RulesDefinition presetRules(RulesPreset preset) {
        JsonObject rules = EditorDocument.rules().defaults();
        preset.applyToRules(rules);
        return RulesDefinition.CODEC.parse(JsonOps.INSTANCE, rules).getOrThrow();
    }

    /** JEI et EMI : une page par enchantement, livres de chaque niveau, et seulement les capacités actives. */
    @GameTest(template = ARENA)
    public static void recipeViewerInfo(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        List<RecipeViewerInfo.Entry> entries = RecipeViewerInfo.entries(registries);
        for (ResourceKey<Enchantment> key : ModEnchantments.ALL) {
            String path = key.location().getPath();
            RecipeViewerInfo.Entry page = entries.stream().filter(entry -> entry.id().equals(path)).findFirst().orElseThrow();
            helper.assertTrue(page.stacks().size() == enchantment(helper, key).value().getMaxLevel(), path + " should list one book per level");
            helper.assertTrue(page.stacks().stream().allMatch(book -> book.is(Items.ENCHANTED_BOOK)), path + " should list books");
        }
        RecipeViewerInfo.Entry katana = entries.stream().filter(entry -> entry.id().equals(WeaponType.KATANA.id())).findFirst().orElseThrow();
        helper.assertTrue(katana.stacks().size() == ModItems.allOfType(WeaponType.KATANA).size(), "every material of the weapon should get the page");
        helper.assertTrue(katana.lines().stream().anyMatch(line -> line.getContents() instanceof TranslatableContents text && text.getKey().equals(BetterWeaponry.MODID + ".info.parry")), "the katana page should describe its parry");
        java.util.function.Predicate<RecipeViewerInfo.Entry> mentionsShields = entry -> entry.lines().stream().anyMatch(line -> line.getContents() instanceof TranslatableContents text && text.getKey().equals(BetterWeaponry.MODID + ".info.disables_shield"));
        helper.assertTrue(entries.stream().filter(entry -> entry.id().equals(WeaponType.HAMMER.id())).allMatch(mentionsShields), "the hammer page should say it disables shields");
        helper.assertFalse(mentionsShields.test(katana), "the katana does not disable shields");
        for (String gear : List.of("shield", "bow", "crossbow", "shuriken")) {
            helper.assertTrue(entries.stream().anyMatch(entry -> entry.id().equals(gear) && !entry.stacks().isEmpty()), gear + " should have a page");
        }
        CombatCompat.simulateForTests(true);
        try {
            helper.assertTrue(RecipeViewerInfo.entries(registries).stream().noneMatch(entry -> entry.id().equals(WeaponType.KATANA.id())), "a weapon whose abilities are all off has no page");
        } finally {
            CombatCompat.simulateForTests(false);
        }
        helper.succeed();
    }

    /** Suggestion de tutoriel : le plus utile que le joueur ne connaît pas encore, parmi ceux des systèmes actifs. */
    @GameTest(template = ARENA)
    public static void tutorialHints(GameTestHelper helper) {
        RulesDefinition rules = WeaponData.rules();
        WeaponDefinition katana = WeaponData.weapon(WeaponType.KATANA);
        helper.assertTrue(TutorialHints.suggestion(katana, rules, id -> false).orElseThrow().equals("two_handed"), "a new katana should suggest two-handed weapons");
        helper.assertTrue(TutorialHints.suggestion(katana, rules, id -> id.equals("two_handed")).orElseThrow().equals("parry"), "then the parry");
        helper.assertTrue(TutorialHints.suggestion(WeaponData.weapon(WeaponType.SAI), rules, id -> false).orElseThrow().equals("dual_wield"), "a sai should suggest dual wielding");
        helper.assertTrue(TutorialHints.suggestion(WeaponData.weapon(WeaponType.RAPIER), rules, id -> id.equals("dual_wield")).orElseThrow().equals("dash"), "a rapier should suggest the dash");
        helper.assertTrue(TutorialHints.suggestion(katana, rules, id -> true).isEmpty(), "nothing to suggest when everything is known");
        helper.assertTrue(TutorialHints.relatedScenes(WeaponData.weapon(WeaponType.SPEAR), rules).equals(List.of("combos", "armor_penetration", "reach")), "spear scenes");
        helper.assertTrue(TutorialScenes.IDS.containsAll(TutorialHints.relatedScenes(katana, rules)), "only existing tutorials");

        CombatCompat.simulateForTests(true);
        try {
            helper.assertTrue(TutorialHints.relatedScenes(katana, WeaponData.rules()).isEmpty(), "no suggestion for systems that are off");
        } finally {
            CombatCompat.simulateForTests(false);
        }
        helper.succeed();
    }

    /** Tutoriels : chaque scène est complète et cohérente, ses textes existent, et chaque tutoriel est relié à une option de l'éditeur. */
    @GameTest(template = ARENA)
    public static void tutorialScenesValid(GameTestHelper helper) {
        Language language = Language.getInstance();
        helper.assertTrue(Set.copyOf(TutorialScenes.IDS).size() == TutorialScenes.IDS.size() && TutorialScenes.IDS.size() == 10, "there should be 10 distinct tutorials");
        for (String id : TutorialScenes.IDS) {
            TutorialScene scene = TutorialScenes.create(id);
            helper.assertTrue(language.has(BetterWeaponry.MODID + ".tutorial." + id + ".title"), id + " has no title");
            helper.assertTrue(TutorialScenes.theme(id) != null, id + " has no theme");
            helper.assertTrue(scene.steps().getFirst().start() == 0, id + " should start with a step");
            int previous = -1;
            for (TutorialScene.Step step : scene.steps()) {
                helper.assertTrue(step.start() > previous && step.start() < scene.duration(), id + " has a misplaced step");
                previous = step.start();
                helper.assertTrue(step.caption().getContents() instanceof TranslatableContents text && language.has(text.getKey()), id + " has an untranslated caption");
            }
            for (TutorialScene.Zone zone : scene.zones()) helper.assertTrue(scene.actor(zone.actor()) != null && zone.to() <= scene.duration(), id + " zone");
            for (TutorialScene.Label label : scene.labels()) {
                helper.assertTrue(scene.actor(label.actor()) != null && label.to() <= scene.duration() + 30, id + " label");
                helper.assertTrue(!(label.text().getContents() instanceof TranslatableContents text) || language.has(text.getKey()), id + " has an untranslated label");
            }
            for (TutorialScene.Spark spark : scene.sparks()) helper.assertTrue(scene.actor(spark.actor()) != null && spark.tick() < scene.duration(), id + " spark");
            for (TutorialScene.Actor actor : scene.actors()) {
                for (int tick = 0; tick <= scene.duration(); tick += 7) {
                    TutorialScene.ActorPose pose = scene.pose(actor, tick);
                    helper.assertTrue(Math.abs(pose.x()) <= 4.8D && Math.abs(pose.z()) <= 4.8D, id + ": " + actor.id() + " leaves the platform at tick " + tick);
                }
            }
        }
        for (Map.Entry<String, String> link : TutorialScenes.fieldLinks().entrySet()) {
            helper.assertTrue(TutorialScenes.IDS.contains(link.getValue()), "unknown tutorial for " + link.getKey());
        }
        for (String id : TutorialScenes.IDS) {
            helper.assertTrue(TutorialScenes.fieldLinks().containsValue(id), id + " should be linked to an option");
        }
        helper.assertTrue("dual_wield".equals(TutorialScenes.sceneForField("dual_wield_enabled")) && "dual_wield".equals(TutorialScenes.sceneForField("dual_wielded")), "both dual wield options should open the tutorial");
        helper.assertTrue(TutorialScenes.sceneForField("format_version") == null, "an unrelated option has no tutorial");
        helper.succeed();
    }

    /** Animation d'une scène, et fidélité aux vrais réglages. */
    @GameTest(template = ARENA)
    public static void tutorialAnimation(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        TutorialScene scene = TutorialScene.builder("test")
                .actor("a", TutorialScene.ActorKind.PLAYER, 0.0D, 0.0D, 0.0F, sword, ItemStack.EMPTY)
                .actor("b", TutorialScene.ActorKind.ZOMBIE, 0.0D, 2.0D, 180.0F, ItemStack.EMPTY, ItemStack.EMPTY)
                .step(0, Component.literal("one")).step(20, Component.literal("two")).duration(100)
                .walk("a", 10, 20, 2.0D, 0.0D).swing("a", 30, TutorialScene.Hand.OFF).guard("a", 50, 60, TutorialScene.Hand.MAIN)
                .hurt("b", 40, "a").turn("b", 70, 0.0F).equip("a", 80, false, axe).build();
        TutorialScene.Actor a = scene.actor("a");
        TutorialScene.Actor b = scene.actor("b");
        helper.assertTrue(Math.abs(scene.pose(a, 15.0F).x() - 1.0D) < EPSILON && scene.pose(a, 15.0F).walkAmount() > 0.0F, "the actor should be halfway and walking");
        helper.assertTrue(scene.pose(a, 25.0F).x() == 2.0D && scene.pose(a, 25.0F).walkAmount() == 0.0F, "the actor should have arrived");
        helper.assertTrue(scene.pose(a, 34.0F).offSwing() > 0.0F && scene.pose(a, 34.0F).mainSwing() == 0.0F, "only the off hand swings");
        helper.assertTrue(scene.pose(a, 30.0F + TutorialScene.SWING_TICKS).offSwing() == 0.0F, "the swing should end");
        helper.assertTrue(scene.pose(a, 55.0F).guardMain() && !scene.pose(a, 60.0F).guardMain(), "the guard should last from 50 to 60");
        helper.assertTrue(scene.pose(b, 40.0F).hurt() > 0.0F && scene.pose(b, 40.0F + TutorialScene.HURT_TICKS).hurt() == 0.0F, "the hurt flash should be brief");
        helper.assertTrue(scene.pose(b, 45.0F).z() != 2.0D && scene.pose(b, 60.0F).z() == 2.0D, "the recoil should be temporary");
        helper.assertTrue(scene.pose(b, 90.0F).yaw() == 0.0F, "the actor should have turned");
        helper.assertTrue(scene.pose(a, 79.0F).mainHand() == sword && scene.pose(a, 80.0F).mainHand() == axe, "the item should change at its tick");
        helper.assertTrue(scene.stepAt(19.0F) == 0 && scene.stepAt(20.0F) == 1, "the steps should follow the time");
        helper.assertTrue(TutorialScene.fade(5.0F, 5, 30) == 0.0F && TutorialScene.fade(15.0F, 5, 30) == 1.0F && TutorialScene.fade(30.0F, 5, 30) == 0.0F, "the fade should go in and out");

        // Fidélité aux réglages.
        ParryDefinition parry = WeaponData.weapon(WeaponType.KATANA).parry();
        int window = (int) Math.round(parry.perfectWindowSeconds() * 20.0D) * TutorialScenes.SLOW_MOTION;
        List<TutorialScene.TimingBar> bars = TutorialScenes.create("perfect_parry").bars();
        helper.assertTrue(bars.size() == 2 && bars.get(0).perfect() && !bars.get(1).perfect(), "the first try should be perfect and the second late");
        helper.assertTrue(bars.get(0).windowEnd() - bars.get(0).guard() == window, "the window should follow the settings");
        double spear = WeaponData.weapon(WeaponType.SPEAR).attackReach();
        helper.assertTrue(TutorialScenes.create("reach").zones().stream().anyMatch(zone -> zone.radius() == spear), "the reach circle should follow the settings");

        helper.assertTrue(Math.abs(TutorialScenes.damageThroughArmor(17.5D, 0.0D) - 13.125D) < EPSILON, "armor formula");
        helper.assertTrue(Math.abs(TutorialScenes.damageThroughArmor(17.5D, 30.0D) - 14.4375D) < EPSILON, "armor penetration formula");
        helper.assertTrue(Math.abs(TutorialScenes.damageThroughArmor(17.5D, 100.0D) - 17.5D) < EPSILON, "full penetration ignores the armor");
        helper.assertTrue(TutorialScenes.inSector(0.0D, 2.0D, 3.0D, 90.0D) && !TutorialScenes.inSector(0.0D, -2.0D, 3.0D, 90.0D) && !TutorialScenes.inSector(0.0D, 4.0D, 3.0D, 90.0D)
                && TutorialScenes.inSector(0.0D, -2.0D, 3.0D, 360.0D), "the sector test");
        TutorialScene zone = TutorialScenes.create("attack_zone");
        helper.assertTrue(zone.pose(zone.actor("far"), 220.0F).hurt() == 0.0F, "an enemy outside the area should not be hit");
        helper.assertTrue(zone.pose(zone.actor("target"), 220.0F).hurt() > 0.0F, "the target should be hit");
        helper.succeed();
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

    /** Armes des mobs : seules les armes vanilla sont remplacées, enchantements gardés, main secondaire jamais lâchée, matériau plafonné. */
    @GameTest(template = ARENA)
    public static void mobEquipment(GameTestHelper helper) {
        MobGearDefinition none = new MobGearDefinition(0.0D, 0.0D, 0.0D, 0.0D, false, 0.0D);
        withMobGear(new MobGearDefinition(0.0D, 100.0D, 0.0D, 0.0D, false, 0.0D), () -> {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 2);
            Zombie armed = spawnWith(helper, EntityType.ZOMBIE, sword);
            helper.assertTrue(armed.getMainHandItem().getItem() instanceof WeaponItem weapon && weapon.getMaterial() == WeaponMaterials.IRON, "the iron sword should become an iron weapon of the mod");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.SHARPNESS), armed.getMainHandItem()) == 2, "enchantments should be kept");
            Zombie bare = spawnWith(helper, EntityType.ZOMBIE, ItemStack.EMPTY);
            helper.assertTrue(bare.getMainHandItem().isEmpty(), "a mob without a weapon stays without one");
            helper.assertTrue(spawnWith(helper, EntityType.PIG, new ItemStack(Items.IRON_SWORD)).getMainHandItem().is(Items.IRON_SWORD), "only hostile mobs are concerned");
            Zombie trident = spawnWith(helper, EntityType.ZOMBIE, new ItemStack(Items.TRIDENT));
            helper.assertTrue(trident.getMainHandItem().is(Items.TRIDENT), "a trident is not replaced");
        });
        withMobGear(none, () -> helper.assertTrue(spawnWith(helper, EntityType.ZOMBIE, new ItemStack(Items.IRON_SWORD)).getMainHandItem().is(Items.IRON_SWORD), "at 0% the vanilla weapon stays"));

        withMobGear(new MobGearDefinition(0.0D, 100.0D, 100.0D, 0.0D, false, 0.0D), () -> {
            Zombie dual = spawnWith(helper, EntityType.ZOMBIE, new ItemStack(Items.IRON_SWORD));
            helper.assertTrue(DualWieldSystem.isDualWielding(dual), "the mob should hold two dual-wieldable weapons");
            helper.assertTrue(((MobAccessor) dual).betterweaponry$getHandDropChances()[EquipmentSlot.OFFHAND.getIndex()] == 0.0F, "the off-hand weapon should never drop");
            CombatCompat.simulateForTests(true);
            try {
                Zombie single = spawnWith(helper, EntityType.ZOMBIE, new ItemStack(Items.IRON_SWORD));
                helper.assertTrue(single.getMainHandItem().getItem() instanceof WeaponItem && single.getOffhandItem().isEmpty(), "with another combat mod, the weapons stay but dual wield does not");
            } finally {
                CombatCompat.simulateForTests(false);
            }
        });

        withMobGear(new MobGearDefinition(0.0D, 0.0D, 0.0D, 100.0D, true, 0.0D), () -> {
            Skeleton ninja = spawnWith(helper, EntityType.SKELETON, new ItemStack(Items.BOW));
            helper.assertTrue(ninja.getMainHandItem().getItem() instanceof ShurikenItem, "a skeleton should become a ninja");
            helper.assertTrue(ninja.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof MobThrowShurikenGoal), "a ninja should know how to throw");

            Pillager captain = EntityType.PILLAGER.create(helper.getLevel());
            captain.setPos(helper.absoluteVec(new Vec3(3.5D, 1.0D, 3.5D)));
            captain.setPatrolLeader(true);
            captain.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            helper.getLevel().addFreshEntity(captain);
            helper.assertTrue(captain.getOffhandItem().getItem() instanceof WeaponShieldItem, "a captain should carry a shield of the mod");
            helper.assertTrue(captain.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof MobGuardGoal), "a captain should be able to raise it");

            CombatCompat.simulateForTests(true);
            try {
                helper.assertTrue(spawnWith(helper, EntityType.SKELETON, new ItemStack(Items.BOW)).getMainHandItem().getItem() instanceof WeaponBowItem, "no ninjas with another combat mod");
            } finally {
                CombatCompat.simulateForTests(false);
            }
        });

        helper.assertTrue(VanillaGear.materialCap(Level.OVERWORLD, false) == WeaponMaterials.IRON && VanillaGear.materialCap(Level.OVERWORLD, true) == WeaponMaterials.DIAMOND, "overworld cap");
        helper.assertTrue(VanillaGear.materialCap(Level.NETHER, false) == WeaponMaterials.DIAMOND && VanillaGear.materialCap(Level.NETHER, true) == WeaponMaterials.NETHERITE, "nether cap");
        helper.assertTrue(VanillaGear.materialCap(Level.END, true) == WeaponMaterials.DIAMOND, "end cap");
        helper.assertTrue(VanillaGear.rollMaterial(helper.getLevel().getRandom(), 100.0D, WeaponMaterials.STONE, WeaponMaterials.IRON) == WeaponMaterials.IRON, "the cap should stop the upgrades");
        helper.succeed();
    }

    /** Portée de l'arme, coups alternés et garde des mobs. */
    @GameTest(template = ARENA)
    public static void mobCombat(GameTestHelper helper) {
        // Portée : le cochon est hors d'atteinte à mains nues, à portée d'une lance.
        double extra = WeaponData.weapon(WeaponType.SPEAR).attackReach() - 3.0D;
        helper.assertTrue(extra > 0.0D, "the spear should reach further than a sword");
        Zombie reacher = spawnWith(helper, EntityType.ZOMBIE, ItemStack.EMPTY);
        Pig farPig = pigAt(helper, new Vec3(2.5D, 1.0D, 2.5D + 1.578D + extra * 0.6D));
        helper.assertFalse(reacher.isWithinMeleeAttackRange(farPig), "bare hands should not reach");
        reacher.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.get(WeaponType.SPEAR, WeaponMaterials.IRON).get()));
        helper.assertTrue(reacher.isWithinMeleeAttackRange(farPig), "the spear should reach");
        CombatCompat.simulateForTests(true);
        try {
            helper.assertFalse(reacher.isWithinMeleeAttackRange(farPig), "with another combat mod, the reach is vanilla");
        } finally {
            CombatCompat.simulateForTests(false);
        }

        // Garde à l'arme : réduit les coups de face, une hache la brise, pas un cochon.
        ItemStack katana = new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get());
        Zombie guarding = spawnWith(helper, EntityType.ZOMBIE, katana);
        Zombie open = spawnWith(helper, EntityType.ZOMBIE, ItemStack.EMPTY);
        guarding.setYHeadRot(0.0F);
        open.setYHeadRot(0.0F);
        for (Zombie zombie : List.of(guarding, open)) {
            setBase(zombie.getAttribute(Attributes.MAX_HEALTH), TEST_HEALTH);
            zombie.setHealth(TEST_HEALTH);
        }
        Pig attacker = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));
        guarding.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(guarding.isUsingItem(), "the mob should raise its guard");
        guarding.hurt(guarding.damageSources().mobAttack(attacker), RAW_DAMAGE);
        open.hurt(open.damageSources().mobAttack(attacker), RAW_DAMAGE);
        double guardedLoss = TEST_HEALTH - guarding.getHealth();
        double openLoss = TEST_HEALTH - open.getHealth();
        helper.assertTrue(guardedLoss < openLoss * 0.7D && guardedLoss > 0.0D, "a guard should reduce the hit: " + guardedLoss + " against " + openLoss);
        helper.assertFalse(MobCombat.isGuardBroken(guarding), "a pig does not break a guard");
        Player axeman = mockPlayerAt(helper, helper.absoluteVec(new Vec3(2.5D, 1.0D, 4.5D)));
        axeman.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
        guarding.invulnerableTime = 0;
        guarding.hurt(guarding.damageSources().playerAttack(axeman), RAW_DAMAGE);
        helper.assertTrue(MobCombat.isGuardBroken(guarding), "an axe should break the guard");

        // Le but de garde lève le bouclier quand la cible est proche, et respecte l'interrupteur.
        Zombie shieldBearer = spawnWith(helper, EntityType.ZOMBIE, ItemStack.EMPTY);
        shieldBearer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.shield(WeaponMaterials.IRON).get()));
        shieldBearer.setTarget(attacker);
        MobGuardGoal goal = new MobGuardGoal(shieldBearer);
        withMobGear(new MobGearDefinition(0.0D, 0.0D, 0.0D, 0.0D, false, 100.0D), () -> {
            helper.assertTrue(goal.canUse(), "at 100% the mob should guard");
            goal.start();
            helper.assertTrue(shieldBearer.isUsingItem() && shieldBearer.getUsedItemHand() == InteractionHand.OFF_HAND, "the shield should be raised");
            goal.stop();
        });

        // Dual wield : les bonus d'arme ne sont appliqués qu'au tick suivant.
        Zombie dual = spawnWith(helper, EntityType.ZOMBIE, new ItemStack(ModItems.get(WeaponType.DAGGER, WeaponMaterials.WOODEN).get()));
        dual.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.get(WeaponType.DAGGER, WeaponMaterials.NETHERITE).get()));
        helper.runAfterDelay(2, () -> {
            Pig pig = pigAt(helper, new Vec3(1.5D, 1.0D, 1.5D));
            dual.doHurtTarget(pig);
            double mainHit = TEST_HEALTH - pig.getHealth();
            pig.setHealth(TEST_HEALTH);
            pig.invulnerableTime = 0;
            helper.assertTrue(MobCombat.isOffhandTurn(dual), "the next hit is the off hand's");
            dual.swing(InteractionHand.MAIN_HAND);
            helper.assertTrue(dual.swingingArm == InteractionHand.OFF_HAND, "the off arm should swing");
            dual.doHurtTarget(pig);
            double offHit = TEST_HEALTH - pig.getHealth();
            helper.assertTrue(offHit > mainHit, "the netherite dagger should hit harder: " + offHit + " against " + mainHit);
            helper.assertTrue(dual.getMainHandItem().getItem() instanceof WeaponItem weapon && weapon.getMaterial() == WeaponMaterials.WOODEN, "the hands should be put back");
            helper.succeed();
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

    private static void withMobGear(MobGearDefinition mobs, Runnable body) {
        WeaponData.Snapshot original = WeaponData.current();
        GearDefinition g = original.gear();
        WeaponData.install(new WeaponData.Snapshot(original.weapons(), original.rules(), new GearDefinition(1, g.shieldPerfectWindow(), g.shieldPerfectCooldown(),
                g.shieldPerfectDamageBonus(), g.shieldPerfectBonusSeconds(), g.shieldPerfectKnockback(), g.reflectArrows(), g.replaceVanillaGear(), g.lootUpgradeChance(),
                g.enchantedBookChance(), g.reflectiveCooldownSeconds(), g.projectileSpeedMultipliers(), g.projectileSpeedCap(), g.goldenDrawTime(), g.shurikens(), mobs)));
        try {
            body.run();
        } finally {
            WeaponData.install(original);
        }
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

    /** Mobs : l'arme vanilla devient celle du mod à l'apparition, et ils savent s'en servir ; leur tir plus rapide reste bien visé. */
    @GameTest(template = ARENA)
    public static void rangedMobs(GameTestHelper helper) {
        Skeleton skeleton = EntityType.SKELETON.create(helper.getLevel());
        skeleton.setPos(helper.absoluteVec(new Vec3(1.5D, 1.0D, 1.5D)));
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        // Le 5 % de squelettes ninjas ferait échouer ce test au hasard.
        withMobGear(new MobGearDefinition(9.5D, 0.0D, 0.0D, 0.0D, false, 0.0D), () -> helper.getLevel().addFreshEntity(skeleton));
        helper.assertTrue(skeleton.getMainHandItem().getItem() instanceof WeaponBowItem bow && bow.getMaterial() != WeaponMaterials.WOODEN && bow.getMaterial() != WeaponMaterials.GOLDEN,
                "a skeleton should get a bow of the mod, at least in stone");
        helper.assertTrue(skeleton.canFireProjectileWeapon((ProjectileWeaponItem) skeleton.getMainHandItem().getItem()), "a skeleton should be able to fire it");
        helper.assertTrue(skeleton.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof RangedBowAttackGoal), "a skeleton should shoot, not fight in melee");

        // Tir réel : la flèche d'un squelette est accélérée par le matériau de son arc (moyenne, car les tirs des mobs sont imprécis).
        Pig shooterTarget = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 3));
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.bow(WeaponMaterials.WOODEN).get()));
        double woodenShots = averageMobShot(helper, skeleton, shooterTarget);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.bow(WeaponMaterials.NETHERITE).get()));
        double netheriteShots = averageMobShot(helper, skeleton, shooterTarget);
        helper.assertTrue(woodenShots < 1.9D && netheriteShots > 1.9D, "mob arrows: wooden " + woodenShots + ", netherite " + netheriteShots);

        Pillager pillager = EntityType.PILLAGER.create(helper.getLevel());
        pillager.setPos(helper.absoluteVec(new Vec3(3.5D, 1.0D, 3.5D)));
        pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        helper.getLevel().addFreshEntity(pillager);
        helper.assertTrue(pillager.getMainHandItem().getItem() instanceof WeaponCrossbowItem, "a pillager should get a crossbow of the mod");
        helper.assertTrue(pillager.canFireProjectileWeapon((ProjectileWeaponItem) pillager.getMainHandItem().getItem()), "a pillager should be able to fire it");

        Piglin piglin = EntityType.PIGLIN.create(helper.getLevel());
        helper.assertTrue(piglin.canFireProjectileWeapon(ModItems.crossbow(WeaponMaterials.IRON).get()), "a piglin should be able to fire it");
        helper.assertFalse(skeleton.canFireProjectileWeapon(ModItems.crossbow(WeaponMaterials.IRON).get()), "a skeleton does not use crossbows");

        // Tir d'un mob : deux fois plus rapide, et la marge en hauteur de 20 % devient 5 %.
        Vec3 aimed = new Vec3(0.0D, 0.2D, 1.0D).normalize().scale(1.6D);
        Vec3 faster = RangedWeapon.mobShotVelocity(aimed, 2.0D);
        helper.assertTrue(Math.abs(faster.length() - 3.2D) < EPSILON, "the arrow should be twice as fast");
        helper.assertTrue(Math.abs(faster.y / faster.horizontalDistance() - 0.05D) < EPSILON, "the aim should be lowered, slope " + faster.y / faster.horizontalDistance());

        // Une flèche tirée à l'arbalète du mod compte comme tirée à l'arbalète.
        Arrow bolt = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), new ItemStack(ModItems.crossbow(WeaponMaterials.IRON).get()));
        helper.assertTrue(bolt.shotFromCrossbow(), "an arrow fired by a crossbow of the mod should count as fired by a crossbow");

        // Conversion et prix : un meilleur matériau coûte plus cher.
        ItemStack vanillaBow = new ItemStack(Items.BOW);
        vanillaBow.setDamageValue(192);
        ItemStack converted = VanillaGear.convert(vanillaBow, WeaponMaterials.DIAMOND);
        helper.assertTrue(converted.is(ModItems.bow(WeaponMaterials.DIAMOND).get()) && Math.abs((double) converted.getDamageValue() / converted.getMaxDamage() - 0.5D) < 0.01D, "a converted bow keeps its wear");
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(Items.CROSSBOW), 12, 10, 0.05F);
        MerchantOffer diamond = VanillaGearEvents.convertOffer(offer, WeaponMaterials.DIAMOND);
        helper.assertTrue(diamond.getResult().is(ModItems.crossbow(WeaponMaterials.DIAMOND).get()) && diamond.getItemCostA().count() == 9, "a diamond crossbow should cost 3 times more");

        // Vrais échanges : l'armurier et le fléchier montent jusqu'au diamant, jamais à la netherite.
        Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        boolean masterSellsDiamond = false;
        for (VillagerProfession profession : List.of(VillagerProfession.ARMORER, VillagerProfession.FLETCHER)) {
            for (var level : VillagerTrades.TRADES.get(profession).int2ObjectEntrySet()) {
                for (VillagerTrades.ItemListing listing : level.getValue()) {
                    MerchantOffer trade = listing.getOffer(villager, helper.getLevel().getRandom());
                    if (trade == null) continue;
                    ItemStack sold = trade.getResult();
                    helper.assertFalse(sold.is(ModItems.shield(WeaponMaterials.NETHERITE).get()) || sold.is(ModItems.crossbow(WeaponMaterials.NETHERITE).get())
                            || sold.is(ModItems.bow(WeaponMaterials.NETHERITE).get()), profession + " should not sell netherite");
                    masterSellsDiamond |= level.getIntKey() == 5 && sold.is(ModItems.crossbow(WeaponMaterials.DIAMOND).get());
                }
            }
        }
        helper.assertTrue(masterSellsDiamond, "a master fletcher should sell a diamond crossbow");
        helper.succeed();
    }

    /**
     * Dual wield réservé aux armes qui ont l'option : les deux mains doivent l'avoir, une arme à deux mains ne l'a
     * jamais, et une épée vanilla ne se manie plus par paire.
     */
    @GameTest(template = ARENA)
    public static void dualWieldOption(GameTestHelper helper) {
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            boolean expected = definition.dualWielded() && !definition.twoHanded();
            helper.assertTrue(DualWieldSystem.isDualWielding(player(helper, weapon(type), weapon(type))) == expected,
                    type.id() + ": a pair should " + (expected ? "" : "not ") + "dual wield");
        }
        helper.assertTrue(DualWieldSystem.isDualWielding(player(helper, weapon(WeaponType.DAGGER), weapon(WeaponType.RAPIER))),
                "two different dual-wielded weapons should work");
        helper.assertFalse(DualWieldSystem.isDualWielding(player(helper, weapon(WeaponType.DAGGER), weapon(WeaponType.BROADSWORD))),
                "a dagger and a broadsword should not dual wield");
        helper.assertFalse(DualWieldSystem.isDualWielding(player(helper, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_SWORD))),
                "two vanilla swords should not dual wield");
        helper.assertTrue(DualWieldSystem.isMeleeWeapon(new ItemStack(Items.IRON_SWORD)), "a vanilla sword stays a melee weapon");

        // Le saï pare avec un saï dans l'autre main, pas avec une arme qui n'est pas ambidextre.
        ItemStack sai = weapon(WeaponType.SAI);
        Player withBroadsword = player(helper, sai, weapon(WeaponType.BROADSWORD));
        helper.assertFalse(sai.use(helper.getLevel(), withBroadsword, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "a sai should not parry with a broadsword in the other hand");
        helper.succeed();
    }

    /**
     * Interrupteurs : chaque système se coupe seul, l'interrupteur général et un mod de combat les coupent tous,
     * les bonus d'une arme déjà en main suivent le changement, et le combat redevient vanilla.
     */
    @GameTest(template = ARENA)
    public static void combatSwitches(GameTestHelper helper) {
        RulesDefinition all = RulesDefinition.FALLBACK;
        helper.assertTrue(all.reachActive() && all.sweepActive() && all.armorPenetrationActive() && all.onHitEffectsActive()
                && all.shieldDisablingActive() && all.twoHandedActive() && all.dualWieldActive() && all.parryActive()
                && all.perfectParryActive() && all.guardBreakActive() && all.dashActive() && all.combosActive(), "everything is active by default");

        Predicate<RulesDefinition> anythingActive = rules -> rules.reachActive() || rules.sweepActive() || rules.armorPenetrationActive()
                || rules.onHitEffectsActive() || rules.shieldDisablingActive() || rules.twoHandedActive() || rules.dualWieldActive()
                || rules.parryActive() || rules.perfectParryActive() || rules.guardBreakActive() || rules.dashActive() || rules.combosActive();
        helper.assertFalse(anythingActive.test(rules(false, true)), "the main switch should turn everything off");
        try {
            CombatCompat.simulateForTests(true);
            helper.assertFalse(anythingActive.test(all), "a combat mod should turn everything off");
        } finally {
            CombatCompat.simulateForTests(false);
        }
        RulesDefinition noParry = new RulesDefinition(1, true, 1.5D, 0.25D, true, true, true, true, true, true, true, false, true, true, true, true, false, 60.0D);
        helper.assertFalse(noParry.perfectParryActive() || noParry.guardBreakActive(), "no perfect parry nor guard break without the parry");
        helper.assertTrue(noParry.dashActive(), "turning the parry off should not touch the dash");

        WeaponData.Snapshot original = WeaponData.current();
        ItemStack katana = weapon(WeaponType.KATANA);
        ItemStack sai = weapon(WeaponType.SAI);
        Player twoHands = player(helper, katana, ItemStack.EMPTY);
        Player dual = player(helper, weapon(WeaponType.DAGGER), weapon(WeaponType.DAGGER));
        Pig target = armoredPig(helper, new BlockPos(2, 1, 2));
        double reachBefore = twoHands.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        helper.assertTrue(reachBefore == WeaponData.weapon(WeaponType.KATANA).attackReach(), "the katana's reach should apply at first");
        try {
            WeaponData.install(new WeaponData.Snapshot(original.weapons(), rules(false, true), original.gear()));
            WeaponItem.refreshEquippedModifiers(twoHands);
            helper.assertTrue(twoHands.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == 3.0D,
                    "main switch off: the held weapon should fall back to the vanilla reach");
            helper.assertFalse(DualWieldSystem.isDualWielding(dual), "main switch off: no dual wield");
            helper.assertFalse(WeaponItem.isTwoHanded(katana), "main switch off: no two-handed weapon");
            helper.assertTrue(ParrySystem.parryOf(sai).isEmpty(), "main switch off: no parry");
            helper.assertTrue(ComboSystem.activeCombo(twoHands).isEmpty(), "main switch off: no combo");
            ItemStack hammer = weapon(WeaponType.HAMMER);
            helper.assertFalse(((WeaponItem) hammer.getItem()).canDisableShield(hammer, new ItemStack(Items.SHIELD), target, twoHands), "main switch off: shields stay up");
            AABB zone = ((WeaponItem) hammer.getItem()).getSweepHitBox(hammer, twoHands, target);
            helper.assertTrue(Math.abs(zone.getXsize() - (target.getBoundingBox().getXsize() + 2.0D)) < EPSILON, "main switch off: vanilla sweep zone");
        } finally {
            WeaponData.install(original);
            WeaponItem.refreshEquippedModifiers(twoHands);
        }
        helper.assertTrue(twoHands.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == reachBefore, "the reach should be back");
        helper.succeed();
    }

    /** Règles par défaut, en changeant l'interrupteur général et celui de la parade. */
    private static RulesDefinition rules(boolean main, boolean parry) {
        return new RulesDefinition(1, main, 1.5D, 0.25D, true, true, true, true, true, true, true, parry, true, true, true, true, false, 60.0D);
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

    /** "Tous les mondes" : une valeur venue d'un datapack du monde ne passe pas dans la config, ce qui était déjà dans la config reste. */
    @GameTest(template = ARENA)
    public static void editorAllWorldsIgnoresWorldValues(GameTestHelper helper) {
        EditorDocument document = EditorDocument.weapon(WeaponType.GLAIVE);
        FieldSet fields = document.fields();
        JsonObject defaults = document.defaults();
        double speed = defaults.get("attack_speed").getAsDouble();
        double reach = defaults.get("attack_reach").getAsDouble();

        JsonObject configFolder = defaults.deepCopy();
        configFolder.addProperty("attack_reach", reach + 0.25D);
        JsonObject effective = configFolder.deepCopy();
        effective.addProperty("attack_speed", speed + 0.5D);

        JsonObject untouched = EditorService.globalDifferences(fields, effective, effective, configFolder, defaults);
        helper.assertFalse(untouched.has("attack_speed"), "the value of the world data pack should stay out of the global config");
        helper.assertTrue(untouched.has("attack_reach"), "the value already in the config should be kept");

        JsonObject requested = effective.deepCopy();
        requested.addProperty("attack_speed", speed + 1.0D);
        JsonObject changed = EditorService.globalDifferences(fields, requested, effective, configFolder, defaults);
        helper.assertTrue(changed.has("attack_speed") && changed.get("attack_speed").getAsDouble() == speed + 1.0D, "a value the user changed should be written");
        helper.succeed();
    }

    /** Un clic sur Enregistrer ne recharge les données qu'une fois, au dernier fichier du lot. */
    @GameTest(template = ARENA)
    public static void editorReloadsOncePerBatch(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        EditorDocument glaive = EditorDocument.weapon(WeaponType.GLAIVE);
        EditorDocument katana = EditorDocument.weapon(WeaponType.KATANA);
        JsonObject originalGlaive = glaive.json(WeaponData.current());
        JsonObject originalKatana = katana.json(WeaponData.current());
        double reach = originalGlaive.get("attack_reach").getAsDouble();
        List<EditorService.Outcome> outcomes = new ArrayList<>();
        try {
            JsonObject editedGlaive = originalGlaive.deepCopy();
            editedGlaive.addProperty("attack_reach", reach + 0.25D);
            EditorService.save(server, glaive.id(), EditorTarget.ALL_WORLDS, editedGlaive.toString(), false, outcomes::add);
            helper.assertTrue(outcomes.isEmpty() && WeaponData.weapon(WeaponType.GLAIVE).attackReach() == reach, "nothing should reload before the last file");
            JsonObject editedKatana = originalKatana.deepCopy();
            editedKatana.addProperty("attack_reach", originalKatana.get("attack_reach").getAsDouble() + 0.25D);
            EditorService.save(server, katana.id(), EditorTarget.ALL_WORLDS, editedKatana.toString(), true, outcomes::add);
            helper.assertTrue(outcomes.size() == 2 && outcomes.stream().allMatch(EditorService.Outcome::success), "both files should be confirmed: " + outcomes);
            helper.assertTrue(WeaponData.weapon(WeaponType.GLAIVE).attackReach() == reach + 0.25D, "the batch should apply once the last file arrives");
        } finally {
            EditorService.save(server, glaive.id(), EditorTarget.ALL_WORLDS, originalGlaive.toString(), outcome -> { });
            EditorService.save(server, katana.id(), EditorTarget.ALL_WORLDS, originalKatana.toString(), outcome -> { });
        }
        helper.succeed();
    }

    /** Coup refusé par la cible : ni main gauche, ni zone, ni usure, et le combo n'avance pas. Un bateau ne le fait pas avancer non plus. */
    @GameTest(template = ARENA)
    public static void comboNeedsALandedHit(GameTestHelper helper) {
        int tested = 0;
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            ComboDefinition combo = definition.combo();
            int areaStep = firstStep(combo, ComboStep::hasArea);
            if (!combo.enabled() || areaStep < 0) continue;
            ComboStep area = combo.steps().get(areaStep);

            Vec3 origin = new Vec3(2.5D, 1.0D, 0.5D);
            ItemStack offhand = definition.dualWielded() ? weapon(type) : ItemStack.EMPTY;
            Player player = player(helper, weapon(type), offhand);
            player.setPos(helper.absoluteVec(origin));
            player.setYRot(0.0F);
            player.setYHeadRot(0.0F);
            double sideAngle = Math.toRadians(Math.min(20.0D, area.angle() / 4.0D));
            double sideDistance = area.radius() * 0.7D;
            Pig target = pigAt(helper, origin.add(0.0D, 0.0D, 1.5D));
            Pig nearby = pigAt(helper, origin.add(Math.sin(sideAngle) * sideDistance, 0.0D, Math.cos(sideAngle) * sideDistance));
            target.setInvulnerable(true);

            PlayerCombatState state = PlayerCombatState.of(player);
            state.comboType = type;
            state.comboStep = areaStep;
            state.comboExpiresAt = helper.getLevel().getGameTime() + 200L;
            fullyCharge(player);
            player.attack(target);
            helper.assertFalse(isHurt(nearby), type.id() + ": the area should not fire after a refused hit");
            helper.assertTrue(player.getOffhandItem().getDamageValue() == 0, type.id() + ": a refused hit should not wear the off-hand weapon");
            helper.assertTrue(state.comboStep == areaStep, type.id() + ": a refused hit should not advance the combo, step " + state.comboStep);

            Boat boat = new Boat(helper.getLevel(), player.getX(), player.getY(), player.getZ() + 1.0D);
            helper.getLevel().addFreshEntity(boat);
            fullyCharge(player);
            player.attack(boat);
            boat.discard();
            helper.assertTrue(state.comboStep == areaStep, type.id() + ": a boat should not advance the combo, step " + state.comboStep);

            target.setInvulnerable(false);
            heal(target);
            heal(nearby);
            fullyCharge(player);
            player.attack(target);
            helper.assertTrue(isHurt(target) && isHurt(nearby), type.id() + ": a landed area hit should hurt both pigs");
            helper.assertTrue(state.comboStep == (areaStep + 1) % combo.steps().size(), type.id() + ": a landed hit should advance the combo");

            target.discard();
            nearby.discard();
            tested++;
        }
        helper.assertTrue(tested > 0, "no weapon has an area combo");
        helper.succeed();
    }

    /** Parade parfaite : un trident ou un shuriken récupérable ne change pas de propriétaire, le shuriken d'un mob si. */
    @GameTest(template = ARENA)
    public static void reflectionKeepsOwnedProjectiles(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        Player defender = shieldBearer(helper, new ItemStack(ModItems.shield(WeaponMaterials.IRON).get()), position);
        Pig shooter = pigAt(helper, new Vec3(2.5D, 1.0D, 4.5D));

        ThrownTrident trident = new ThrownTrident(helper.getLevel(), shooter, new ItemStack(Items.TRIDENT));
        trident.setPos(position.add(0.0D, 1.0D, 1.0D));
        trident.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertFalse(ShieldSystem.tryReflectArrow(trident, defender), "a trident should not be reflected");
        helper.assertTrue(trident.getOwner() == shooter, "a trident should keep its owner");

        ItemStack stack = new ItemStack(ModItems.shuriken(WeaponMaterials.IRON).get());
        Player thrower = thrower(helper, stack, GameType.SURVIVAL);
        ShurikenEntity recoverable = new ShurikenEntity(helper.getLevel(), thrower, stack, true);
        recoverable.setPos(position.add(0.0D, 1.0D, 1.0D));
        recoverable.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertFalse(ShieldSystem.tryReflectArrow(recoverable, defender), "a recoverable shuriken should not be reflected");
        helper.assertTrue(recoverable.getOwner() == thrower, "its thrower should still be able to pick it up");

        ShurikenEntity fromMob = new ShurikenEntity(helper.getLevel(), shooter, stack.copyWithCount(1), false);
        fromMob.setPos(position.add(0.0D, 1.0D, 1.0D));
        fromMob.setDeltaMovement(0.0D, 0.0D, -2.0D);
        helper.assertTrue(ShieldSystem.tryReflectArrow(fromMob, defender), "the shuriken of a mob can be reflected");
        helper.succeed();
    }

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

    /** Un bouclier levé avant de prendre une arme à deux mains est baissé ; une main principale vide ne le fait pas. */
    @GameTest(template = ARENA)
    public static void twoHandedStopsRaisedOffhand(GameTestHelper helper) {
        WeaponType twoHanded = null;
        for (WeaponType type : WeaponType.values()) {
            if (WeaponData.weapon(type).twoHanded()) twoHanded = type;
        }
        helper.assertTrue(twoHanded != null, "no two-handed weapon");
        Player player = player(helper, ItemStack.EMPTY, new ItemStack(Items.SHIELD));
        player.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(player.isUsingItem(), "the shield should be raised");
        TwoHandedSystem.stopLockedOffhandUse(player);
        helper.assertTrue(player.isUsingItem(), "an empty main hand does not lock the off hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon(twoHanded));
        TwoHandedSystem.stopLockedOffhandUse(player);
        helper.assertFalse(player.isUsingItem(), "a two-handed weapon should lower the shield");
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

    /** Momentum : la réduction de recharge reste bornée, même à un niveau obtenu par commande. */
    @GameTest(template = ARENA)
    public static void momentumStaysBounded(GameTestHelper helper) {
        helper.assertTrue(Math.abs(ModEnchantments.momentumMultiplier(1) - 0.85D) < EPSILON, "level 1");
        helper.assertTrue(ModEnchantments.momentumMultiplier(50) == 0.25D, "a huge level should be capped");
        helper.succeed();
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

    /** Les shurikens cassés en attente sont sauvegardés avec le joueur. */
    @GameTest(template = ARENA)
    public static void brokenShurikensAreSaved(GameTestHelper helper) {
        java.util.UUID batch = java.util.UUID.randomUUID();
        Player player = thrower(helper, ItemStack.EMPTY, GameType.SURVIVAL);
        PlayerCombatState.of(player).brokenShurikens = List.of(batch);
        net.minecraft.nbt.CompoundTag tag = player.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        Player loaded = thrower(helper, ItemStack.EMPTY, GameType.SURVIVAL);
        loaded.load(tag);
        helper.assertTrue(PlayerCombatState.of(loaded).brokenShurikens.equals(List.of(batch)), "the list should survive a save: " + PlayerCombatState.of(loaded).brokenShurikens);
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

    /** Le prochain coup du joueur sera chargé à fond. */
    private static void fullyCharge(Player player) {
        ((LivingEntityAccessor) player).betterweaponry$setAttackStrengthTicker(1000);
    }

    private static int firstStep(ComboDefinition combo, java.util.function.Predicate<ComboStep> condition) {
        for (int i = 0; i < combo.steps().size(); i++) {
            if (condition.test(combo.steps().get(i))) return i;
        }
        return -1;
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
