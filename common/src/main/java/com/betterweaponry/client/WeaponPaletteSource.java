package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Fabrique au chargement des ressources la texture de chaque arme : un modèle de couleurs par type, une palette par matériau.
 * <ul>
 *   <li>{@code textures/templates/<dossier>/katana.png} : le modèle de couleurs ;</li>
 *   <li>{@code textures/palettes/key.png} : les couleurs de référence, une par pixel ;</li>
 *   <li>{@code textures/palettes/iron.png} : les couleurs du matériau, dans le même ordre. Les couleurs absentes de la clé
 *       ne changent pas (manche, détails).</li>
 * </ul>
 * Le résultat s'appelle {@code <dossier>/iron_katana}, exactement comme une texture dessinée à la main. Si le fichier
 * {@code textures/<dossier>/iron_katana.png} existe, rien n'est généré : c'est lui qui est utilisé.
 * <p>
 * Déclarée dans {@code assets/minecraft/atlases/} pour les atlas où vivent les textures concernées.
 */
public final class WeaponPaletteSource implements SpriteSource {
    private static final Codec<WeaponPaletteSource> CODEC = Codec.STRING.fieldOf("directory")
            .xmap(WeaponPaletteSource::new, source -> source.directory).codec();
    /** Déclaré au premier appel, qui doit avoir lieu avant le chargement des textures. */
    public static final SpriteSourceType TYPE = SpriteSources.register(BetterWeaponry.MODID + ":weapon_palettes", CODEC);

    /** Dossier de la texture finale dans "textures/" : "item" ou "entity/shield". */
    private final String directory;

    private WeaponPaletteSource(String directory) {
        this.directory = directory;
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }

    @Override
    public void run(ResourceManager manager, Output output) {
        FileToIdConverter templates = new FileToIdConverter("textures/templates/" + directory, ".png");
        Map<ResourceLocation, Resource> files = templates.listMatchingResources(manager);
        files.keySet().removeIf(file -> !file.getNamespace().equals(BetterWeaponry.MODID));
        if (files.isEmpty()) return;

        Map<WeaponMaterial, IntUnaryOperator> palettes = loadPalettes(manager);
        files.forEach((file, resource) -> generate(manager, output, templates.fileToId(file).getPath(), file, resource, palettes));
    }

    private void generate(ResourceManager manager, Output output, String template, ResourceLocation file, Resource resource,
                          Map<WeaponMaterial, IntUnaryOperator> palettes) {
        Map<ResourceLocation, IntUnaryOperator> targets = new LinkedHashMap<>();
        palettes.forEach((material, palette) -> {
            ResourceLocation target = new ResourceLocation(BetterWeaponry.MODID, directory + "/" + material.id() + "_" + template);
            boolean handDrawn = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(target)).isPresent();
            if (!handDrawn) targets.put(target, palette);
        });
        if (targets.isEmpty()) return;

        // L'image est libérée quand le compteur tombe à zéro : il doit valoir exactement le nombre de textures créées.
        LazyLoadedImage image = new LazyLoadedImage(file, resource, targets.size());
        targets.forEach((target, palette) -> output.add(target, new PalettedPermutations.PalettedSpriteSupplier(image, () -> palette, target)));
    }

    /** Une palette absente ou de la mauvaise taille n'empêche pas le jeu de démarrer : l'arme apparaît en violet et noir. */
    private static Map<WeaponMaterial, IntUnaryOperator> loadPalettes(ResourceManager manager) {
        Map<WeaponMaterial, IntUnaryOperator> palettes = new LinkedHashMap<>();
        int[] key = readPalette(manager, "key");
        if (key == null) {
            BetterWeaponry.LOGGER.warn("textures/palettes/key.png est absent ou illisible : aucune texture d'arme générée");
            return palettes;
        }

        List<String> problems = new ArrayList<>();
        for (WeaponMaterial material : WeaponMaterials.active()) {
            int[] colors = readPalette(manager, material.id());
            if (colors != null && colors.length == key.length) {
                palettes.put(material, recolor(key, colors));
            } else {
                problems.add(material.id());
            }
        }
        if (!problems.isEmpty()) {
            BetterWeaponry.LOGGER.warn("Palette absente, illisible ou différente de key.png ({} couleurs) dans textures/palettes/ pour : {}",
                    key.length, String.join(", ", problems));
        }
        return palettes;
    }

    @Nullable
    private static int[] readPalette(ResourceManager manager, String name) {
        ResourceLocation palette = new ResourceLocation(BetterWeaponry.MODID, "palettes/" + name);
        if (manager.getResource(TEXTURE_ID_CONVERTER.idToFile(palette)).isEmpty()) return null;
        try {
            return PalettedPermutations.loadPaletteEntryFromImage(manager, palette);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** Même calcul que les garnitures d'armure vanilla : la transparence du modèle est conservée. */
    private static IntUnaryOperator recolor(int[] key, int[] colors) {
        Int2IntMap mapping = new Int2IntOpenHashMap(key.length);
        for (int i = 0; i < key.length; i++) {
            if (FastColor.ABGR32.alpha(key[i]) != 0) mapping.put(FastColor.ABGR32.transparent(key[i]), colors[i]);
        }
        return pixel -> {
            int alpha = FastColor.ABGR32.alpha(pixel);
            if (alpha == 0) return pixel;
            int color = FastColor.ABGR32.transparent(pixel);
            int replacement = mapping.getOrDefault(color, FastColor.ABGR32.opaque(color));
            return FastColor.ABGR32.color(alpha * FastColor.ABGR32.alpha(replacement) / 255, replacement);
        };
    }
}
