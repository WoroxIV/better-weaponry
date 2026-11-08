package com.betterweaponry.tutorial;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.Docs;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.ComboStep;
import com.betterweaponry.data.DashDefinition;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModItems;
import com.betterweaponry.tutorial.TutorialScene.ActorKind;
import com.betterweaponry.tutorial.TutorialScene.Hand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Les tutoriels. Chaque scène est construite à son ouverture, avec les réglages en vigueur : une portée ou une réduction
 * modifiée dans l'éditeur se retrouve dans la démonstration.
 */
public final class TutorialScenes {
    static final double LABEL_HEIGHT = 2.35D;
    /** Les dégâts partent plus bas et à côté : ils ne recouvrent pas les textes fixes au-dessus du personnage. */
    static final double RISE_START_HEIGHT = 1.9D;
    /** Second texte au-dessus d'un personnage, par exemple un temps de recharge. */
    private static final double SECOND_LABEL_HEIGHT = 2.85D;
    static final int RISE_TICKS = 24;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final int GOLD = 0xFFFFAA00;
    private static final int CYAN = 0xFF55FFFF;
    private static final int ZONE_WHITE = 0x80FFFFFF;
    private static final int ZONE_GREEN = 0x8055FF55;
    private static final int ZONE_GOLD = 0x90FFAA00;
    private static final String PLAYER = "player";
    private static final String TARGET = "target";
    private static final double PERCENT = 100.0D;
    private static final double TICKS_PER_SECOND = 20.0D;
    /** La parade parfaite dure quelques ticks : sa scène est jouée trois fois plus lentement pour qu'on la voie. */
    public static final int SLOW_MOTION = 3;
    /** Le coup touche quelques ticks après le début du mouvement. */
    private static final int HIT_DELAY = 3;
    /** Bord de la plateforme : un personnage plus loin serait dans le vide. */
    private static final double MAX_DISTANCE = 4.0D;
    /** Armure complète en fer. */
    private static final float IRON_ARMOR = 15.0F;

    private static final Map<String, Supplier<TutorialScene>> SCENES = Map.of(
            "two_handed", TutorialScenes::twoHanded,
            "dual_wield", TutorialScenes::dualWield,
            "parry", TutorialScenes::parry,
            "perfect_parry", TutorialScenes::perfectParry,
            "guard_break", TutorialScenes::guardBreak,
            "reach", TutorialScenes::reach,
            "attack_zone", TutorialScenes::attackZone,
            "armor_penetration", TutorialScenes::armorPenetration,
            "dash", TutorialScenes::dash,
            "combos", TutorialScenes::combos);

    /** Ordre d'affichage dans la page Tutoriels. */
    public static final List<String> IDS = List.of("two_handed", "dual_wield", "parry", "perfect_parry", "guard_break", "reach",
            "attack_zone", "armor_penetration", "dash", "combos");

    /** Options de l'éditeur qui ont un bouton « ? » : chemin de l'option → tutoriel. */
    private static final Map<String, String> FIELD_LINKS = Map.ofEntries(
            Map.entry("two_handed_enabled", "two_handed"), Map.entry("two_handed", "two_handed"),
            Map.entry("dual_wield_enabled", "dual_wield"), Map.entry("dual_wielded", "dual_wield"),
            Map.entry("dual_wield_speed_multiplier", "dual_wield"),
            Map.entry("parry_enabled", "parry"), Map.entry("parry", "parry"), Map.entry("parry.damage_reduction", "parry"),
            Map.entry("perfect_parry_enabled", "perfect_parry"), Map.entry("parry.perfect_window_seconds", "perfect_parry"),
            Map.entry("shield_perfect_window_seconds", "perfect_parry"),
            Map.entry("guard_break_enabled", "guard_break"), Map.entry("parry.guard_break_seconds", "guard_break"),
            Map.entry("reach_enabled", "reach"), Map.entry("attack_reach", "reach"),
            Map.entry("sweep_enabled", "attack_zone"), Map.entry("sweep", "attack_zone"),
            Map.entry("armor_penetration_enabled", "armor_penetration"), Map.entry("armor_penetration", "armor_penetration"),
            Map.entry("dash_enabled", "dash"), Map.entry("dash", "dash"),
            Map.entry("combos_enabled", "combos"), Map.entry("combo", "combos"));

    private TutorialScenes() {
    }

    /** Scène de ce nom, construite avec les réglages actuels. */
    public static TutorialScene create(String id) {
        return SCENES.get(id).get();
    }

    /** Tutoriel lié à une option de l'éditeur, ou null si elle n'en a pas. */
    @Nullable
    public static String sceneForField(String fieldKey) {
        return FIELD_LINKS.get(fieldKey);
    }

    public static Map<String, String> fieldLinks() {
        return FIELD_LINKS;
    }

    public static Component title(String id) {
        return Component.translatable(key(id, "title"));
    }

    private static String key(String scene, String part) {
        return BetterWeaponry.MODID + ".tutorial." + scene + "." + part;
    }

    private static Component caption(String scene, String part, Object... args) {
        return Component.translatable(key(scene, part), args);
    }

    private static Component word(String part, Object... args) {
        return Component.translatable(BetterWeaponry.MODID + ".tutorial.label." + part, args);
    }

    private static Component damage(double amount) {
        return Component.literal("-" + Docs.number(amount));
    }

    private static ItemStack weapon(WeaponType type, WeaponMaterial material) {
        return new ItemStack(ModItems.get(type, material).get());
    }

    private static ItemStack iron(WeaponType type) {
        return weapon(type, WeaponMaterials.IRON);
    }

    private static double damageOf(WeaponType type, WeaponMaterial material) {
        return WeaponData.weapon(type).damageFor(material);
    }

    private static double damageOf(WeaponType type) {
        return damageOf(type, WeaponMaterials.IRON);
    }

    private static TutorialScene.Builder duel(String id, ItemStack main, ItemStack off, double distance, ItemStack targetWeapon) {
        return TutorialScene.builder(id)
                .actor(PLAYER, ActorKind.PLAYER, 0.0D, 0.0D, 0.0F, main, off)
                .actor(TARGET, ActorKind.ZOMBIE, 0.0D, distance, 180.0F, targetWeapon, ItemStack.EMPTY);
    }

    /** Un coup qui touche : mouvement du bras, flash de la cible, dégâts qui montent. */
    private static void hit(TutorialScene.Builder scene, String attacker, String target, int tick, Hand hand, double amount) {
        scene.swing(attacker, tick, hand).hurt(target, tick + HIT_DELAY, attacker).riseLabel(target, damage(amount), RED, tick + HIT_DELAY);
    }

    private static TutorialScene twoHanded() {
        String id = "two_handed";
        double damage = damageOf(WeaponType.CLAYMORE);
        ItemStack shield = new ItemStack(ModItems.shield(WeaponMaterials.IRON).get());
        TutorialScene.Builder scene = duel(id, iron(WeaponType.CLAYMORE), ItemStack.EMPTY, 2.4D, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(80, caption(id, "locked"))
                .step(155, caption(id, "one_handed"));
        hit(scene, PLAYER, TARGET, 30, Hand.MAIN, damage);
        scene.equip(PLAYER, 88, true, shield)
                .labelAt(PLAYER, 0.75D, 1.45D, 0.0D, Component.literal("✖"), RED, 94, 135)
                .equip(PLAYER, 128, true, ItemStack.EMPTY)
                .equip(PLAYER, 160, false, iron(WeaponType.BROADSWORD))
                .equip(PLAYER, 166, true, shield);
        hit(scene, PLAYER, TARGET, 190, Hand.MAIN, damageOf(WeaponType.BROADSWORD));
        return scene.duration(245).build();
    }

    private static TutorialScene dualWield() {
        String id = "dual_wield";
        double mainDamage = damageOf(WeaponType.DAGGER, WeaponMaterials.IRON);
        double offDamage = damageOf(WeaponType.DAGGER, WeaponMaterials.DIAMOND);
        double speed = WeaponData.rules().dualWieldSpeedMultiplier();
        TutorialScene.Builder scene = duel(id, iron(WeaponType.DAGGER), weapon(WeaponType.DAGGER, WeaponMaterials.DIAMOND), 2.0D, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(60, caption(id, "alternate"))
                .step(160, caption(id, "speed", Docs.number(speed)))
                .step(240, caption(id, "only_marked"));
        Hand[] hands = {Hand.MAIN, Hand.OFF};
        for (int i = 0; i < 4; i++) {
            hit(scene, PLAYER, TARGET, 72 + i * 20, hands[i % 2], i % 2 == 0 ? mainDamage : offDamage);
        }
        for (int i = 0; i < 5; i++) {
            hit(scene, PLAYER, TARGET, 172 + i * 12, hands[i % 2], i % 2 == 0 ? mainDamage : offDamage);
        }
        scene.label(PLAYER, word("dual_wielded"), GREEN, 248, 315);
        return scene.duration(320).build();
    }

    private static TutorialScene parry() {
        String id = "parry";
        ParryDefinition parry = WeaponData.weapon(WeaponType.KATANA).parry();
        double zombieDamage = 6.0D;
        double reduced = zombieDamage * (1.0D - parry.damageReduction() / PERCENT);
        TutorialScene.Builder scene = duel(id, iron(WeaponType.KATANA), ItemStack.EMPTY, 2.2D, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(70, caption(id, "front", Docs.number(parry.damageReduction())))
                .step(160, caption(id, "behind"))
                .step(255, caption(id, "wear"))
                .guard(PLAYER, 25, 300, Hand.MAIN);
        hit(scene, TARGET, PLAYER, 90, Hand.BOTH, reduced);
        hit(scene, TARGET, PLAYER, 125, Hand.BOTH, reduced);
        scene.walk(TARGET, 165, 200, 0.0D, -2.2D).turn(TARGET, 196, 0.0F);
        hit(scene, TARGET, PLAYER, 222, Hand.BOTH, zombieDamage);
        return scene.duration(310).build();
    }

    private static TutorialScene perfectParry() {
        String id = "perfect_parry";
        ParryDefinition parry = WeaponData.weapon(WeaponType.KATANA).parry();
        int window = (int) Math.round(parry.perfectWindowSeconds() * TICKS_PER_SECOND) * SLOW_MOTION;
        double reduced = 6.0D * (1.0D - parry.damageReduction() / PERCENT);
        double damage = damageOf(WeaponType.KATANA);

        int firstGuard = 70;
        int firstHit = firstGuard + window / 2;
        int secondGuard = 165;
        int secondHit = secondGuard + window + 15;
        TutorialScene.Builder scene = duel(id, iron(WeaponType.KATANA), ItemStack.EMPTY, 2.2D, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(50, caption(id, "timing", Docs.number(parry.perfectWindowSeconds())))
                .step(150, caption(id, "late"))
                .step(250, caption(id, "bonus", Docs.number(parry.perfectDamageBonus())))
                .guard(PLAYER, firstGuard, firstHit + 30, Hand.MAIN)
                .swing(TARGET, firstHit - HIT_DELAY, Hand.BOTH)
                .spark(PLAYER, firstHit, GOLD)
                .label(PLAYER, word("perfect"), GOLD, firstHit, firstHit + 30)
                .bar(55, firstHit + 40, firstGuard, firstGuard + window, firstHit)
                .guard(PLAYER, secondGuard, secondHit + 30, Hand.MAIN)
                .bar(155, secondHit + 35, secondGuard, secondGuard + window, secondHit);
        hit(scene, TARGET, PLAYER, secondHit - HIT_DELAY, Hand.BOTH, reduced);
        hit(scene, PLAYER, TARGET, 275, Hand.MAIN, damage * (1.0D + parry.perfectDamageBonus() / PERCENT));
        scene.label(PLAYER, Component.literal("+" + Docs.number(parry.perfectDamageBonus()) + " %"), GOLD, 262, 300);
        return scene.duration(320).build();
    }

    private static TutorialScene guardBreak() {
        String id = "guard_break";
        ParryDefinition parry = WeaponData.weapon(WeaponType.KATANA).parry();
        double seconds = parry.guardBreakSeconds();
        double reduced = 6.0D * (1.0D - parry.damageReduction() / PERCENT);
        int perfectHit = 195;
        TutorialScene.Builder scene = duel(id, iron(WeaponType.KATANA), ItemStack.EMPTY, 2.2D, new ItemStack(Items.IRON_AXE))
                .step(0, caption(id, "intro"))
                .step(60, caption(id, "broken", Docs.number(seconds)))
                .step(170, caption(id, "perfect"))
                .guard(PLAYER, 20, 100, Hand.MAIN)
                .spark(PLAYER, 99, RED)
                .label(PLAYER, word("guard_broken"), RED, 100, 160)
                .labelAt(PLAYER, 0.0D, SECOND_LABEL_HEIGHT, 0.0D, Component.literal(Docs.number(seconds) + " s"), WHITE, 108, 160)
                .guard(PLAYER, 185, 240, Hand.MAIN)
                .swing(TARGET, perfectHit - HIT_DELAY, Hand.BOTH)
                .spark(PLAYER, perfectHit, GOLD)
                .label(PLAYER, word("perfect"), GOLD, perfectHit, perfectHit + 35);
        hit(scene, TARGET, PLAYER, 96, Hand.BOTH, reduced);
        return scene.duration(255).build();
    }

    private static TutorialScene reach() {
        String id = "reach";
        double shortReach = WeaponData.weapon(WeaponType.DAGGER).attackReach();
        double longReach = WeaponData.weapon(WeaponType.SPEAR).attackReach();
        double distance = Math.min(MAX_DISTANCE, (shortReach + longReach) / 2.0D);
        TutorialScene.Builder scene = duel(id, iron(WeaponType.DAGGER), ItemStack.EMPTY, distance, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(65, caption(id, "short", Docs.number(shortReach)))
                .step(135, caption(id, "long", Docs.number(longReach)))
                .step(220, caption(id, "mobs"))
                .zone(PLAYER, shortReach, 360.0D, ZONE_WHITE, 10, 135)
                .beam(PLAYER, TARGET, WHITE, 10, 220)
                .label(TARGET, word("distance", Docs.number(distance)), WHITE, 20, 80)
                .swing(PLAYER, 85, Hand.MAIN)
                .label(TARGET, word("too_far"), RED, 92, 130)
                .equip(PLAYER, 142, false, iron(WeaponType.SPEAR))
                .zone(PLAYER, longReach, 360.0D, ZONE_GREEN, 142, 220)
                .equip(TARGET, 225, false, iron(WeaponType.SPEAR))
                .zone(TARGET, longReach, 360.0D, ZONE_GREEN, 225, 290);
        hit(scene, PLAYER, TARGET, 170, Hand.MAIN, damageOf(WeaponType.SPEAR));
        hit(scene, TARGET, PLAYER, 250, Hand.BOTH, damageOf(WeaponType.SPEAR));
        return scene.duration(295).build();
    }

    /** Vrai si un point est dans le secteur de ce rayon et de cet angle, devant le joueur placé à l'origine et tourné vers +Z. */
    public static boolean inSector(double x, double z, double radius, double angle) {
        if (x * x + z * z > radius * radius) return false;
        return Math.abs(Math.toDegrees(Math.atan2(x, z))) <= angle / 2.0D;
    }

    private static ComboStep lastStepWithArea(ComboDefinition combo) {
        ComboStep fallback = new ComboStep(false, 3.5D, 180.0D, 1.0D, 0.0D, 1.0D);
        return combo.steps().stream().filter(ComboStep::hasArea).reduce((first, second) -> second).orElse(fallback);
    }

    private static TutorialScene attackZone() {
        String id = "attack_zone";
        WeaponDefinition scythe = WeaponData.weapon(WeaponType.SCYTHE);
        double radius = scythe.sweep().radius();
        ComboStep area = lastStepWithArea(WeaponData.weapon(WeaponType.KATANA).combo());
        double damage = damageOf(WeaponType.SCYTHE);
        double[] near = {1.3D, 3.0D};
        double[] far = {-3.2D, -1.0D};
        TutorialScene.Builder scene = duel(id, iron(WeaponType.SCYTHE), ItemStack.EMPTY, 2.4D, ItemStack.EMPTY)
                .actor("near", ActorKind.ZOMBIE, near[0], near[1], 180.0F, ItemStack.EMPTY, ItemStack.EMPTY)
                .actor("far", ActorKind.ZOMBIE, far[0], far[1], 90.0F, ItemStack.EMPTY, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(90, caption(id, "radius", Docs.number(radius)))
                .step(165, caption(id, "area", Docs.number(area.radius()), Docs.number(area.angle())))
                .zone(TARGET, radius, 360.0D, ZONE_GREEN, 15, 165);
        hit(scene, PLAYER, TARGET, 45, Hand.MAIN, damage);
        scene.hurt("near", 48, TARGET).riseLabel("near", damage(damage * scythe.sweep().damageRatio() / PERCENT), RED, 48);
        scene.equip(PLAYER, 172, false, iron(WeaponType.KATANA)).zone(PLAYER, area.radius(), area.angle(), ZONE_GOLD, 175, 262);
        int areaHit = 215;
        double areaDamage = damageOf(WeaponType.KATANA) * area.damageMultiplier();
        hit(scene, PLAYER, TARGET, areaHit, Hand.MAIN, areaDamage);
        if (inSector(near[0], near[1], area.radius(), area.angle())) {
            scene.hurt("near", areaHit + HIT_DELAY, PLAYER).riseLabel("near", damage(areaDamage), RED, areaHit + HIT_DELAY);
        }
        if (inSector(far[0], far[1], area.radius(), area.angle())) {
            scene.hurt("far", areaHit + HIT_DELAY, PLAYER).riseLabel("far", damage(areaDamage), RED, areaHit + HIT_DELAY);
        }
        return scene.duration(270).build();
    }

    /**
     * Dégâts après l'armure (fer complet), comme le jeu les calcule : la pénétration réduit la part que l'armure retire.
     *
     * @param penetration en pourcentage
     */
    public static double damageThroughArmor(double damage, double penetration) {
        double absorbed = Math.max(IRON_ARMOR - damage / 2.0D, IRON_ARMOR * 0.2D);
        double reduction = damage * Math.min(absorbed, 20.0D) / 25.0D;
        return damage - reduction * (1.0D - Math.min(100.0D, penetration) / PERCENT);
    }

    private static TutorialScene armorPenetration() {
        String id = "armor_penetration";
        WeaponDefinition hammer = WeaponData.weapon(WeaponType.HAMMER);
        double damage = hammer.damageFor(WeaponMaterials.IRON);
        double withoutPenetration = damageThroughArmor(damage, 0.0D);
        double withPenetration = damageThroughArmor(damage, hammer.armorPenetration());
        TutorialScene.Builder scene = duel(id, iron(WeaponType.HAMMER), ItemStack.EMPTY, 2.8D, ItemStack.EMPTY)
                .armored(TARGET)
                .step(0, caption(id, "intro", Docs.number(damage)))
                .step(60, caption(id, "armor", Docs.number(withoutPenetration)))
                .step(135, caption(id, "pierce", Docs.number(hammer.armorPenetration()), Docs.number(withPenetration)))
                .step(215, caption(id, "comparison"));
        hit(scene, PLAYER, TARGET, 85, Hand.MAIN, withoutPenetration);
        scene.label(TARGET, word("no_penetration"), WHITE, 75, 125);
        hit(scene, PLAYER, TARGET, 165, Hand.MAIN, withPenetration);
        scene.label(TARGET, Component.literal(Docs.number(hammer.armorPenetration()) + " %"), GREEN, 150, 205);
        return scene.duration(270).build();
    }

    private static TutorialScene dash() {
        String id = "dash";
        DashDefinition rapier = WeaponData.weapon(WeaponType.RAPIER).dash();
        DashDefinition dagger = WeaponData.weapon(WeaponType.DAGGER).dash();
        double start = -3.0D;
        double end = Math.min(MAX_DISTANCE - 1.0D, start + rapier.distance());
        double side = Math.min(MAX_DISTANCE - 0.5D, dagger.distance());
        TutorialScene.Builder scene = TutorialScene.builder(id)
                .actor(PLAYER, ActorKind.PLAYER, 0.0D, start, 0.0F, iron(WeaponType.RAPIER), ItemStack.EMPTY)
                .actor(TARGET, ActorKind.ZOMBIE, 0.0D, 3.2D, 180.0F, ItemStack.EMPTY, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(100, caption(id, "cooldown", Docs.number(rapier.cooldownSeconds())))
                .step(185, caption(id, "direction"))
                .dash(PLAYER, 40, 48, 0.0D, end)
                .trail(0.0D, start, 0.0D, end, CYAN, 40, 75)
                .label(PLAYER, word("distance", Docs.number(rapier.distance())), CYAN, 50, 95)
                .labelAt(PLAYER, 0.0D, LABEL_HEIGHT, 0.0D, Component.literal(Docs.number(rapier.cooldownSeconds()) + " s"), WHITE, 110, 170)
                .equip(PLAYER, 192, false, iron(WeaponType.DAGGER))
                .turn(PLAYER, 200, 90.0F)
                .dash(PLAYER, 212, 220, -side, end)
                .trail(0.0D, end, -side, end, CYAN, 212, 245);
        hit(scene, PLAYER, TARGET, 75, Hand.MAIN, damageOf(WeaponType.RAPIER));
        return scene.duration(265).build();
    }

    private static TutorialScene combos() {
        String id = "combos";
        ComboDefinition combo = WeaponData.weapon(WeaponType.KATANA).combo();
        ComboStep area = lastStepWithArea(combo);
        double damage = damageOf(WeaponType.KATANA);
        int total = Math.max(1, combo.steps().size());
        TutorialScene.Builder scene = duel(id, iron(WeaponType.KATANA), ItemStack.EMPTY, 2.2D, ItemStack.EMPTY)
                .actor("near", ActorKind.ZOMBIE, -1.6D, 2.0D, 180.0F, ItemStack.EMPTY, ItemStack.EMPTY)
                .step(0, caption(id, "intro"))
                .step(50, caption(id, "hits"))
                .step(125, caption(id, "finisher", Docs.number(area.radius()), Docs.number(area.angle())))
                .step(200, caption(id, "reset", Docs.number(combo.resetAfterSeconds())));
        int[] hits = {62, 90};
        for (int i = 0; i < hits.length; i++) {
            hit(scene, PLAYER, TARGET, hits[i], Hand.MAIN, damage);
            scene.labelAt(PLAYER, 0.0D, LABEL_HEIGHT, 0.0D, Component.literal((i + 1) + " / " + total), GOLD, hits[i], hits[i] + 24);
        }
        int finisher = 145;
        double finisherDamage = damage * area.damageMultiplier();
        hit(scene, PLAYER, TARGET, finisher, Hand.MAIN, finisherDamage);
        scene.labelAt(PLAYER, 0.0D, LABEL_HEIGHT, 0.0D, Component.literal(total + " / " + total), GOLD, finisher, finisher + 30)
                .zone(PLAYER, area.radius(), area.angle(), ZONE_GOLD, 130, 190);
        if (inSector(-1.6D, 2.0D, area.radius(), area.angle())) {
            scene.hurt("near", finisher + HIT_DELAY, PLAYER).riseLabel("near", damage(finisherDamage), RED, finisher + HIT_DELAY);
        }
        scene.label(PLAYER, word("combo_reset"), WHITE, 235, 275);
        return scene.duration(280).build();
    }
}
