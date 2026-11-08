package com.betterweaponry.datagen;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.GearType;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponRecipe;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Génère les recettes de toutes les armes à partir du motif de chaque {@link WeaponType} :
 * <ul>
 *   <li>matériau "fabriqué" : recette d'établi, "M" remplacé par le tag du matériau, "S" par un bâton ;</li>
 *   <li>matériau "amélioré" (netherite) : recette de table de forge depuis l'arme du matériau de base.</li>
 * </ul>
 * Les recettes des matériaux moddés reçoivent une condition "mod chargé" pour ne pas créer
 * d'erreurs au chargement quand le mod est absent.
 */
public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    /** Forme du bouclier vanilla : les planches sont remplacées par le matériau, le lingot de fer central reste. */
    private static final WeaponRecipe SHIELD_RECIPE = WeaponRecipe.of("MoM", "MMM", " M ").with('o', Tags.Items.INGOTS_IRON);

    /** Forme de l'arc vanilla : les bâtons sont remplacés par le matériau (l'arc en bois garde les bâtons). */
    private static final WeaponRecipe BOW_RECIPE = WeaponRecipe.of(" MX", "M X", " MX").with('X', Tags.Items.STRINGS);
    /** Forme de l'arbalète vanilla, sur le même principe. */
    private static final WeaponRecipe CROSSBOW_RECIPE = WeaponRecipe.of("MIM", "XHX", " M ")
            .with('I', Tags.Items.INGOTS_IRON).with('X', Tags.Items.STRINGS).withItem('H', () -> Items.TRIPWIRE_HOOK);

    @Override
    protected void buildRecipes(RecipeOutput output) {
        for (WeaponType type : WeaponType.values()) {
            WeaponRecipe recipe = type.recipe();
            if (recipe == null) continue; // pas encore de motif pour ce type

            for (WeaponMaterial material : WeaponMaterials.all()) {
                addRecipes(output, material, recipe, ModItems.get(type, material).get(), material.itemName(type),
                        base -> ModItems.get(type, base).get());
            }
        }
        for (WeaponMaterial material : WeaponMaterials.all()) {
            addRecipes(output, material, SHIELD_RECIPE, ModItems.shield(material).get(), material.itemName(GearType.SHIELD),
                    base -> ModItems.shield(base).get());
            addRecipes(output, material, BOW_RECIPE, ModItems.bow(material).get(), material.itemName(GearType.BOW),
                    base -> ModItems.bow(base).get());
            addRecipes(output, material, CROSSBOW_RECIPE, ModItems.crossbow(material).get(), material.itemName(GearType.CROSSBOW),
                    base -> ModItems.crossbow(base).get());
        }
    }

    /** Recette d'établi, ou de table de forge pour un matériau obtenu par amélioration. */
    private void addRecipes(RecipeOutput output, WeaponMaterial material, WeaponRecipe recipe, Item result, String name,
                            Function<WeaponMaterial, Item> itemOfMaterial) {
        RecipeOutput conditionalOutput = withLoadConditions(output, material);
        if (material.craftingIngredient() != null) {
            boolean sticks = material == WeaponMaterials.WOODEN && (recipe == BOW_RECIPE || recipe == CROSSBOW_RECIPE);
            shaped(conditionalOutput, result, name, recipe, sticks ? Tags.Items.RODS_WOODEN : material.craftingIngredient().tag());
        } else if (material.smithingUpgrade() != null) {
            smithing(conditionalOutput, result, name, itemOfMaterial.apply(material.smithingUpgrade().base()), material.smithingUpgrade());
        }
    }

    private void shaped(RecipeOutput output, Item result, String name, WeaponRecipe recipe, TagKey<Item> materialTag) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result);
        recipe.pattern().forEach(builder::pattern);

        // On ne définit que les symboles présents : un ingrédient inutilisé fait échouer le datagen.
        if (recipe.uses(WeaponRecipe.MATERIAL)) builder.define(WeaponRecipe.MATERIAL, materialTag);
        if (recipe.uses(WeaponRecipe.STICK)) builder.define(WeaponRecipe.STICK, Tags.Items.RODS_WOODEN);
        for (Map.Entry<Character, TagKey<Item>> extra : recipe.extraIngredients().entrySet()) {
            if (recipe.uses(extra.getKey())) builder.define(extra.getKey(), extra.getValue());
        }
        recipe.extraItems().forEach((symbol, item) -> builder.define(symbol, item.get()));

        builder.unlockedBy("has_material", has(materialTag))
                .save(output, recipeId(name));
    }

    private void smithing(RecipeOutput output, Item result, String name, Item base, WeaponMaterial.SmithingUpgrade upgrade) {
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(upgrade.template().tag()),
                        Ingredient.of(base),
                        Ingredient.of(upgrade.addition().tag()),
                        RecipeCategory.COMBAT,
                        result)
                .unlocks("has_addition", has(upgrade.addition().tag()))
                .save(output, recipeId(name + "_smithing"));
    }

    /**
     * Conditions de chargement des recettes de matériaux moddés (rien pour le vanilla) :
     * <ul>
     *   <li>un des mods requis est chargé (sinon l'arme n'existe pas et la recette planterait au chargement) ;</li>
     *   <li>chaque tag d'ingrédient contient au moins un item (sinon la recette serait impossible à faire).</li>
     * </ul>
     */
    private static RecipeOutput withLoadConditions(RecipeOutput output, WeaponMaterial material) {
        List<ICondition> conditions = new ArrayList<>();
        addModCondition(conditions, material);

        if (material.craftingIngredient() != null) {
            addTagNotEmpty(conditions, material, material.craftingIngredient());
        }
        WeaponMaterial.SmithingUpgrade upgrade = material.smithingUpgrade();
        if (upgrade != null) {
            addModCondition(conditions, upgrade.base());
            addTagNotEmpty(conditions, material, upgrade.template());
            addTagNotEmpty(conditions, material, upgrade.addition());
        }
        return conditions.isEmpty() ? output : output.withConditions(conditions.toArray(ICondition[]::new));
    }

    private static void addModCondition(List<ICondition> conditions, WeaponMaterial material) {
        List<String> mods = material.requiredMods();
        if (mods.size() == 1) {
            conditions.add(new ModLoadedCondition(mods.get(0)));
        } else if (mods.size() > 1) {
            conditions.add(new OrCondition(mods.stream().<ICondition>map(ModLoadedCondition::new).toList()));
        }
    }

    private static void addTagNotEmpty(List<ICondition> conditions, WeaponMaterial material, WeaponMaterial.MaterialIngredient ingredient) {
        if (material.isModded()) {
            conditions.add(new NotCondition(new TagEmptyCondition(ingredient.tag().location())));
        }
    }

    private static ResourceLocation recipeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, path);
    }
}
