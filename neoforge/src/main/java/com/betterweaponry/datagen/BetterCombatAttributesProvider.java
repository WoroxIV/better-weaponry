package com.betterweaponry.datagen;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Génère les fichiers {@code data/better_weaponry/weapon_attributes/<arme>.json} lus par Better Combat
 * (animations et attaques de ce mod quand il est installé).
 * <p>
 * Chaque arme reprend un "preset" de Better Combat ({@code "parent"}), et impose le nombre de mains des valeurs
 * par défaut de Better Weaponry (ex : le preset "spear" est à deux mains, notre lance à une main).
 * Better Combat ignore les fichiers des armes qui n'existent pas (matériaux d'un mod absent).
 */
public final class BetterCombatAttributesProvider implements DataProvider {
    /** Presets de Better Combat 1.21.1 (data/bettercombat/weapon_attributes/). */
    private static final Map<WeaponType, String> PRESETS = new EnumMap<>(Map.ofEntries(
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

    public BetterCombatAttributesProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (WeaponType type : WeaponType.values()) {
            String preset = PRESETS.get(type);
            if (preset == null) throw new IllegalStateException("No Better Combat preset for the weapon type " + type.id());

            JsonObject attributes = new JsonObject();
            attributes.addProperty("two_handed", WeaponData.builtin().weapon(type).twoHanded());
            JsonObject json = new JsonObject();
            json.addProperty("parent", "bettercombat:" + preset);
            json.add("attributes", attributes);

            for (WeaponMaterial material : WeaponMaterials.all()) {
                writes.add(DataProvider.saveStable(cache, json, output.getOutputFolder(PackOutput.Target.DATA_PACK)
                        .resolve(BetterWeaponry.MODID).resolve("weapon_attributes").resolve(material.itemName(type) + ".json")));
            }
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Better Weaponry - Better Combat weapon attributes";
    }
}
