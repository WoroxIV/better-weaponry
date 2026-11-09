package com.betterweaponry.data;

import com.betterweaponry.item.WeaponMaterial;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Les stats d'un type d'arme, lues dans {@code better_weaponry/weapons/<type>.json}.
 * Les valeurs par défaut du mod sont dans {@code src/main/resources/data/better_weaponry/better_weaponry/weapons/}.
 * <p>
 * Ajouter un champ : le déclarer ci-dessous avec {@link DataField}, l'ajouter au record, au {@link #CODEC} et à
 * {@link #FIELDS}, puis écrire son nom et son aide dans les sources de traduction
 * ({@code better_weaponry.field.weapon.<champ>} et {@code .help}). La validation, le schéma JSON et le README suivent.
 *
 * @param formatVersion    version du format de fichier (1)
 * @param twoHanded        arme à deux mains : main secondaire bloquée et cachée
 * @param dualWielded      peut être maniée avec une autre arme ambidextre dans l'autre main (ignoré si à deux mains)
 * @param attackSpeed      attaques par seconde à pleine puissance (épée vanilla : 1.6)
 * @param attackReach      portée d'attaque en blocs (joueur vanilla : 3)
 * @param baseDamage       dégâts de la version en bois ; chaque matériau les multiplie par son facteur
 * @param materialDamage   dégâts imposés pour certains matériaux (id du matériau → dégâts)
 * @param armorPenetration part de l'armure de la cible ignorée, en % (0 à 100)
 * @param disablesShield   un coup sur un bouclier levé le désactive, comme une hache
 * @param onHitEffects     effets donnés à la cible touchée
 * @param sweep            coup balayé
 * @param ability          capacité du clic droit (une seule par arme)
 * @param parry            réglages de la parade, utilisés si {@code ability} vaut "parry"
 * @param dash             réglages du dash, utilisés si {@code ability} vaut "dash"
 * @param combo            combo : suite de coups qui se répète, avec des coups spéciaux
 * @param items            autres objets qui reprennent les comportements de cette arme (ids, ou "#tag")
 */
public record WeaponDefinition(int formatVersion,
                               boolean twoHanded,
                               boolean dualWielded,
                               double attackSpeed,
                               double attackReach,
                               double baseDamage,
                               Map<String, Double> materialDamage,
                               double armorPenetration,
                               boolean disablesShield,
                               List<OnHitEffect> onHitEffects,
                               SweepDefinition sweep,
                               WeaponAbility ability,
                               ParryDefinition parry,
                               DashDefinition dash,
                               ComboDefinition combo,
                               List<String> items) {

    public static final int FORMAT_VERSION = 1;
    /** Les dégâts calculés sont arrondis au demi-point, pour des valeurs lisibles. */
    private static final double DAMAGE_STEPS_PER_POINT = 2.0D;
    /** Le mode "équilibrage global" retire de petites parts des dégâts : elles sont arrondies au dixième pour rester visibles. */
    private static final double PENALIZED_STEPS_PER_POINT = 10.0D;
    // Réduction des dégâts du mode "équilibrage global" (balance_abilities), en fraction des dégâts.
    private static final double PARRY_PENALTY = 0.04D;
    private static final double DASH_PENALTY = 0.04D;
    private static final double AREA_COMBO_PENALTY = 0.03D;
    private static final double ON_HIT_EFFECTS_PENALTY = 0.02D;
    private static final double SHIELD_DISABLING_PENALTY = 0.02D;
    /** 1 % de dégâts en moins par tranche de 10 % de pénétration d'armure. */
    private static final double PENALTY_PER_PENETRATION_PERCENT = 0.001D;
    /** Les capacités comptent moins que les dégâts bruts : jamais plus de 12 % en moins. */
    private static final double MAX_ABILITY_PENALTY = 0.12D;
    private static final int MAX_ON_HIT_EFFECTS = 8;
    private static final int MAX_ITEMS = 64;

    public static final DataField<Integer> FORMAT = DataField.integer("format_version", 1, FORMAT_VERSION, FORMAT_VERSION);
    public static final DataField<Boolean> TWO_HANDED = DataField.bool("two_handed", false);
    public static final DataField<Boolean> DUAL_WIELDED = DataField.bool("dual_wielded", false);
    // Au-delà de 20 attaques/s, la recharge passerait sous un tick ; sous 0.1, une attaque prendrait plus de 10 s.
    public static final DataField<Double> ATTACK_SPEED = DataField.number("attack_speed", 0.1D, 20.0D, 1.6D);
    public static final DataField<Double> ATTACK_REACH = DataField.number("attack_reach", 0.5D, 64.0D, 3.0D);
    public static final DataField<Double> BASE_DAMAGE = DataField.number("base_damage", 0.0D, 1024.0D, 4.0D);
    public static final DataField<Map<String, Double>> MATERIAL_DAMAGE = DataField.numberMap("material_damage", 0.0D, 1024.0D);
    public static final DataField<Double> ARMOR_PENETRATION = DataField.number("armor_penetration", 0.0D, 100.0D, 0.0D);
    public static final DataField<Boolean> DISABLES_SHIELD = DataField.bool("disables_shield", false);
    public static final DataField<List<OnHitEffect>> ON_HIT_EFFECTS =
            DataField.objectList("on_hit_effects", OnHitEffect.FIELDS, OnHitEffect.CODEC, MAX_ON_HIT_EFFECTS);
    public static final DataField<SweepDefinition> SWEEP =
            DataField.object("sweep", SweepDefinition.FIELDS, SweepDefinition.CODEC, SweepDefinition.VANILLA);
    public static final DataField<WeaponAbility> ABILITY = DataField.choice("ability", WeaponAbility.values(), WeaponAbility.NONE);
    public static final DataField<ParryDefinition> PARRY =
            DataField.object("parry", ParryDefinition.FIELDS, ParryDefinition.CODEC, ParryDefinition.DEFAULT);
    public static final DataField<DashDefinition> DASH =
            DataField.object("dash", DashDefinition.FIELDS, DashDefinition.CODEC, DashDefinition.DEFAULT);
    public static final DataField<ComboDefinition> COMBO =
            DataField.object("combo", ComboDefinition.FIELDS, ComboDefinition.CODEC, ComboDefinition.NONE);
    public static final DataField<List<String>> ITEMS = DataField.itemList("items", MAX_ITEMS, "minecraft:diamond_sword", "#minecraft:axes");

    public static final FieldSet FIELDS = new FieldSet("weapon",
            List.of(FORMAT, TWO_HANDED, DUAL_WIELDED, ATTACK_SPEED, ATTACK_REACH, BASE_DAMAGE, MATERIAL_DAMAGE,
                    ARMOR_PENETRATION, DISABLES_SHIELD, ON_HIT_EFFECTS, SWEEP, ABILITY, PARRY, DASH, COMBO, ITEMS),
            // Noms de l'ancienne config TOML et variantes courantes
            Map.ofEntries(
                    Map.entry("attack_damage", "base_damage"),
                    Map.entry("damage", "base_damage"),
                    Map.entry("speed", "attack_speed"),
                    Map.entry("reach", "attack_reach"),
                    Map.entry("two_hand", "two_handed"),
                    Map.entry("dual_wield", "dual_wielded"),
                    Map.entry("dual_wieldable", "dual_wielded"),
                    Map.entry("dual_handed", "dual_wielded"),
                    Map.entry("penetration", "armor_penetration"),
                    Map.entry("armor_pen", "armor_penetration"),
                    Map.entry("effects", "on_hit_effects"),
                    Map.entry("slowness", "on_hit_effects"),
                    Map.entry("sweeping", "sweep"),
                    Map.entry("disable_shield", "disables_shield"),
                    Map.entry("abilities", "ability"),
                    Map.entry("right_click", "ability"),
                    Map.entry("block", "parry"),
                    Map.entry("blocking", "parry"),
                    Map.entry("dodge", "dash"),
                    Map.entry("lunge", "dash"),
                    Map.entry("combos", "combo"),
                    Map.entry("item", "items"),
                    Map.entry("applies_to", "items")));

    public static final Codec<WeaponDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FORMAT.forGetter(WeaponDefinition::formatVersion),
            TWO_HANDED.forGetter(WeaponDefinition::twoHanded),
            DUAL_WIELDED.forGetter(WeaponDefinition::dualWielded),
            ATTACK_SPEED.forGetter(WeaponDefinition::attackSpeed),
            ATTACK_REACH.forGetter(WeaponDefinition::attackReach),
            BASE_DAMAGE.forGetter(WeaponDefinition::baseDamage),
            MATERIAL_DAMAGE.forGetter(WeaponDefinition::materialDamage),
            ARMOR_PENETRATION.forGetter(WeaponDefinition::armorPenetration),
            DISABLES_SHIELD.forGetter(WeaponDefinition::disablesShield),
            ON_HIT_EFFECTS.forGetter(WeaponDefinition::onHitEffects),
            SWEEP.forGetter(WeaponDefinition::sweep),
            ABILITY.forGetter(WeaponDefinition::ability),
            PARRY.forGetter(WeaponDefinition::parry),
            DASH.forGetter(WeaponDefinition::dash),
            COMBO.forGetter(WeaponDefinition::combo),
            ITEMS.forGetter(WeaponDefinition::items)
    ).apply(instance, WeaponDefinition::new));

    /** Utilisé si une arme n'a aucune donnée (fichier du mod introuvable) : une épée vanilla en bois. */
    public static final WeaponDefinition FALLBACK = new WeaponDefinition(FORMAT_VERSION,
            false, false, ATTACK_SPEED.defaultValue(), ATTACK_REACH.defaultValue(), BASE_DAMAGE.defaultValue(), Map.of(),
            0.0D, false, List.of(), SweepDefinition.VANILLA, WeaponAbility.NONE, ParryDefinition.DEFAULT, DashDefinition.DEFAULT,
            ComboDefinition.NONE, List.of());

    public WeaponDefinition {
        materialDamage = Map.copyOf(materialDamage);
        onHitEffects = List.copyOf(onHitEffects);
        items = List.copyOf(items);
    }

    /** Dégâts par seconde réels de l'épée en fer vanilla (6 dégâts toutes les 12 ticks), la référence des comparaisons. */
    public static final double VANILLA_IRON_SWORD_DPS = 10.0D;
    private static final double TICKS_PER_SECOND = 20.0D;
    /** Une cible ignore les coups qui se suivent en moins de 10 ticks, sauf si l'attaquant tient une arme dans chaque main. */
    private static final int MIN_TICKS_BETWEEN_HITS = 10;

    /** Dégâts par seconde d'une seule arme de ce matériau, chargée à fond (sans armure, critiques ni combos). */
    public double dps(WeaponMaterial material) {
        return damageFor(material) * TICKS_PER_SECOND / attackTicks(attackSpeed, MIN_TICKS_BETWEEN_HITS);
    }

    /** Dégâts par seconde avec une de ces armes dans chaque main, ou 0 si elle ne se manie pas ainsi. */
    public double pairDps(WeaponMaterial material) {
        RulesDefinition rules = WeaponData.rules();
        if (!dualWielded || (twoHanded && rules.twoHandedActive()) || !rules.dualWieldActive()) return 0.0D;
        int protection = (int) Math.round(rules.dualWieldInvulnerabilitySeconds() * TICKS_PER_SECOND);
        return damageFor(material) * TICKS_PER_SECOND / attackTicks(attackSpeed * rules.dualWieldSpeedMultiplier(), protection);
    }

    /**
     * Temps réel entre deux coups : le jeu compte en ticks entiers, et un coup est à pleine force dès que le compteur
     * + 0.5 atteint la recharge. Frapper plus vite que la protection de la cible ne sert à rien.
     */
    private static int attackTicks(double attacksPerSecond, int minTicks) {
        return Math.max(Math.max(1, minTicks), (int) Math.ceil(TICKS_PER_SECOND / attacksPerSecond - 0.5D));
    }

    /**
     * Dégâts affichés pour ce matériau : la valeur imposée dans {@code material_damage} s'il y en a une (elle est
     * prise telle quelle, sans pénalité), sinon {@code base_damage × facteur du matériau}, arrondi au demi-point.
     * Dans ce second cas, le mode "équilibrage global" retire une petite part de ces dégâts selon les capacités
     * de l'arme ({@link #abilityPenalty}).
     */
    public double damageFor(WeaponMaterial material) {
        Double forced = materialDamage.get(material.id());
        if (forced != null) return forced;
        double damage = Math.round(baseDamage * material.damageMultiplier() * DAMAGE_STEPS_PER_POINT) / DAMAGE_STEPS_PER_POINT;
        double penalty = abilityPenalty(WeaponData.rules());
        return penalty <= 0.0D ? damage : Math.round(damage * (1.0D - penalty) * PENALIZED_STEPS_PER_POINT) / PENALIZED_STEPS_PER_POINT;
    }

    /**
     * Part des dégâts retirée par le mode "équilibrage global" ({@code balance_abilities} de rules.json) : 0 si le
     * mode est coupé. Seuls les systèmes actifs comptent, et le total reste plafonné.
     */
    public double abilityPenalty(RulesDefinition rules) {
        if (!rules.balanceAbilities()) return 0.0D;
        double penalty = 0.0D;
        if (ability == WeaponAbility.PARRY && rules.parryActive()) penalty += PARRY_PENALTY;
        if (ability == WeaponAbility.DASH && rules.dashActive()) penalty += DASH_PENALTY;
        if (rules.combosActive() && combo.enabled() && combo.steps().stream().anyMatch(ComboStep::hasArea)) penalty += AREA_COMBO_PENALTY;
        if (rules.onHitEffectsActive() && !onHitEffects.isEmpty()) penalty += ON_HIT_EFFECTS_PENALTY;
        if (rules.shieldDisablingActive() && disablesShield) penalty += SHIELD_DISABLING_PENALTY;
        if (rules.armorPenetrationActive()) penalty += armorPenetration * PENALTY_PER_PENETRATION_PERCENT;
        return Math.min(penalty, MAX_ABILITY_PENALTY);
    }
}
