package com.betterweaponry;

import com.betterweaponry.config.ConfigFolder;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModAttributes;
import com.betterweaponry.registry.ModCreativeTabs;
import com.betterweaponry.registry.ModEffects;
import com.betterweaponry.registry.ModEntities;
import com.betterweaponry.registry.ModItems;
import com.betterweaponry.registry.ModRecipes;
import com.betterweaponry.registry.ModSounds;
import com.betterweaponry.registry.ModStats;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Le cœur du mod, commun à tous les chargeurs. Chaque chargeur a son propre point d'entrée, qui installe sa
 * {@link Platform} puis appelle {@link #init()}.
 * <p>
 * Les stats des armes et les règles sont des fichiers JSON (voir {@link ConfigFolder} et
 * {@link com.betterweaponry.data.WeaponDataLoader}).
 */
public final class BetterWeaponry {
    public static final String MODID = "better_weaponry";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Langue du README et des schémas du dossier de config : celle du jeu côté client, l'anglais sur un serveur dédié. */
    private static volatile Supplier<String> languageSupplier = () -> "en_us";

    private BetterWeaponry() {
    }

    /** Déclare tout le contenu du mod (objets, sons, effets...). */
    public static void init() {
        ModItems.init();
        ModCreativeTabs.init();
        ModStats.init();
        ModEntities.init();
        ModAttributes.init();
        ModSounds.init();
        ModRecipes.init();
        ModEffects.init();
    }

    /** Appelé par le point d'entrée client pour écrire la documentation dans la langue du joueur. */
    public static void setLanguageSupplier(Supplier<String> supplier) {
        languageSupplier = supplier;
    }

    /** Prépare config/better_weaponry/ (et convertit les anciennes configs) avant le chargement de tout monde. */
    public static void prepareConfigFolder() {
        if (Platform.get().isDataGen()) return;
        ConfigFolder.prepare(languageSupplier.get());
    }
}
