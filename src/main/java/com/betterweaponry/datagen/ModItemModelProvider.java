package com.betterweaponry.datagen;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Génère un modèle par arme : la texture "item/&lt;nom&gt;" posée sur le modèle de base du type
 * (qui définit la position en main), ou sur "item/handheld" vanilla si le type n'en a pas.
 * <p>
 * Une texture manquante n'arrête pas le datagen : le modèle est généré quand même (l'item apparaîtra
 * en violet et noir) et la liste des textures à dessiner est affichée dans la console.
 */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, BetterWeaponry.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        List<String> missingTextures = new ArrayList<>();

        for (WeaponType type : WeaponType.values()) {
            ResourceLocation parent = type.baseModel() != null
                    ? modLoc("item/" + type.baseModel())
                    : mcLoc("item/handheld");

            for (WeaponMaterial material : WeaponMaterials.all()) {
                String name = material.itemName(type);
                ResourceLocation texture = modLoc("item/" + name);
                if (!existingFileHelper.exists(texture, ModelProvider.TEXTURE)) {
                    // Déclarée comme "générée" pour que le builder accepte de la référencer.
                    existingFileHelper.trackGenerated(texture, ModelProvider.TEXTURE);
                    missingTextures.add(name);
                }
                withExistingParent(name, parent).texture("layer0", texture);
            }
        }

        if (!missingTextures.isEmpty()) {
            BetterWeaponry.LOGGER.warn("{} textures d'armes manquantes dans textures/item/ : {}",
                    missingTextures.size(), String.join(", ", missingTextures));
        }
    }
}
