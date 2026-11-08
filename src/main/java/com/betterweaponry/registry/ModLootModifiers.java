package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.loot.AddEnchantedBookModifier;
import com.betterweaponry.loot.ReplaceVanillaGearModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Modifications des butins du jeu. Les fichiers sont dans data/better_weaponry/loot_modifiers/. */
public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BetterWeaponry.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ReplaceVanillaGearModifier>> REPLACE_VANILLA_GEAR =
            SERIALIZERS.register("replace_vanilla_gear", () -> ReplaceVanillaGearModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddEnchantedBookModifier>> ADD_ENCHANTED_BOOK =
            SERIALIZERS.register("add_enchanted_book", () -> AddEnchantedBookModifier.CODEC);

    private ModLootModifiers() {
    }
}
