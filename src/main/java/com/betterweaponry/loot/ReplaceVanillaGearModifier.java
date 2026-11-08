package com.betterweaponry.loot;

import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.VanillaGear;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Dans tous les butins du jeu (coffres, pêche...), l'équipement vanilla devient une version du mod. */
public final class ReplaceVanillaGearModifier extends LootModifier {
    public static final MapCodec<ReplaceVanillaGearModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, ReplaceVanillaGearModifier::new));

    public ReplaceVanillaGearModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        VanillaGear.replaceInLoot(generatedLoot, context.getRandom(), WeaponData.gear());
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
