package com.betterweaponry.loot;

import com.betterweaponry.data.WeaponData;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;
import java.util.Optional;

/** Ajoute parfois à un coffre un livre portant un de ces enchantements (les coffres visés sont dans les conditions du JSON). */
public final class AddEnchantedBookModifier extends LootModifier {
    private static final double PERCENT = 100.0D;

    public static final MapCodec<AddEnchantedBookModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(ResourceKey.codec(Registries.ENCHANTMENT).listOf().fieldOf("enchantments").forGetter(modifier -> modifier.enchantments))
            .apply(instance, AddEnchantedBookModifier::new));

    private final List<ResourceKey<Enchantment>> enchantments;

    public AddEnchantedBookModifier(LootItemCondition[] conditions, List<ResourceKey<Enchantment>> enchantments) {
        super(conditions);
        this.enchantments = List.copyOf(enchantments);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        RandomSource random = context.getRandom();
        if (enchantments.isEmpty() || random.nextDouble() * PERCENT >= WeaponData.gear().enchantedBookChance()) return generatedLoot;

        ResourceKey<Enchantment> key = enchantments.get(random.nextInt(enchantments.size()));
        Optional<? extends Holder<Enchantment>> enchantment = context.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key);
        enchantment.ifPresent(holder -> generatedLoot.add(EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(holder, Mth.nextInt(random, 1, holder.value().getMaxLevel())))));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
