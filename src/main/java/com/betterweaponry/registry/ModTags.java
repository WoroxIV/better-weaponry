package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Tags du mod, modifiables par datapack comme n'importe quel tag. */
public final class ModTags {
    /**
     * Objets qui comptent comme des armes pour le dual wield (une dans chaque main = coups alternés).
     * Par défaut : épées, haches, trident et le tag commun des armes de mêlée ({@code c:tools/melee_weapon}).
     */
    public static final TagKey<Item> DUAL_WIELDABLE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "dual_wieldable"));

    private ModTags() {
    }
}
