package com.betterweaponry.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;
import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ModEnchantments;
import com.betterweaponry.registry.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Pages d'information de JEI et d'EMI : ce que font nos enchantements et les capacités de chaque objet.
 * Cette classe ne dépend d'aucun des deux mods : leurs plugins ne font qu'afficher ce qu'elle renvoie.
 */
public final class RecipeViewerInfo {
    private static final String KEY = BetterWeaponry.MODID + ".info.";

    private RecipeViewerInfo() {
    }

    /** Une page : les objets concernés et ses lignes. */
    public record Entry(String id, List<ItemStack> stacks, List<Component> lines) {
    }

    public static List<Entry> entries(RegistryAccess registries) {
        List<Entry> entries = new ArrayList<>();
        for (ResourceKey<Enchantment> key : ModEnchantments.ALL) {
            BuiltInRegistries.ENCHANTMENT.getOptional(key).ifPresent(enchantment -> entries.add(enchantmentEntry(key, enchantment)));
        }
        entries.add(new Entry("bow", stacks(ModItems.allBows()), List.of(Component.translatable(KEY + "ranged"))));
        entries.add(new Entry("crossbow", stacks(ModItems.allCrossbows()), List.of(Component.translatable(KEY + "ranged"))));
        entries.add(new Entry("shuriken", stacks(ModItems.allShurikens()), List.of(Component.translatable(KEY + "shuriken"))));
        return entries;
    }

    /** Un livre par niveau : JEI et EMI les listent séparément. */
    private static Entry enchantmentEntry(ResourceKey<Enchantment> key, Enchantment enchantment) {
        List<ItemStack> books = new ArrayList<>();
        for (int level = enchantment.getMinLevel(); level <= enchantment.getMaxLevel(); level++) {
            books.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
        }
        return new Entry(key.location().getPath(), books, List.of(Component.translatable("enchantment." + BetterWeaponry.MODID + "." + key.location().getPath() + ".desc")));
    }

    private static List<ItemStack> stacks(Collection<? extends Supplier<? extends Item>> items) {
        return items.stream().map(item -> new ItemStack(item.get())).toList();
    }
}
