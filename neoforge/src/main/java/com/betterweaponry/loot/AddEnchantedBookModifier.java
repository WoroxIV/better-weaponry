package com.betterweaponry.loot;

import com.betterweaponry.item.LootBooks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

/** Ajoute parfois à un coffre un livre portant un de ces enchantements (les coffres visés sont dans les conditions du JSON). */
public final class AddEnchantedBookModifier extends LootModifier {
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
        LootBooks.maybeAdd(generatedLoot, context, enchantments);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
