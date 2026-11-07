package com.betterweaponry.gametest;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.data.DataProblem;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModItems;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests automatiques du combat, lancés par {@code gradlew runGameTestServer} (ou {@code /test runall} en jeu, en dev).
 * Ils lisent les stats actuelles des armes : ils restent valables si les valeurs par défaut changent.
 * Ils ne tournent jamais dans une partie normale (les GameTests ne sont actifs qu'en développement).
 */
@GameTestHolder(BetterWeaponry.MODID)
@PrefixGameTestTemplate(false)
public final class CombatGameTests {
    /** Structure vide (data/better_weaponry/structure/empty.nbt) qui sert de terrain. */
    private static final String ARENA = "empty";
    private static final double EPSILON = 0.01D;
    private static final float TEST_HEALTH = 100.0F;
    private static final float RAW_DAMAGE = 10.0F;
    private static final int VANILLA_INVULNERABILITY_TICKS = 20;

    private CombatGameTests() {
    }

    /** Fusion des couches : valeur invalide ignorée, sous-champ fusionné, fautes de frappe signalées avec suggestion. */
    @GameTest(template = ARENA)
    public static void dataLayersMerge(GameTestHelper helper) {
        JsonObject defaults = json("{ \"armor_penetration\": 10, \"sweep\": { \"enabled\": true, \"radius\": 1.0, \"damage_ratio\": 0 },"
                + " \"on_hit_effects\": [ { \"effect\": \"minecraft:slowness\" } ] }");
        JsonObject user = json("{ \"armor_penetration\": 150, \"sweep\": { \"radus\": 3, \"radius\": 2.5 },"
                + " \"on_hit_effects\": [ { \"effect\": \"minecraft:slownes\" } ], \"atack_speed\": 2 }");
        List<DataProblem> problems = new ArrayList<>();
        JsonObject merged = WeaponDefinition.FIELDS.merge(List.of(new FieldSet.Layer("defaults", defaults), new FieldSet.Layer("user", user)), problems);
        WeaponDefinition definition = WeaponDefinition.CODEC.parse(JsonOps.INSTANCE, merged).getOrThrow();

        helper.assertTrue(definition.armorPenetration() == 10.0D, "armor_penetration 150 must be ignored, got " + definition.armorPenetration());
        helper.assertTrue(definition.sweep().radius() == 2.5D && definition.sweep().enabled(), "sweep must merge field by field: " + definition.sweep());
        helper.assertTrue(definition.onHitEffects().size() == 1, "an invalid effect list must keep the previous list");
        helper.assertTrue(problems.size() == 4, "4 problems expected, got " + problems.stream().map(DataProblem::toLogString).toList());
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
        double expected = armorReduction * (1.0D - penetration);
        helper.assertTrue(Math.abs(reducedArmorReduction - expected) < EPSILON,
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

    // --- Outils ---

    private static JsonObject json(String text) {
        return JsonUtil.parseLenient(text).getAsJsonObject();
    }

    private static ItemStack weapon(WeaponType type) {
        return new ItemStack(ModItems.get(type, WeaponMaterials.IRON).get());
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
    private static Player player(GameTestHelper helper, ItemStack mainHand, ItemStack offHand) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        mainHand.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null && !instance.hasModifier(modifier.id())) instance.addTransientModifier(modifier);
        });
        return player;
    }

    private static void setBase(AttributeInstance attribute, double value) {
        if (attribute != null) attribute.setBaseValue(value);
    }
}
