package com.betterweaponry.datagen;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.GearType;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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

        // Même modèle que le bouclier vanilla, et son modèle "levé" pendant le blocage. La texture est choisie par ShieldItemRenderer.
        for (WeaponMaterial material : WeaponMaterials.all()) {
            withExistingParent(material.itemName(GearType.SHIELD), mcLoc("item/shield"))
                    .override()
                    .predicate(mcLoc("blocking"), 1.0F)
                    .model(new ModelFile.UncheckedModelFile(mcLoc("item/shield_blocking")))
                    .end();
        }

        for (WeaponMaterial material : WeaponMaterials.all()) {
            addBowModels(material.itemName(GearType.BOW));
            addCrossbowModels(material.itemName(GearType.CROSSBOW));
            addShurikenModel(material.itemName(GearType.SHURIKEN));
        }

        if (!missingTextures.isEmpty()) {
            BetterWeaponry.LOGGER.warn("{} textures d'armes manquantes dans textures/item/ : {}",
                    missingTextures.size(), String.join(", ", missingTextures));
        }
    }

    /** Texture propre à l'objet si le fichier existe, sinon celle de l'objet vanilla (relancer runData après avoir ajouté le fichier). */
    private ResourceLocation textureOr(String name, String vanillaTexture) {
        ResourceLocation own = modLoc("item/" + name);
        return existingFileHelper.exists(own, ModelProvider.TEXTURE) ? own : mcLoc("item/" + vanillaTexture);
    }

    private ModelFile.UncheckedModelFile model(String name) {
        return new ModelFile.UncheckedModelFile(modLoc("item/" + name));
    }

    /** Pas d'équivalent vanilla : un matériau sans texture propre prend la texture générique. */
    private void addShurikenModel(String name) {
        ResourceLocation own = modLoc("item/" + name);
        withExistingParent(name, mcLoc("item/handheld")).texture("layer0", existingFileHelper.exists(own, ModelProvider.TEXTURE) ? own : modLoc("item/shuriken"));
    }

    /** Mêmes états que l'arc vanilla : repos, puis trois étapes de bandage. */
    private void addBowModels(String name) {
        for (int stage = 0; stage < 3; stage++) {
            withExistingParent(name + "_pulling_" + stage, mcLoc("item/bow")).texture("layer0", textureOr(name + "_pulling_" + stage, "bow_pulling_" + stage));
        }
        ResourceLocation pulling = mcLoc("pulling");
        ResourceLocation pull = mcLoc("pull");
        withExistingParent(name, mcLoc("item/bow")).texture("layer0", textureOr(name, "bow"))
                .override().predicate(pulling, 1.0F).model(model(name + "_pulling_0")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 0.65F).model(model(name + "_pulling_1")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 0.9F).model(model(name + "_pulling_2")).end();
    }

    /** Mêmes états que l'arbalète vanilla : repos, trois étapes de chargement, chargée d'une flèche ou d'une fusée. */
    private void addCrossbowModels(String name) {
        for (int stage = 0; stage < 3; stage++) {
            withExistingParent(name + "_pulling_" + stage, mcLoc("item/crossbow")).texture("layer0", textureOr(name + "_pulling_" + stage, "crossbow_pulling_" + stage));
        }
        withExistingParent(name + "_arrow", mcLoc("item/crossbow")).texture("layer0", textureOr(name + "_arrow", "crossbow_arrow"));
        withExistingParent(name + "_firework", mcLoc("item/crossbow")).texture("layer0", textureOr(name + "_firework", "crossbow_firework"));
        ResourceLocation pulling = mcLoc("pulling");
        ResourceLocation pull = mcLoc("pull");
        ResourceLocation charged = mcLoc("charged");
        withExistingParent(name, mcLoc("item/crossbow")).texture("layer0", textureOr(name, "crossbow_standby"))
                .override().predicate(pulling, 1.0F).model(model(name + "_pulling_0")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 0.58F).model(model(name + "_pulling_1")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 1.0F).model(model(name + "_pulling_2")).end()
                .override().predicate(charged, 1.0F).model(model(name + "_arrow")).end()
                .override().predicate(charged, 1.0F).predicate(mcLoc("firework"), 1.0F).model(model(name + "_firework")).end();
    }
}
