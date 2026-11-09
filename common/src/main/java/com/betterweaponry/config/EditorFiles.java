package com.betterweaponry.config;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.editor.EditorDocument;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Écriture des fichiers par l'éditeur en jeu. Seules les différences avec les couches inférieures sont écrites :
 * le reste continue de suivre les valeurs du mod. Attention : les commentaires d'un fichier réécrit sont perdus.
 */
public final class EditorFiles {

    private EditorFiles() {
    }

    /**
     * Cible "tous les mondes" : config/better_weaponry. Le fichier reste en place (avec le lien vers le schéma)
     * même vide, pour que l'utilisateur puisse y ajouter des champs à la main.
     */
    public static Path writeGlobal(EditorDocument document, JsonObject differences) throws IOException {
        Path file = document.configFile();
        ConfigFolder.writeJson(file, ConfigFolder.withSchema(differences, document.schemaPath()));
        return file;
    }

    /**
     * Cible "ce monde seulement" : le datapack de l'éditeur dans le dossier du monde (créé au besoin).
     * Le fichier est supprimé s'il n'y a plus aucune différence.
     *
     * @return le fichier écrit (ou supprimé)
     */
    public static Path writeWorld(Path worldRoot, EditorDocument document, JsonObject differences) throws IOException {
        Path pack = editorPack(worldRoot);
        Path metadata = pack.resolve("pack.mcmeta");
        if (!Files.exists(metadata)) ConfigFolder.writeJson(metadata, packMetadata());

        Path file = pack.resolve("data").resolve(BetterWeaponry.MODID).resolve(WeaponDataLoader.DATA_DIRECTORY)
                .resolve(document.relativePath());
        if (differences.size() == 0) {
            Files.deleteIfExists(file);
        } else {
            ConfigFolder.writeJson(file, differences);
        }
        return file;
    }

    public static Path editorPack(Path worldRoot) {
        return worldRoot.resolve("datapacks").resolve(WeaponDataLoader.EDITOR_PACK);
    }

    /** Chemin affiché dans les messages, relatif au dossier du jeu. */
    public static String displayPath(Path file) {
        return ConfigFolder.displayPath(file);
    }

    private static JsonObject packMetadata() {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
        pack.addProperty("description", "Better Weaponry: settings made with the in-game editor for this world");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }
}
