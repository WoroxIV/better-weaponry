package com.betterweaponry.item;

import org.jetbrains.annotations.Nullable;

import static com.betterweaponry.item.WeaponStats.oneHanded;
import static com.betterweaponry.item.WeaponStats.twoHanded;

/**
 * Tous les types d'armes du mod.
 * <p>
 * Ajouter une arme = ajouter une ligne ici (+ textures et traductions) :
 * l'enregistrement des items, la config, les modèles, les tags et les recettes sont générés automatiquement.
 * L'ordre de déclaration est l'ordre d'affichage dans l'onglet créatif.
 *
 * <h2>Équilibrage</h2>
 * Référence : l'épée vanilla en bois, 4 dégâts × 1.6 attaque/s = 6.4 dégâts par seconde (DPS).
 * Chaque arme vise : 6.4 × (1.5 si deux mains) × facteur de portée, le facteur compensant le risque
 * d'une portée courte ou l'avantage d'une portée longue :
 * <pre>
 *   portée   2.0    2.5    3.0    4.0    4.5    5.0
 *   facteur  1.25   1.15   1.00   0.85   0.80   0.75
 * </pre>
 * Toutes les armes sont à ±5 % de leur cible. Deux mains ≈ 1.5× une arme à une main, ce qui équivaut au
 * dual wield (multiplicateur de vitesse 1.5 par défaut) : les deux façons d'occuper la main secondaire se valent,
 * et une arme à une main garde l'option du bouclier.
 *
 * <h2>Recettes</h2>
 * Le motif de craft (voir {@link WeaponRecipe}) : M = matériau, S = bâton, décliné pour chaque matériau
 * par {@code gradlew runData}. {@code null} = pas de recette.
 * Ces motifs ont été vérifiés contre les 1290 recettes vanilla de la 1.21.1 (miroir compris) pour tous
 * les matériaux vanilla : aucun conflit, ni entre eux. En cas de modification, éviter les formes vanilla
 * (épée M/M/S, hache, houe, panneau MMM/MMM/.S., levier S/M avec la pierre, bâton M/M avec les planches...).
 */
public enum WeaponType {
    //                                                                                     dégâts vitesse portée   DPS
    // Épée lourde à une main : longue portée pour une arme à bouclier, mais lente
    BROADSWORD("broadsword", "broadsword_base", WeaponRecipe.of("MM", "MM", "S "),
            oneHanded(4.5, 1.2, 4.0)),                                                 //  4.5    1.2    4.0     5.4
    // Lance de phalange : la plus longue portée, à une main pour garder le bouclier, coups légers
    SPEAR("spear", "spear_base", WeaponRecipe.of("  M", " S ", "S  "),
            oneHanded(4.0, 1.2, 5.0)),                                                 //  4.0    1.2    5.0     4.8
    // Dague : la plus rapide, au corps à corps ; idéale par paire
    DAGGER("dagger", "dagger_base", WeaponRecipe.of("M", "S"),
            oneHanded(4.0, 2.0, 2.0)),                                                 //  4.0    2.0    2.0     8.0
    // Faux : très grande portée, très lente, gros coups balayés
    SCYTHE("scythe", "scythe_base", WeaponRecipe.of("MMM", "  S", " S "),
            twoHanded(9.0, 0.8, 5.0)),                                                 //  9.0    0.8    5.0     7.2
    // Rapière : estoc rapide et précis, coups légers
    RAPIER("rapier", null, WeaponRecipe.of("  M", " M ", "S  "),
            oneHanded(3.5, 1.9, 3.0)),                                                 //  3.5    1.9    3.0     6.65
    // Faucille : lame en crochet, coups tranchants appuyés à courte portée
    SICKLE("sickle", null, WeaponRecipe.of("MM", " M", "S "),
            oneHanded(5.5, 1.4, 2.5)),                                                 //  5.5    1.4    2.5     7.7
    // Katana : arme à deux mains rapide, frappe fort sans sacrifier la vitesse
    KATANA("katana", "katana_base", WeaponRecipe.of(" M", " M", "S "),
            twoHanded(6.0, 1.6, 3.0)),                                                 //  6.0    1.6    3.0     9.6
    // Hache de guerre : lourde, lente, gros dégâts
    BATTLEAXE("battleaxe", "battleaxe_base", WeaponRecipe.of("MSM", "MSM", " S "),
            twoHanded(9.0, 0.9, 4.0)),                                                 //  9.0    0.9    4.0     8.1
    // Glaive : arme d'hast équilibrée, longue portée à deux mains
    GLAIVE("glaive", "glaive_base", WeaponRecipe.of(" MM", " S ", "S  "),
            twoHanded(7.5, 1.0, 4.5)),                                                 //  7.5    1.0    4.5     7.5
    // Marteau : le plus lent, coups dévastateurs (le meilleur contre les armures)
    HAMMER("hammer", "hammer_base", WeaponRecipe.of("MMM", "MSM", " S "),
            twoHanded(11.5, 0.7, 4.0)),                                                // 11.5    0.7    4.0     8.05
    // Claymore : grande épée à deux mains, la plus maniable des armes lourdes
    CLAYMORE("claymore", "claymore_base", WeaponRecipe.of(" M ", "MMM", " S "),
            twoHanded(7.5, 1.1, 4.0)),                                                 //  7.5    1.1    4.0     8.25
    // Chakram : anneau tranchant, lent mais chaque coup porte
    CHAKRAM("chakram", "chakram_base", WeaponRecipe.of(" M ", "M M", " M "),
            oneHanded(5.0, 1.3, 3.0)),                                                 //  5.0    1.3    3.0     6.5
    // Saï : arme de parade rapide au corps à corps, faite pour le dual wield
    SAI("sai", "sai_base", WeaponRecipe.of("M M", " M ", " S "),
            oneHanded(4.0, 1.8, 2.5));                                                 //  4.0    1.8    2.5     7.2

    private final String id;
    @Nullable private final String baseModel;
    @Nullable private final WeaponRecipe recipe;
    private final WeaponStats stats;

    WeaponType(String id, @Nullable String baseModel, @Nullable WeaponRecipe recipe, WeaponStats stats) {
        this.id = id;
        this.baseModel = baseModel;
        this.recipe = recipe;
        this.stats = stats;
    }

    /** Nom utilisé dans les IDs d'items et les catégories de config (ex : "katana"). */
    public String id() {
        return id;
    }

    /** Modèle parent dans assets/better_weaponry/models/item, ou null pour le "item/handheld" vanilla. */
    @Nullable
    public String baseModel() {
        return baseModel;
    }

    @Nullable
    public WeaponRecipe recipe() {
        return recipe;
    }

    /** Statistiques par défaut, utilisées pour générer la config. */
    public WeaponStats defaultStats() {
        return stats;
    }
}
