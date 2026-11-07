package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Un matériau d'arme (bois, fer, ou un matériau d'un autre mod).
 * Chaque matériau déclaré dans {@link WeaponMaterials} donne une version de chaque {@link WeaponType}.
 * <p>
 * Un matériau est soit fabriqué à l'établi ({@link Builder#craftedWith} / {@link Builder#craftedFrom}),
 * soit obtenu par amélioration à la table de forge ({@link Builder#smithingUpgrade}).
 */
public final class WeaponMaterial {
    /**
     * Dégâts de base d'une épée vanilla en bois. Le multiplicateur d'un matériau vaut (4 + bonus) / 4 :
     * il reproduit exactement l'échelle des épées vanilla (4 / 5 / 6 / 7 / 8 du bois au netherite).
     */
    private static final double VANILLA_SWORD_BASE_DAMAGE = 4.0D;

    private final String id;
    private final Tier tier;
    private final double damageMultiplier;
    @Nullable private final MaterialIngredient craftingIngredient;
    @Nullable private final SmithingUpgrade smithingUpgrade;
    private final List<String> requiredMods;

    private WeaponMaterial(Builder builder, Tier tier) {
        this.id = builder.id;
        this.tier = tier;
        this.damageMultiplier = builder.damageMultiplier != null
                ? builder.damageMultiplier
                : (VANILLA_SWORD_BASE_DAMAGE + tier.getAttackDamageBonus()) / VANILLA_SWORD_BASE_DAMAGE;
        this.craftingIngredient = builder.craftingIngredient;
        this.smithingUpgrade = builder.smithingUpgrade;
        this.requiredMods = List.copyOf(builder.requiredMods);
    }

    /** Matériau basé sur un {@link Tier} existant (vanilla). */
    public static Builder builder(String id, Tier tier) {
        return new Builder(id).tier(tier);
    }

    /** Matériau moddé : le Tier est créé à partir de {@link Builder#stats}. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Raccourci pour un tag d'item, ex : {@code itemTag("c:ingots/steel")}. */
    public static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    // --- Accesseurs ---

    /** Préfixe des noms d'items, ex : "golden" pour "golden_katana". */
    public String id() {
        return id;
    }

    /** Fournit la durabilité, l'enchantabilité et l'objet de réparation. */
    public Tier tier() {
        return tier;
    }

    /**
     * Multiplicateur appliqué aux dégâts de base de chaque type d'arme (bois = 1, fer = 1.5, netherite = 2).
     * C'est la seule statistique qui dépend du matériau.
     */
    public double damageMultiplier() {
        return damageMultiplier;
    }

    /** Ingrédient qui remplace "M" dans les motifs de craft, ou null si le matériau ne se fabrique pas à l'établi. */
    @Nullable
    public MaterialIngredient craftingIngredient() {
        return craftingIngredient;
    }

    @Nullable
    public SmithingUpgrade smithingUpgrade() {
        return smithingUpgrade;
    }

    /** Le matériau est actif si au moins un de ces mods est installé. Vide = toujours actif. */
    public List<String> requiredMods() {
        return requiredMods;
    }

    public boolean isModded() {
        return !requiredMods.isEmpty();
    }

    /** Nom de l'item pour ce matériau et ce type, ex : "netherite_katana". */
    public String itemName(WeaponType type) {
        return id + "_" + type.id();
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Amélioration à la table de forge, comme le netherite vanilla :
     * arme du matériau {@code base} + {@code template} + {@code addition} = arme de ce matériau.
     */
    public record SmithingUpgrade(WeaponMaterial base, MaterialIngredient template, MaterialIngredient addition) {
    }

    /**
     * Ingrédient de recette sous forme de tag.
     * <ul>
     *   <li>{@code entries} vide : tag existant (vanilla ou commun "c:"), utilisé tel quel ;</li>
     *   <li>sinon : tag propre au mod ({@code better_weaponry:weapon_materials/...}) généré par le datagen,
     *       dont chaque entrée est optionnelle. On peut ainsi viser des items d'autres mods sans dépendance :
     *       si l'item n'existe pas, il est simplement ignoré.</li>
     * </ul>
     */
    public record MaterialIngredient(TagKey<Item> tag, List<String> entries) {

        public boolean isGenerated() {
            return !entries.isEmpty();
        }

        /**
         * Construit un ingrédient à partir d'entrées "#namespace:tag" ou "namespace:item".
         * Une seule entrée de type tag est utilisée directement, sans générer de tag.
         */
        static MaterialIngredient of(String generatedTagPath, String... entries) {
            if (entries.length == 0) {
                throw new IllegalArgumentException("Aucun ingrédient pour " + generatedTagPath);
            }
            if (entries.length == 1 && entries[0].startsWith("#")) {
                return new MaterialIngredient(itemTag(entries[0].substring(1)), List.of());
            }
            TagKey<Item> tag = TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "weapon_materials/" + generatedTagPath));
            return new MaterialIngredient(tag, List.of(entries));
        }
    }

    public static final class Builder {
        private final String id;
        @Nullable private Tier tier;
        @Nullable private Double damageMultiplier;
        @Nullable private MaterialIngredient craftingIngredient;
        @Nullable private SmithingUpgrade smithingUpgrade;
        private List<String> requiredMods = List.of();
        // Stats d'un matériau moddé, transformées en Tier au build()
        private int durability = -1;
        private int enchantability;
        private float attackBonus;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id);
        }

        private Builder tier(Tier tier) {
            this.tier = tier;
            return this;
        }

        /**
         * Stats d'un matériau moddé. Références vanilla (durabilité / enchantabilité / bonus) :
         * bois 59/15/0, pierre 131/5/1, fer 250/14/2, or 32/22/0, diamant 1561/10/3, netherite 2031/15/4.
         * Les dégâts de chaque arme sont multipliés par (4 + bonus) / 4 (fer ×1.5, diamant ×1.75).
         * L'objet de réparation à l'enclume est l'ingrédient du matériau.
         */
        public Builder stats(int durability, int enchantability, float attackBonus) {
            this.durability = durability;
            this.enchantability = enchantability;
            this.attackBonus = attackBonus;
            return this;
        }

        /** Remplace le multiplicateur de dégâts calculé à partir du bonus du Tier. */
        public Builder damageMultiplier(double multiplier) {
            this.damageMultiplier = multiplier;
            return this;
        }

        /** Fabriqué à l'établi avec un tag existant, qui remplace "M" dans le motif de chaque arme. */
        public Builder craftedWith(TagKey<Item> ingredient) {
            this.craftingIngredient = new MaterialIngredient(ingredient, List.of());
            this.smithingUpgrade = null;
            return this;
        }

        /**
         * Fabriqué à l'établi avec une liste d'entrées optionnelles, ex :
         * {@code craftedFrom("#c:ingots/manasteel", "botania:manasteel_ingot")}.
         * Mettre le tag commun en premier, puis l'item exact du mod en secours.
         */
        public Builder craftedFrom(String... entries) {
            this.craftingIngredient = MaterialIngredient.of(id, entries);
            this.smithingUpgrade = null;
            return this;
        }

        /**
         * Obtenu à la table de forge en améliorant l'arme du matériau {@code base}.
         * {@code template} et {@code addition} acceptent "#tag" ou "namespace:item".
         */
        public Builder smithingUpgrade(WeaponMaterial base, String template, String addition) {
            this.smithingUpgrade = new SmithingUpgrade(base,
                    MaterialIngredient.of(id + "_template", template),
                    MaterialIngredient.of(id + "_addition", addition));
            this.craftingIngredient = null;
            return this;
        }

        /** Le matériau (items, config, recettes) n'existe que si au moins un de ces mods est installé. */
        public Builder requiresAnyMod(String... modIds) {
            this.requiredMods = List.of(modIds);
            return this;
        }

        public WeaponMaterial build() {
            if (craftingIngredient == null && smithingUpgrade == null) {
                throw new IllegalStateException("Le matériau " + id + " n'a ni ingrédient ni amélioration");
            }
            return new WeaponMaterial(this, tier != null ? tier : createTier());
        }

        private Tier createTier() {
            if (durability < 0) {
                throw new IllegalStateException("Le matériau " + id + " doit avoir un Tier vanilla ou des stats()");
            }
            // Réparation : l'ingrédient du matériau (ou l'ajout de l'amélioration). Lu à la demande, donc
            // les tags n'ont pas besoin d'être chargés au démarrage.
            TagKey<Item> repairTag = craftingIngredient != null ? craftingIngredient.tag() : smithingUpgrade.addition().tag();
            // Le tag de blocs n'a pas d'effet pour une épée (elle ne mine que les toiles d'araignée).
            return new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, durability, 6.0F, attackBonus, enchantability,
                    () -> Ingredient.of(repairTag));
        }
    }
}
