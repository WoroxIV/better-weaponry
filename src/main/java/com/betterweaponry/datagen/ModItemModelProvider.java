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
 * Génère un modèle par arme : la texture "item/&lt;nom&gt;" posée sur "models/item/&lt;type&gt;_base.json"
 * (qui définit la position en main), ou sur "item/handheld" vanilla si le type n'en a pas.
 * Un fichier "models/item/&lt;nom&gt;.json" écrit à la main dans src/main/resources remplace le modèle généré.
 * <p>
 * La texture est dessinée à la main ou fabriquée au chargement par {@code WeaponPaletteSource}. Sans aucune des deux,
 * le datagen continue : le modèle est généré quand même (l'item apparaîtra en violet et noir)
 * et la liste des textures à dessiner est affichée dans la console.
 */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, BetterWeaponry.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        List<String> missingTextures = new ArrayList<>();

        for (WeaponType type : WeaponType.values()) {
            ResourceLocation base = modLoc("item/" + type.id() + "_base");
            ResourceLocation parent = existingFileHelper.exists(base, MODEL) ? base : mcLoc("item/handheld");

            for (WeaponMaterial material : WeaponMaterials.all()) {
                String name = material.itemName(type);
                if (isHandWritten(name)) continue;
                ResourceLocation texture = modLoc("item/" + name);
                if (!hasTexture(name, type.id())) {
                    // Déclarée comme "générée" pour que le builder accepte de la référencer.
                    existingFileHelper.trackGenerated(texture, ModelProvider.TEXTURE);
                    missingTextures.add(name);
                }
                withExistingParent(name, parent).texture("layer0", texture);
            }
        }

        // Même modèle que le bouclier vanilla, et son modèle "levé" pendant le blocage. La texture est choisie par ShieldItemRenderer.
        for (WeaponMaterial material : WeaponMaterials.all()) {
            String name = material.itemName(GearType.SHIELD);
            if (isHandWritten(name)) continue;
            withExistingParent(name, mcLoc("item/shield"))
                    .override()
                    .predicate(mcLoc("blocking"), 1.0F)
                    .model(new ModelFile.UncheckedModelFile(mcLoc("item/shield_blocking")))
                    .end();
        }

        for (WeaponMaterial material : WeaponMaterials.all()) {
            String prefix = material.id() + "_";
            addBowModels(prefix);
            addCrossbowModels(prefix);
            addShurikenModel(prefix);
        }

        if (!missingTextures.isEmpty()) {
            BetterWeaponry.LOGGER.warn("{} textures d'armes manquantes dans textures/item/ : {}",
                    missingTextures.size(), String.join(", ", missingTextures));
        }
    }

    /** Un fichier models/item/&lt;nom&gt;.json existe dans src/main/resources : ne pas générer le même chemin. */
    private boolean isHandWritten(String name) {
        return existingFileHelper.exists(modLoc("item/" + name), MODEL);
    }

    /** La texture est dessinée à la main, ou sera fabriquée au chargement à partir du modèle de couleurs. */
    private boolean hasTexture(String name, String template) {
        ResourceLocation own = modLoc("item/" + name);
        if (existingFileHelper.exists(own, ModelProvider.TEXTURE)) return true;
        if (!existingFileHelper.exists(modLoc("templates/item/" + template), ModelProvider.TEXTURE)) return false;
        existingFileHelper.trackGenerated(own, ModelProvider.TEXTURE);
        return true;
    }

    /** Texture propre à l'objet si elle existe, sinon la texture de repli (relancer runData après avoir ajouté le premier fichier). */
    private ResourceLocation textureOr(String prefix, String template, ResourceLocation fallback) {
        return hasTexture(prefix + template, template) ? modLoc("item/" + prefix + template) : fallback;
    }

    private void addLayeredModel(String name, ResourceLocation parent, ResourceLocation texture) {
        if (!isHandWritten(name)) withExistingParent(name, parent).texture("layer0", texture);
    }

    private ModelFile.UncheckedModelFile model(String name) {
        return new ModelFile.UncheckedModelFile(modLoc("item/" + name));
    }

    /** Pas d'équivalent vanilla : un matériau sans texture propre prend la texture générique. */
    private void addShurikenModel(String prefix) {
        String template = GearType.SHURIKEN.id();
        addLayeredModel(prefix + template, mcLoc("item/handheld"), textureOr(prefix, template, modLoc("item/shuriken")));
    }

    /** Mêmes états que l'arc vanilla : repos, puis trois étapes de bandage. */
    private void addBowModels(String prefix) {
        String bow = GearType.BOW.id();
        String name = prefix + bow;
        for (int stage = 0; stage < 3; stage++) {
            String state = bow + "_pulling_" + stage;
            addLayeredModel(prefix + state, mcLoc("item/bow"), textureOr(prefix, state, mcLoc("item/" + state)));
        }
        if (isHandWritten(name)) return;
        ResourceLocation pulling = mcLoc("pulling");
        ResourceLocation pull = mcLoc("pull");
        withExistingParent(name, mcLoc("item/bow")).texture("layer0", textureOr(prefix, bow, mcLoc("item/bow")))
                .override().predicate(pulling, 1.0F).model(model(name + "_pulling_0")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 0.65F).model(model(name + "_pulling_1")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 0.9F).model(model(name + "_pulling_2")).end();
    }

    /** Mêmes états que l'arbalète vanilla : repos, trois étapes de chargement, chargée d'une flèche ou d'une fusée. */
    private void addCrossbowModels(String prefix) {
        String crossbow = GearType.CROSSBOW.id();
        String name = prefix + crossbow;
        for (int stage = 0; stage < 3; stage++) {
            String state = crossbow + "_pulling_" + stage;
            addLayeredModel(prefix + state, mcLoc("item/crossbow"), textureOr(prefix, state, mcLoc("item/" + state)));
        }
        for (String loaded : new String[]{crossbow + "_arrow", crossbow + "_firework"}) {
            addLayeredModel(prefix + loaded, mcLoc("item/crossbow"), textureOr(prefix, loaded, mcLoc("item/" + loaded)));
        }
        if (isHandWritten(name)) return;
        ResourceLocation pulling = mcLoc("pulling");
        ResourceLocation pull = mcLoc("pull");
        ResourceLocation charged = mcLoc("charged");
        withExistingParent(name, mcLoc("item/crossbow")).texture("layer0", textureOr(prefix, crossbow, mcLoc("item/crossbow_standby")))
                .override().predicate(pulling, 1.0F).model(model(name + "_pulling_0")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 0.58F).model(model(name + "_pulling_1")).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 1.0F).model(model(name + "_pulling_2")).end()
                .override().predicate(charged, 1.0F).model(model(name + "_arrow")).end()
                .override().predicate(charged, 1.0F).predicate(mcLoc("firework"), 1.0F).model(model(name + "_firework")).end();
    }
}
