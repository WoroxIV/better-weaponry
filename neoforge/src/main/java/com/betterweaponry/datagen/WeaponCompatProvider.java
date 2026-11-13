package com.betterweaponry.datagen;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.EpicFightFiles;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.GearType;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Génère les fichiers qui font reconnaître les armes par Better Combat
 * ({@code data/better_weaponry/weapon_attributes/}) et Epic Fight ({@code data/better_weaponry/capabilities/weapons/}).
 * Une seule table par arme : si une arme y manque, {@code runData} échoue.
 */
public final class WeaponCompatProvider implements DataProvider {
    private static final double PLAYER_BASE_REACH = 3.0D;

    private static final Map<WeaponType, String> TABLE = new EnumMap<>(Map.ofEntries(
            Map.entry(WeaponType.BROADSWORD, "sword"),
            Map.entry(WeaponType.SPEAR, "spear"),
            Map.entry(WeaponType.DAGGER, "dagger"),
            Map.entry(WeaponType.SCYTHE, "scythe"),
            Map.entry(WeaponType.RAPIER, "rapier"),
            Map.entry(WeaponType.SICKLE, "sickle"),
            Map.entry(WeaponType.KATANA, "katana"),
            Map.entry(WeaponType.BATTLEAXE, "heavy_axe"),
            Map.entry(WeaponType.GLAIVE, "glaive"),
            Map.entry(WeaponType.HAMMER, "hammer"),
            Map.entry(WeaponType.CLAYMORE, "claymore"),
            Map.entry(WeaponType.CHAKRAM, "sickle"),
            Map.entry(WeaponType.SAI, "dagger")));

    private final PackOutput output;

    public WeaponCompatProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path data = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(BetterWeaponry.MODID);
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (WeaponType type : WeaponType.values()) {
            String compat = TABLE.get(type);
            if (compat == null) throw new IllegalStateException("No compatibility entry for the weapon type " + type.id());
            WeaponDefinition stats = WeaponData.builtin().weapon(type);
            JsonObject behavior = combatFile(type);

            JsonObject betterCombat = betterCombat(compat, behavior, stats);
            JsonObject epicFight = epicFight(type, behavior, stats);
            for (WeaponMaterial material : WeaponMaterials.all()) {
                writes.add(DataProvider.saveStable(cache, betterCombat, data.resolve("weapon_attributes").resolve(material.itemName(type) + ".json")));
                // Epic Fight signale chaque fichier d'un objet absent : les matériaux des autres mods sont générés au chargement.
                if (!material.isModded()) writes.add(DataProvider.saveStable(cache, epicFight, data.resolve("capabilities/weapons").resolve(material.itemName(type) + ".json")));
            }
        }
        for (WeaponMaterial material : WeaponMaterials.all()) {
            writes.add(DataProvider.saveStable(cache, parent("bow_two_handed_light"), data.resolve("weapon_attributes").resolve(material.itemName(GearType.BOW) + ".json")));
            writes.add(DataProvider.saveStable(cache, parent("crossbow_two_handed_light"), data.resolve("weapon_attributes").resolve(material.itemName(GearType.CROSSBOW) + ".json")));
            if (!material.isModded()) {
                writes.add(DataProvider.saveStable(cache, simpleEpicFight("shield"), data.resolve("capabilities/weapons").resolve(material.itemName(GearType.SHIELD) + ".json")));
                writes.add(DataProvider.saveStable(cache, simpleEpicFight("bow"), data.resolve("capabilities/weapons").resolve(material.itemName(GearType.BOW) + ".json")));
                writes.add(DataProvider.saveStable(cache, simpleEpicFight("crossbow"), data.resolve("capabilities/weapons").resolve(material.itemName(GearType.CROSSBOW) + ".json")));
            }
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Better Weaponry - weapon compatibility files";
    }

    /** Le fichier Combat Ascended de cette arme, livré avec le mod : il dit combien de mains et quelles propriétés. */
    private static JsonObject combatFile(WeaponType type) {
        Path file = Path.of(System.getProperty("betterweaponry.combatFiles"), type.id() + ".json");
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean flag(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.getAsBoolean();
    }

    private static JsonObject parent(String preset) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "bettercombat:" + preset);
        return json;
    }

    /** Better Combat additionne la portée du joueur et "range_bonus" : les armes coupent la leur, celle-ci la remplace. */
    private static JsonObject betterCombat(String compat, JsonObject behavior, WeaponDefinition stats) {
        JsonObject attributes = new JsonObject();
        attributes.addProperty("two_handed", flag(behavior, "two_handed"));
        attributes.addProperty("range_bonus", stats.attackReach() - PLAYER_BASE_REACH);
        JsonObject json = parent(compat);
        json.add("attributes", attributes);
        return json;
    }

    private static JsonObject simpleEpicFight(String type) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "epicfight:" + type);
        return json;
    }

    /** La longueur de la zone de touche suit la portée de l'arme, à régler en jeu. */
    private static JsonObject epicFight(WeaponType type, JsonObject behavior, WeaponDefinition stats) {
        JsonObject json = simpleEpicFight(EpicFightFiles.TYPES.get(type));
        JsonObject common = new JsonObject();
        common.addProperty("armor_negation", behavior.has("armor_penetration") ? behavior.get("armor_penetration").getAsDouble() : 0.0D);
        common.addProperty("impact", 0.5D);
        common.addProperty("max_strikes", 1);
        common.addProperty("damage_bonus", 0.0D);
        common.addProperty("speed_bonus", 0.0D);
        JsonObject attributes = new JsonObject();
        attributes.add("common", common);
        json.add("attributes", attributes);

        double length = 0.7D + Math.max(0.0D, stats.attackReach() - 2.0D) * 0.35D;
        JsonObject collider = new JsonObject();
        collider.addProperty("number", 3);
        collider.add("center", vector(0.0D, 0.0D, -length));
        collider.add("size", vector(0.4D, 0.4D, length));
        json.add("collider", collider);
        return json;
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
