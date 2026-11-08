package com.betterweaponry.gametest;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.TooltipLayout;
import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.DashAbility;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.PlayerCombatState;
import com.betterweaponry.combat.TwoHandedSystem;
import com.betterweaponry.combat.WeaponProfiles;
import com.betterweaponry.compat.CombatCompat;
import com.betterweaponry.config.TooltipCategory;
import com.betterweaponry.config.TooltipPlacement;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.ComboStep;
import com.betterweaponry.data.DashDefinition;
import com.betterweaponry.data.DashDirection;
import com.betterweaponry.data.DataProblem;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.editor.EditorDocument;
import com.betterweaponry.editor.EditorService;
import com.betterweaponry.editor.EditorTarget;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.mixin.LivingEntityAccessor;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModAttributes;
import com.betterweaponry.registry.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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
            int expectedWear = parry.durabilityPerDamage() <= 0.0D || blocked <= 0.0D
                    ? 0 : Math.max(1, (int) Math.round(blocked * parry.durabilityPerDamage()));
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

    /**
     * Quand le clic droit lance une parade : arme qui pare en main principale, main secondaire vide,
     * arme à deux mains, ou arme en main secondaire avec while_dual_wielding. Un bouclier ou une torche
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
            expectParry(helper, type, dagger, parries && (twoHanded || definition.parry().whileDualWielding()));

            // Avec une arme dans chaque main (saïs), les deux armes prennent la pose de parade.
            ItemStack dualMain = weapon(type);
            Player dualParrier = player(helper, dualMain, dagger.copy());
            dualMain.use(helper.getLevel(), dualParrier, InteractionHand.MAIN_HAND);
            boolean bothHands = parries && !twoHanded && definition.parry().whileDualWielding();
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
            for (DeferredItem<WeaponItem> item : ModItems.allOfType(type)) {
                helper.assertTrue(player.getCooldowns().isOnCooldown(item.get()),
                        type.id() + ": " + item.getId() + " should be on cooldown too");
            }
            float expectedExhaustion = (float) dash.hungerCost() * 4.0F;
            helper.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel() - expectedExhaustion) < EPSILON,
                    type.id() + ": exhaustion " + player.getFoodData().getExhaustionLevel() + " instead of " + expectedExhaustion);
            helper.assertFalse(useMainHand(helper, player, stack), type.id() + ": no second dash during the cooldown");

            expectDash(helper, type, ItemStack.EMPTY, false, dash.allowAirborne(), "in mid-air");
            expectDash(helper, type, weapon(WeaponType.DAGGER), true, true, "with a dagger in the off hand");
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
            if (!combo.enabled() || combo.requiresDualWield() || finisher < 0) continue;
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
            if (!combo.enabled() || !combo.requiresDualWield() || bothHands < 0) continue;

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
                    new RulesDefinition(1, true, 1.5D, 0.25D, true, true, true, true, true, true, true, true, false, true, true, true)));
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
            for (DeferredItem<WeaponItem> item : ModItems.allOfType(WeaponType.KATANA)) {
                helper.assertTrue(defender.getCooldowns().isOnCooldown(item.get()), breaker + ": " + item.getId() + " should be on cooldown");
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
            WeaponData.install(new WeaponData.Snapshot(weapons, original.rules()));

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
            helper.assertTrue(Math.abs(attacker.getAttributeValue(ModAttributes.ARMOR_PENETRATION) - hammer.armorPenetration() / 100.0D) < EPSILON,
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
     * Rangement du tooltip : deux mains au-dessus des stats sans indentation, portée placée avant les dégâts,
     * détails remplacés par l'invite tant que la touche n'est pas maintenue, couleur appliquée, catégorie cachée absente.
     */
    @GameTest(template = ARENA)
    public static void tooltipLayout(GameTestHelper helper) {
        TooltipLayout.Settings defaults = new TooltipLayout.Settings(TooltipCategory::defaultPlacement, TooltipCategory::defaultColor);
        Component hint = Component.literal("hint");

        List<Component> closed = arrangedTooltip(defaults, false, hint);
        helper.assertTrue(texts(closed).equals(List.of("Weapon", "two", "", "When in Main Hand:",
                " reach", " Attack Damage", " Attack Speed", "hint", "ID")),
                "wrong layout without the key: " + texts(closed));
        helper.assertTrue(closed.get(1).getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY)),
                "the hands line should be dark gray");
        helper.assertTrue(closed.get(4).getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN)),
                "the stats should be dark green");

        List<Component> open = arrangedTooltip(defaults, true, hint);
        helper.assertTrue(texts(open).equals(List.of("Weapon", "two", "", "When in Main Hand:",
                " reach", " Attack Damage", " Attack Speed", " parry", "ID")),
                "wrong layout with the key: " + texts(open));

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
        RulesDefinition noParry = new RulesDefinition(1, true, 1.5D, 0.25D, true, true, true, true, true, true, true, false, true, true, true, true);
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
            WeaponData.install(new WeaponData.Snapshot(original.weapons(), rules(false, true)));
            WeaponItem.refreshEquippedModifiers(twoHands);
            helper.assertTrue(twoHands.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == 3.0D,
                    "main switch off: the held weapon should fall back to the vanilla reach");
            helper.assertFalse(DualWieldSystem.isDualWielding(dual), "main switch off: no dual wield");
            helper.assertFalse(WeaponItem.isTwoHanded(katana), "main switch off: no two-handed weapon");
            helper.assertTrue(ParrySystem.parryOf(sai).isEmpty(), "main switch off: no parry");
            helper.assertTrue(ComboSystem.activeCombo(twoHands).isEmpty(), "main switch off: no combo");
            ItemStack hammer = weapon(WeaponType.HAMMER);
            helper.assertFalse(hammer.getItem().canDisableShield(hammer, new ItemStack(Items.SHIELD), target, twoHands), "main switch off: shields stay up");
            AABB zone = hammer.getSweepHitBox(twoHands, target);
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
        return new RulesDefinition(1, main, 1.5D, 0.25D, true, true, true, true, true, true, true, parry, true, true, true, true);
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

    // --- Outils ---

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
