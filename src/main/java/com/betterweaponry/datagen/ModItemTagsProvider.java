package com.betterweaponry.datagen;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModItems;
import com.betterweaponry.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * <ul>
 *   <li>Ajoute toutes les armes au tag minecraft:swords (enchantements d'épée : Tranchant, Butin, Affilage...).</li>
 *   <li>Génère le tag {@code better_weaponry:dual_wieldable} (armes utilisables en dual wield).</li>
 *   <li>Génère les tags d'ingrédients des matériaux ({@code better_weaponry:weapon_materials/...}).</li>
 * </ul>
 * Tout ce qui vient d'un autre mod est ajouté en "optionnel" : si le mod est absent, l'entrée est ignorée
 * au lieu de faire échouer le chargement du tag entier.
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, BetterWeaponry.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        addSwords();
        addDualWieldable();
        for (WeaponMaterial material : WeaponMaterials.all()) {
            addIngredientTag(material.craftingIngredient());
            if (material.smithingUpgrade() != null) {
                addIngredientTag(material.smithingUpgrade().template());
                addIngredientTag(material.smithingUpgrade().addition());
            }
        }
    }

    private void addSwords() {
        var swords = tag(ItemTags.SWORDS);
        for (WeaponType type : WeaponType.values()) {
            for (WeaponMaterial material : WeaponMaterials.all()) {
                Item item = ModItems.get(type, material).get();
                if (material.isModded()) {
                    // Item absent sans son mod : une entrée obligatoire casserait tout minecraft:swords.
                    swords.addOptional(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, material.itemName(type)));
                } else {
                    swords.add(item);
                }
            }
        }
    }

    /**
     * Armes utilisables en dual wield. Même comportement qu'avant (épées, haches, trident), plus le tag commun
     * des armes de mêlée, que beaucoup de mods remplissent. Un datapack peut ajouter ou retirer des objets.
     */
    private void addDualWieldable() {
        tag(ModTags.DUAL_WIELDABLE)
                .addTag(ItemTags.SWORDS)
                .addTag(ItemTags.AXES)
                .add(Items.TRIDENT)
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "tools/melee_weapon"));
    }

    private void addIngredientTag(@Nullable WeaponMaterial.MaterialIngredient ingredient) {
        if (ingredient == null || !ingredient.isGenerated()) return; // tag vanilla/commun existant

        var tag = tag(ingredient.tag());
        for (String entry : ingredient.entries()) {
            if (entry.startsWith("#")) {
                tag.addOptionalTag(ResourceLocation.parse(entry.substring(1)));
            } else {
                tag.addOptional(ResourceLocation.parse(entry));
            }
        }
    }
}
