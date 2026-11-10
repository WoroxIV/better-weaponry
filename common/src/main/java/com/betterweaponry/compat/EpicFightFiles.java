package com.betterweaponry.compat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.ConfigFolder;
import com.betterweaponry.data.ModFiles;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.platform.Platform;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Fichiers d'Epic Fight pour les matériaux des autres mods. Epic Fight signale chaque fichier d'un objet absent : le jar ne
 * livre donc que les matériaux vanilla, et ceux des autres mods sont écrits au lancement, seulement pour les mods installés.
 */
public final class EpicFightFiles {
    private static final String FOLDER = "epic_fight_generated";

    /** Type d'arme d'Epic Fight, par arme. La même table que celle de la génération des fichiers du jar. */
    public static final Map<WeaponType, String> TYPES = Map.ofEntries(
            Map.entry(WeaponType.BROADSWORD, "sword"), Map.entry(WeaponType.SPEAR, "spear"), Map.entry(WeaponType.DAGGER, "dagger"),
            Map.entry(WeaponType.SCYTHE, "longsword"), Map.entry(WeaponType.RAPIER, "sword"), Map.entry(WeaponType.SICKLE, "dagger"),
            Map.entry(WeaponType.KATANA, "uchigatana"), Map.entry(WeaponType.BATTLEAXE, "greatsword"), Map.entry(WeaponType.GLAIVE, "spear"),
            Map.entry(WeaponType.HAMMER, "greatsword"), Map.entry(WeaponType.CLAYMORE, "greatsword"), Map.entry(WeaponType.CHAKRAM, "dagger"), Map.entry(WeaponType.SAI, "dagger"));

    private EpicFightFiles() {
    }

    /** Dossier du pack généré. */
    public static Path packFolder() {
        return Platform.get().configDir().resolve(BetterWeaponry.MODID).resolve(FOLDER);
    }

    /** Écrit le pack. Sans Epic Fight, rien n'est écrit. */
    public static void generate() {
        if (!BwCompat.epicFight()) return;
        Path pack = packFolder();
        Path folder = pack.resolve("data").resolve(BetterWeaponry.MODID).resolve("capabilities/weapons");
        try {
            for (WeaponMaterial material : WeaponMaterials.active()) {
                if (!material.isModded()) continue;
                for (WeaponType type : WeaponType.values()) {
                    ConfigFolder.writeJson(folder.resolve(material.itemName(type) + ".json"), weapon(type));
                }
            }
            JsonObject description = new JsonObject();
            description.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
            description.addProperty("description", "Better Weaponry: Epic Fight files for the materials of installed mods");
            JsonObject root = new JsonObject();
            root.add("pack", description);
            ConfigFolder.writeJson(pack.resolve("pack.mcmeta"), root);
        } catch (IOException e) {
            BetterWeaponry.LOGGER.warn("[Better Weaponry] Could not write the Epic Fight files of modded materials", e);
        }
    }

    /** Même contenu que les fichiers du jar : type d'arme, pénétration d'armure et longueur de la zone de touche. */
    private static JsonObject weapon(WeaponType type) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "epicfight:" + TYPES.get(type));
        JsonObject common = new JsonObject();
        common.addProperty("armor_negation", armorPenetration(type));
        common.addProperty("impact", 0.5D);
        common.addProperty("max_strikes", 1);
        common.addProperty("damage_bonus", 0.0D);
        common.addProperty("speed_bonus", 0.0D);
        JsonObject attributes = new JsonObject();
        attributes.add("common", common);
        json.add("attributes", attributes);

        double length = 0.7D + Math.max(0.0D, WeaponData.builtin().weapon(type).attackReach() - 2.0D) * 0.35D;
        JsonObject collider = new JsonObject();
        collider.addProperty("number", 3);
        collider.add("center", vector(0.0D, 0.0D, -length));
        collider.add("size", vector(0.4D, 0.4D, length));
        json.add("collider", collider);
        return json;
    }

    private static double armorPenetration(WeaponType type) {
        Optional<String> file = ModFiles.read("data", BetterWeaponry.MODID, "combat_reforged", "weapons", type.id() + ".json");
        if (file.isEmpty()) return 0.0D;
        JsonObject weapon = JsonParser.parseString(file.get()).getAsJsonObject();
        return weapon.has("armor_penetration") ? weapon.get("armor_penetration").getAsDouble() : 0.0D;
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
