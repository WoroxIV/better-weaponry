package com.betterweaponry.item;

import com.betterweaponry.data.WeaponData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.List;
import java.util.Optional;

/** Ajoute parfois à un coffre un livre portant un des enchantements du mod. */
public final class LootBooks {
    private static final double PERCENT = 100.0D;

    private LootBooks() {
    }

    public static void maybeAdd(List<ItemStack> loot, LootContext context, List<ResourceKey<Enchantment>> enchantments) {
        RandomSource random = context.getRandom();
        if (enchantments.isEmpty() || random.nextDouble() * PERCENT >= WeaponData.gear().enchantedBookChance()) return;

        ResourceKey<Enchantment> key = enchantments.get(random.nextInt(enchantments.size()));
        Optional<? extends Holder<Enchantment>> enchantment = context.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key);
        enchantment.ifPresent(holder -> loot.add(EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(holder, Mth.nextInt(random, 1, holder.value().getMaxLevel())))));
    }
}
