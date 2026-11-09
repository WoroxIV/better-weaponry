package com.betterweaponry.item;

import com.betterweaponry.data.WeaponData;
import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.platform.Platform;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ajoute parfois à un coffre un livre portant un des enchantements du mod. */
public final class LootBooks {
    private static final double PERCENT = 100.0D;
    private static final List<String> FILES = List.of("nether_enchanted_books.json", "end_enchanted_books.json");
    private static Map<ResourceLocation, List<ResourceKey<Enchantment>>> byLootTable;

    private LootBooks() {
    }

    /** Les coffres concernés et leurs enchantements sont dans data/better_weaponry/loot_modifiers/. */
    public static void maybeAdd(ResourceLocation lootTable, List<ItemStack> loot, LootContext context) {
        if (byLootTable == null) byLootTable = read();
        List<ResourceKey<Enchantment>> enchantments = byLootTable.get(lootTable);
        if (enchantments != null) maybeAdd(loot, context, enchantments);
    }

    private static Map<ResourceLocation, List<ResourceKey<Enchantment>>> read() {
        Map<ResourceLocation, List<ResourceKey<Enchantment>>> result = new HashMap<>();
        for (String file : FILES) {
            Platform.get().readModFile("data", BetterWeaponry.MODID, "loot_modifiers", file).ifPresent(text -> {
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                List<ResourceKey<Enchantment>> enchantments = new ArrayList<>();
                for (JsonElement id : json.getAsJsonArray("enchantments")) {
                    enchantments.add(ResourceKey.create(Registries.ENCHANTMENT, new ResourceLocation(id.getAsString())));
                }
                for (JsonElement condition : json.getAsJsonArray("conditions")) {
                    for (JsonElement term : condition.getAsJsonObject().getAsJsonArray("terms")) {
                        result.put(new ResourceLocation(term.getAsJsonObject().get("loot_table_id").getAsString()), List.copyOf(enchantments));
                    }
                }
            });
        }
        return result;
    }

    public static void maybeAdd(List<ItemStack> loot, LootContext context, List<ResourceKey<Enchantment>> enchantments) {
        RandomSource random = context.getRandom();
        if (enchantments.isEmpty() || random.nextDouble() * PERCENT >= WeaponData.gear().enchantedBookChance()) return;

        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(enchantments.get(random.nextInt(enchantments.size())));
        if (enchantment == null) return;
        loot.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, Mth.nextInt(random, 1, enchantment.getMaxLevel()))));
    }
}
