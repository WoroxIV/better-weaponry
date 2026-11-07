package com.betterweaponry.item;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Tous les types d'armes du mod.
 * <p>
 * Ajouter une arme = ajouter une ligne ici, un fichier de stats
 * {@code data/better_weaponry/better_weaponry/weapons/<id>.json} (+ textures et traductions) :
 * l'enregistrement des items, les modèles, les tags et les recettes sont générés automatiquement.
 * L'ordre de déclaration est l'ordre d'affichage dans l'onglet créatif.
 *
 * <h2>Stats</h2>
 * Les stats (dégâts, vitesse, portée, deux mains) ne sont plus ici : elles sont dans les fichiers JSON ci-dessus,
 * modifiables sans toucher au code. L'équilibrage (DPS visé selon la portée et le nombre de mains) est expliqué
 * dans le README du projet.
 *
 * <h2>Recettes</h2>
 * Le motif de craft (voir {@link WeaponRecipe}) : M = matériau, S = bâton, décliné pour chaque matériau
 * par {@code gradlew runData}. {@code null} = pas de recette.
 * Ces motifs ont été vérifiés contre les 1290 recettes vanilla de la 1.21.1 (miroir compris) pour tous
 * les matériaux vanilla : aucun conflit, ni entre eux. En cas de modification, éviter les formes vanilla
 * (épée M/M/S, hache, houe, panneau MMM/MMM/.S., levier S/M avec la pierre, bâton M/M avec les planches...).
 */
public enum WeaponType implements StringRepresentable {
    // Épée lourde à une main : longue portée pour une arme à bouclier, mais lente
    BROADSWORD("broadsword", "broadsword_base", WeaponRecipe.of("MM", "MM", "S ")),
    // Lance de phalange : la plus longue portée, à une main pour garder le bouclier, coups légers
    SPEAR("spear", "spear_base", WeaponRecipe.of("  M", " S ", "S  ")),
    // Dague : la plus rapide, au corps à corps ; idéale par paire
    DAGGER("dagger", "dagger_base", WeaponRecipe.of("M", "S")),
    // Faux : très grande portée, très lente, gros coups balayés
    SCYTHE("scythe", "scythe_base", WeaponRecipe.of("MMM", "  S", " S ")),
    // Rapière : estoc rapide et précis, coups légers
    RAPIER("rapier", null, WeaponRecipe.of("  M", " M ", "S  ")),
    // Faucille : lame en crochet, coups tranchants appuyés à courte portée
    SICKLE("sickle", null, WeaponRecipe.of("MM", " M", "S ")),
    // Katana : arme à deux mains rapide, frappe fort sans sacrifier la vitesse
    KATANA("katana", "katana_base", WeaponRecipe.of(" M", " M", "S ")),
    // Hache de guerre : lourde, lente, gros dégâts
    BATTLEAXE("battleaxe", "battleaxe_base", WeaponRecipe.of("MSM", "MSM", " S ")),
    // Glaive : arme d'hast équilibrée, longue portée à deux mains
    GLAIVE("glaive", "glaive_base", WeaponRecipe.of(" MM", " S ", "S  ")),
    // Marteau : le plus lent, coups dévastateurs
    HAMMER("hammer", "hammer_base", WeaponRecipe.of("MMM", "MSM", " S ")),
    // Claymore : grande épée à deux mains, la plus maniable des armes lourdes
    CLAYMORE("claymore", "claymore_base", WeaponRecipe.of(" M ", "MMM", " S ")),
    // Chakram : anneau tranchant, lent mais chaque coup porte
    CHAKRAM("chakram", "chakram_base", WeaponRecipe.of(" M ", "M M", " M ")),
    // Saï : arme de parade rapide au corps à corps, faite pour le dual wield
    SAI("sai", "sai_base", WeaponRecipe.of("M M", " M ", " S "));

    /** Lit et écrit un type par son id ("katana") dans les fichiers JSON et la synchronisation réseau. */
    public static final Codec<WeaponType> CODEC = StringRepresentable.fromEnum(WeaponType::values);

    private final String id;
    @Nullable private final String baseModel;
    @Nullable private final WeaponRecipe recipe;

    WeaponType(String id, @Nullable String baseModel, @Nullable WeaponRecipe recipe) {
        this.id = id;
        this.baseModel = baseModel;
        this.recipe = recipe;
    }

    /** Nom utilisé dans les IDs d'items, les fichiers de stats et les traductions (ex : "katana"). */
    public String id() {
        return id;
    }

    /** Type de cet id ("katana"), ou null s'il n'existe pas. */
    @Nullable
    public static WeaponType byId(String id) {
        for (WeaponType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }

    @Override
    public String getSerializedName() {
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
}
