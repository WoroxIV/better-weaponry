package com.betterweaponry.data;

import com.betterweaponry.BetterWeaponry;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Lit des fichiers directement dans le jar du mod, sans passer par les datapacks ni les packs de ressources.
 * Sert aux valeurs par défaut (avant le chargement d'un monde) et aux traductions du README et du schéma.
 */
public final class ModFiles {

    private ModFiles() {
    }

    /** Contenu texte d'un fichier du jar, ex : {@code read("data", "better_weaponry", "...", "katana.json")}. */
    public static Optional<String> read(String... path) {
        IModFileInfo info = ModList.get().getModFileById(BetterWeaponry.MODID);
        if (info == null) return Optional.empty();
        Path file = info.getFile().findResource(path);
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("Could not read {} from the mod jar", String.join("/", path), e);
            return Optional.empty();
        }
    }
}
