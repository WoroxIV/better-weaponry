package com.betterweaponry.datagen;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.GearType;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <ul>
 *   <li>Ajoute toutes les armes au tag minecraft:swords (enchantements d'épée : Tranchant, Butin, Affilage...).</li>
 *   <li>Génère les tags {@code better_weaponry:melee_weapons} et {@code better_weaponry:dual_wieldable}.</li>
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
        addWeaponTags();
        addShields();
        addEnchantable();
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
     * Armes de mêlée (épées, haches, trident), plus le tag commun que beaucoup de mods remplissent.
     * Le dual wield est réservé aux armes qui ont l'option "dual_wielded" : son tag reste vide par défaut,
     * et un datapack peut y ajouter des armes d'autres mods.
     */
    private void addWeaponTags() {
        tag(ModTags.DUAL_WIELDABLE);
        tag(ModTags.MELEE_WEAPONS)
                .addTag(ItemTags.SWORDS)
                .addTag(ItemTags.AXES)
                .add(Items.TRIDENT)
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "tools/melee_weapon"));
    }

    /** Boucliers : tag commun, et enchantements du bouclier vanilla (Solidité, Raccommodage, Malédiction de disparition). */
    private void addShields() {
        var shields = tag(ModTags.SHIELDS);
        for (WeaponMaterial material : WeaponMaterials.all()) {
            if (material.isModded()) {
                shields.addOptional(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, material.itemName(GearType.SHIELD)));
            } else {
                shields.add(ModItems.shield(material).get());
            }
        }
        tag(Tags.Items.TOOLS_SHIELD).addTag(ModTags.SHIELDS);
        tag(ItemTags.DURABILITY_ENCHANTABLE).addTag(ModTags.SHIELDS);
    }

    /** Objets qui acceptent les enchantements du mod, d'après les armes par défaut. Un datapack peut en ajouter. */
    private void addEnchantable() {
        tag(ModTags.ENCHANTABLE_PARRYING).addTag(Tags.Items.TOOLS_SHIELD).addTag(ModTags.ENCHANTABLE_PARRY_WEAPONS);
        for (WeaponType type : WeaponType.values()) {
            WeaponAbility ability = WeaponData.builtin().weapon(type).ability();
            if (ability == WeaponAbility.PARRY) addWeapons(ModTags.ENCHANTABLE_PARRY_WEAPONS, type);
            if (ability == WeaponAbility.DASH) addWeapons(ModTags.ENCHANTABLE_DASHING, type);
        }
        for (WeaponType type : List.of(WeaponType.SICKLE, WeaponType.SCYTHE, WeaponType.KATANA)) addWeapons(ModTags.ENCHANTABLE_BLEEDING, type);
    }

    private void addWeapons(TagKey<Item> tagKey, WeaponType type) {
        var builder = tag(tagKey);
        for (WeaponMaterial material : WeaponMaterials.all()) {
            builder.addOptional(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, material.itemName(type)));
        }
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
