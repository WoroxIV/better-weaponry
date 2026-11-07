package com.betterweaponry.item;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Tiers;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Liste de tous les matériaux d'armes. L'ordre de déclaration est l'ordre dans l'onglet créatif.
 *
 * <h2>Ajouter un matériau d'un autre mod</h2>
 * <ol>
 *   <li>Le déclarer ci-dessous avec {@code stats}, {@code craftedFrom} et {@code requiresAnyMod}.</li>
 *   <li>Ajouter les 13 textures {@code textures/item/<matériau>_<type>.png}.</li>
 *   <li>Ajouter le nom du matériau dans chaque source de traduction {@code src/main/lang/<langue>.json}
 *       ({@code names.materials}) : les 13 noms d'armes sont construits automatiquement.</li>
 *   <li>Lancer {@code gradlew runData} : modèles, traductions, tags et recettes sont générés automatiquement.</li>
 * </ol>
 * Pour les ingrédients, mettre d'abord le tag commun ({@code #c:ingots/...}) puis l'item exact du mod :
 * toutes les entrées sont optionnelles, donc un item mal nommé ou absent ne casse rien.
 * Les recettes ne se chargent que si le mod est présent ET que l'ingrédient existe.
 */
public final class WeaponMaterials {
    private static final List<WeaponMaterial> ALL = new ArrayList<>();
    private static final List<WeaponMaterial> ACTIVE = new ArrayList<>();

    // IDs des mods compatibles
    private static final String MEKANISM = "mekanism";
    private static final String CREATE = "create";
    private static final String THERMAL = "thermal";
    private static final String IMMERSIVE_ENGINEERING = "immersiveengineering";
    private static final String BOTANIA = "botania";
    private static final String TWILIGHT_FOREST = "twilightforest";
    private static final String AE2 = "ae2";
    private static final String AETHER = "aether";
    private static final String DEEPER_DARKER = "deeperdarker";
    private static final String ICE_AND_FIRE = "iceandfire";
    private static final String TINKERS = "tconstruct";
    private static final String ALLTHEMODIUM = "allthemodium";
    private static final String CATACLYSM = "cataclysm";
    private static final String UNDERGARDEN = "undergarden";
    private static final String OCCULTISM = "occultism";
    private static final String OREGANIZED = "oreganized";
    private static final String GALOSPHERE = "galosphere";

    // =====================================================================
    // Vanilla
    // Dégâts par défaut calculés à partir du Tier : 8.5 + 2 × bonus de dégâts.
    // =====================================================================

    public static final WeaponMaterial WOODEN = register(WeaponMaterial.builder("wooden", Tiers.WOOD)
            .craftedWith(ItemTags.PLANKS));

    public static final WeaponMaterial STONE = register(WeaponMaterial.builder("stone", Tiers.STONE)
            .craftedWith(ItemTags.STONE_TOOL_MATERIALS)); // pierre taillée, roche noire, ardoise des abîmes taillée

    public static final WeaponMaterial IRON = register(WeaponMaterial.builder("iron", Tiers.IRON)
            .craftedWith(Tags.Items.INGOTS_IRON));

    public static final WeaponMaterial GOLDEN = register(WeaponMaterial.builder("golden", Tiers.GOLD)
            .craftedWith(Tags.Items.INGOTS_GOLD));

    public static final WeaponMaterial DIAMOND = register(WeaponMaterial.builder("diamond", Tiers.DIAMOND)
            .craftedWith(Tags.Items.GEMS_DIAMOND));

    // Comme en vanilla : arme en diamant + modèle d'amélioration + lingot de netherite.
    public static final WeaponMaterial NETHERITE = register(WeaponMaterial.builder("netherite", Tiers.NETHERITE)
            .smithingUpgrade(DIAMOND, "minecraft:netherite_upgrade_smithing_template", "#c:ingots/netherite")
            .fireResistant());

    // =====================================================================
    // Métaux communs, fournis par plusieurs mods (actifs dès qu'un d'eux est installé)
    // stats(durabilité, enchantabilité, bonus de dégâts)
    // =====================================================================

    public static final WeaponMaterial STEEL = register(WeaponMaterial.builder("steel")
            .stats(500, 10, 2.5F)
            .craftedFrom("#c:ingots/steel")
            .requiresAnyMod(MEKANISM, IMMERSIVE_ENGINEERING));

    public static final WeaponMaterial BRONZE = register(WeaponMaterial.builder("bronze")
            .stats(375, 12, 2.0F)
            .craftedFrom("#c:ingots/bronze")
            .requiresAnyMod(MEKANISM, THERMAL));

    public static final WeaponMaterial SILVER = register(WeaponMaterial.builder("silver")
            .stats(200, 20, 1.5F)
            .craftedFrom("#c:ingots/silver")
            .requiresAnyMod(IMMERSIVE_ENGINEERING, THERMAL, OCCULTISM, OREGANIZED, GALOSPHERE, ICE_AND_FIRE));

    public static final WeaponMaterial LEAD = register(WeaponMaterial.builder("lead")
            .stats(160, 8, 1.5F)
            .craftedFrom("#c:ingots/lead")
            .requiresAnyMod(IMMERSIVE_ENGINEERING, THERMAL, MEKANISM, OREGANIZED));

    public static final WeaponMaterial NICKEL = register(WeaponMaterial.builder("nickel")
            .stats(300, 12, 2.0F)
            .craftedFrom("#c:ingots/nickel")
            .requiresAnyMod(IMMERSIVE_ENGINEERING, THERMAL));

    public static final WeaponMaterial ALUMINUM = register(WeaponMaterial.builder("aluminum")
            .stats(220, 14, 1.5F)
            .craftedFrom("#c:ingots/aluminum")
            .requiresAnyMod(IMMERSIVE_ENGINEERING));

    public static final WeaponMaterial ELECTRUM = register(WeaponMaterial.builder("electrum")
            .stats(150, 26, 1.5F)
            .craftedFrom("#c:ingots/electrum")
            .requiresAnyMod(IMMERSIVE_ENGINEERING, THERMAL, OREGANIZED));

    public static final WeaponMaterial CONSTANTAN = register(WeaponMaterial.builder("constantan")
            .stats(350, 12, 2.0F)
            .craftedFrom("#c:ingots/constantan")
            .requiresAnyMod(IMMERSIVE_ENGINEERING, THERMAL));

    // =====================================================================
    // Mekanism
    // =====================================================================

    public static final WeaponMaterial OSMIUM = register(WeaponMaterial.builder("osmium")
            .stats(1024, 14, 3.0F)
            .craftedFrom("#c:ingots/osmium", "mekanism:ingot_osmium")
            .requiresAnyMod(MEKANISM));

    public static final WeaponMaterial REFINED_OBSIDIAN = register(WeaponMaterial.builder("refined_obsidian")
            .stats(2500, 18, 5.0F)
            .craftedFrom("#c:ingots/refined_obsidian", "mekanism:ingot_refined_obsidian")
            .requiresAnyMod(MEKANISM));

    public static final WeaponMaterial REFINED_GLOWSTONE = register(WeaponMaterial.builder("refined_glowstone")
            .stats(384, 20, 2.5F)
            .craftedFrom("#c:ingots/refined_glowstone", "mekanism:ingot_refined_glowstone")
            .requiresAnyMod(MEKANISM));

    // =====================================================================
    // Thermal Series
    // =====================================================================

    public static final WeaponMaterial INVAR = register(WeaponMaterial.builder("invar")
            .stats(450, 12, 2.5F)
            .craftedFrom("#c:ingots/invar")
            .requiresAnyMod(THERMAL));

    public static final WeaponMaterial SIGNALUM = register(WeaponMaterial.builder("signalum")
            .stats(800, 18, 3.0F)
            .craftedFrom("#c:ingots/signalum", "thermal:signalum_ingot")
            .requiresAnyMod(THERMAL));

    public static final WeaponMaterial LUMIUM = register(WeaponMaterial.builder("lumium")
            .stats(900, 20, 3.0F)
            .craftedFrom("#c:ingots/lumium", "thermal:lumium_ingot")
            .requiresAnyMod(THERMAL));

    public static final WeaponMaterial ENDERIUM = register(WeaponMaterial.builder("enderium")
            .stats(1800, 16, 4.0F)
            .craftedFrom("#c:ingots/enderium", "thermal:enderium_ingot")
            .requiresAnyMod(THERMAL));

    // =====================================================================
    // Create
    // =====================================================================

    public static final WeaponMaterial BRASS = register(WeaponMaterial.builder("brass")
            .stats(300, 16, 2.0F)
            .craftedFrom("#c:ingots/brass", "create:brass_ingot")
            .requiresAnyMod(CREATE));

    public static final WeaponMaterial ANDESITE_ALLOY = register(WeaponMaterial.builder("andesite_alloy")
            .stats(200, 10, 1.5F)
            .craftedFrom("#c:ingots/andesite_alloy", "create:andesite_alloy")
            .requiresAnyMod(CREATE));

    // =====================================================================
    // Botania
    // =====================================================================

    public static final WeaponMaterial MANASTEEL = register(WeaponMaterial.builder("manasteel")
            .stats(300, 20, 2.0F)
            .craftedFrom("#c:ingots/manasteel", "botania:manasteel_ingot")
            .requiresAnyMod(BOTANIA));

    public static final WeaponMaterial ELEMENTIUM = register(WeaponMaterial.builder("elementium")
            .stats(720, 20, 2.0F)
            .craftedFrom("#c:ingots/elementium", "botania:elementium_ingot")
            .requiresAnyMod(BOTANIA));

    public static final WeaponMaterial TERRASTEEL = register(WeaponMaterial.builder("terrasteel")
            .stats(2300, 26, 3.0F)
            .craftedFrom("#c:ingots/terrasteel", "botania:terrasteel_ingot")
            .requiresAnyMod(BOTANIA));

    // =====================================================================
    // The Twilight Forest
    // =====================================================================

    public static final WeaponMaterial IRONWOOD = register(WeaponMaterial.builder("ironwood")
            .stats(512, 25, 2.0F)
            .craftedFrom("#c:ingots/ironwood", "twilightforest:ironwood_ingot")
            .requiresAnyMod(TWILIGHT_FOREST));

    public static final WeaponMaterial STEELEAF = register(WeaponMaterial.builder("steeleaf")
            .stats(131, 9, 3.0F)
            .craftedFrom("#c:ingots/steeleaf", "twilightforest:steeleaf_ingot")
            .requiresAnyMod(TWILIGHT_FOREST));

    public static final WeaponMaterial KNIGHTMETAL = register(WeaponMaterial.builder("knightmetal")
            .stats(512, 8, 3.0F)
            .craftedFrom("#c:ingots/knightmetal", "twilightforest:knightmetal_ingot")
            .requiresAnyMod(TWILIGHT_FOREST));

    public static final WeaponMaterial FIERY = register(WeaponMaterial.builder("fiery")
            .stats(1024, 10, 4.0F)
            .craftedFrom("#c:ingots/fiery", "twilightforest:fiery_ingot")
            .fireResistant()
            .requiresAnyMod(TWILIGHT_FOREST));

    // =====================================================================
    // Applied Energistics 2
    // =====================================================================

    public static final WeaponMaterial CERTUS_QUARTZ = register(WeaponMaterial.builder("certus_quartz")
            .stats(250, 14, 2.0F)
            .craftedFrom("#c:gems/certus_quartz", "ae2:certus_quartz_crystal")
            .requiresAnyMod(AE2));

    public static final WeaponMaterial FLUIX = register(WeaponMaterial.builder("fluix")
            .stats(750, 16, 2.5F)
            .craftedFrom("#c:gems/fluix", "ae2:fluix_crystal")
            .requiresAnyMod(AE2));

    // =====================================================================
    // The Aether
    // =====================================================================

    public static final WeaponMaterial ZANITE = register(WeaponMaterial.builder("zanite")
            .stats(250, 14, 2.0F)
            .craftedFrom("#c:gems/zanite", "aether:zanite_gemstone")
            .requiresAnyMod(AETHER));

    // Comme les outils de l'Aether : fabriqués avec des blocs de gravitite enchantée.
    public static final WeaponMaterial GRAVITITE = register(WeaponMaterial.builder("gravitite")
            .stats(1561, 10, 3.0F)
            .craftedFrom("aether:enchanted_gravitite")
            .requiresAnyMod(AETHER));

    // =====================================================================
    // Deeper and Darker : amélioration du netherite, comme ses outils de Warden
    // =====================================================================

    public static final WeaponMaterial WARDEN = register(WeaponMaterial.builder("warden")
            .stats(4000, 18, 5.0F)
            .smithingUpgrade(NETHERITE, "deeperdarker:warden_upgrade_smithing_template", "deeperdarker:reinforced_echo_shard")
            .fireResistant() // amélioration du netherite : garde sa résistance au feu
            .requiresAnyMod(DEEPER_DARKER));

    // =====================================================================
    // Ice and Fire
    // =====================================================================

    public static final WeaponMaterial DRAGONBONE = register(WeaponMaterial.builder("dragonbone")
            .stats(1660, 22, 3.5F)
            .craftedFrom("iceandfire:dragonbone")
            .requiresAnyMod(ICE_AND_FIRE));

    public static final WeaponMaterial FIRE_DRAGONSTEEL = register(WeaponMaterial.builder("fire_dragonsteel")
            .stats(4000, 20, 6.0F)
            .craftedFrom("iceandfire:dragonsteel_fire_ingot")
            .requiresAnyMod(ICE_AND_FIRE));

    public static final WeaponMaterial ICE_DRAGONSTEEL = register(WeaponMaterial.builder("ice_dragonsteel")
            .stats(4000, 20, 6.0F)
            .craftedFrom("iceandfire:dragonsteel_ice_ingot")
            .requiresAnyMod(ICE_AND_FIRE));

    public static final WeaponMaterial LIGHTNING_DRAGONSTEEL = register(WeaponMaterial.builder("lightning_dragonsteel")
            .stats(4000, 20, 6.0F)
            .craftedFrom("iceandfire:dragonsteel_lightning_ingot")
            .requiresAnyMod(ICE_AND_FIRE));

    // =====================================================================
    // Tinkers' Construct
    // =====================================================================

    public static final WeaponMaterial COBALT = register(WeaponMaterial.builder("cobalt")
            .stats(800, 18, 2.5F)
            .craftedFrom("#c:ingots/cobalt", "tconstruct:cobalt_ingot")
            .requiresAnyMod(TINKERS));

    public static final WeaponMaterial MANYULLYN = register(WeaponMaterial.builder("manyullyn")
            .stats(1500, 16, 4.0F)
            .craftedFrom("#c:ingots/manyullyn", "tconstruct:manyullyn_ingot")
            .requiresAnyMod(TINKERS));

    // =====================================================================
    // Allthemodium (plus fort que le netherite)
    // =====================================================================

    public static final WeaponMaterial ALLTHEMODIUM_MATERIAL = register(WeaponMaterial.builder("allthemodium")
            .stats(3000, 20, 5.0F)
            .craftedFrom("#c:ingots/allthemodium", "allthemodium:allthemodium_ingot")
            .requiresAnyMod(ALLTHEMODIUM));

    public static final WeaponMaterial VIBRANIUM = register(WeaponMaterial.builder("vibranium")
            .stats(4000, 25, 6.0F)
            .craftedFrom("#c:ingots/vibranium", "allthemodium:vibranium_ingot")
            .requiresAnyMod(ALLTHEMODIUM));

    public static final WeaponMaterial UNOBTAINIUM = register(WeaponMaterial.builder("unobtainium")
            .stats(5000, 30, 7.0F)
            .craftedFrom("#c:ingots/unobtainium", "allthemodium:unobtainium_ingot")
            .requiresAnyMod(ALLTHEMODIUM));

    // =====================================================================
    // L_Ender's Cataclysm
    // =====================================================================

    public static final WeaponMaterial IGNITIUM = register(WeaponMaterial.builder("ignitium")
            .stats(3000, 16, 5.0F)
            .craftedFrom("#c:ingots/ignitium", "cataclysm:ignitium_ingot")
            .fireResistant()
            .requiresAnyMod(CATACLYSM));

    public static final WeaponMaterial CURSIUM = register(WeaponMaterial.builder("cursium")
            .stats(3000, 16, 5.0F)
            .craftedFrom("#c:ingots/cursium", "cataclysm:cursium_ingot")
            .requiresAnyMod(CATACLYSM));

    // =====================================================================
    // The Undergarden
    // =====================================================================

    public static final WeaponMaterial CLOGGRUM = register(WeaponMaterial.builder("cloggrum")
            .stats(250, 12, 2.0F)
            .craftedFrom("#c:ingots/cloggrum", "undergarden:cloggrum_ingot")
            .requiresAnyMod(UNDERGARDEN));

    public static final WeaponMaterial FROSTSTEEL = register(WeaponMaterial.builder("froststeel")
            .stats(575, 15, 2.5F)
            .craftedFrom("#c:ingots/froststeel", "undergarden:froststeel_ingot")
            .requiresAnyMod(UNDERGARDEN));

    public static final WeaponMaterial UTHERIUM = register(WeaponMaterial.builder("utherium")
            .stats(1600, 14, 3.0F)
            .craftedFrom("#c:gems/utherium", "undergarden:utherium_crystal")
            .requiresAnyMod(UNDERGARDEN));

    public static final WeaponMaterial FORGOTTEN = register(WeaponMaterial.builder("forgotten")
            .stats(1500, 18, 3.0F)
            .craftedFrom("#c:ingots/forgotten_metal", "undergarden:forgotten_ingot")
            .requiresAnyMod(UNDERGARDEN));

    private WeaponMaterials() {
    }

    /**
     * Matériaux actifs dans cette partie : ceux sans mod requis, plus ceux dont un mod est installé.
     * Pendant "gradlew runData", tous les matériaux sont actifs pour générer modèles et recettes
     * (les recettes des matériaux moddés reçoivent des conditions de chargement).
     */
    public static List<WeaponMaterial> active() {
        return Collections.unmodifiableList(ACTIVE);
    }

    /** Tous les matériaux déclarés, y compris ceux dont le mod n'est pas installé. */
    public static List<WeaponMaterial> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static WeaponMaterial register(WeaponMaterial.Builder builder) {
        WeaponMaterial material = builder.build();
        ALL.add(material);
        if (isAvailable(material)) {
            ACTIVE.add(material);
        }
        return material;
    }

    private static boolean isAvailable(WeaponMaterial material) {
        return !material.isModded()
                || DatagenModLoader.isRunningDataGen()
                || material.requiredMods().stream().anyMatch(ModList.get()::isLoaded);
    }
}
