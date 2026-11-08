package com.betterweaponry.item;

import com.betterweaponry.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/** Bouclier + bannière = bouclier décoré. La recette vanilla n'accepte que le bouclier vanilla. */
public class ModShieldDecorationRecipe extends CustomRecipe {

    public ModShieldDecorationRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack shield = ItemStack.EMPTY;
        ItemStack banner = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BannerItem) {
                if (!banner.isEmpty()) return false;
                banner = stack;
            } else {
                boolean undecorated = stack.getItem() instanceof WeaponShieldItem
                        && stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().isEmpty();
                if (!undecorated || !shield.isEmpty()) return false;
                shield = stack;
            }
        }
        return !shield.isEmpty() && !banner.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack shield = ItemStack.EMPTY;
        ItemStack banner = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof BannerItem) banner = stack;
            else if (stack.getItem() instanceof WeaponShieldItem) shield = stack.copy();
        }
        if (shield.isEmpty() || banner.isEmpty()) return ItemStack.EMPTY;
        shield.set(DataComponents.BANNER_PATTERNS, banner.get(DataComponents.BANNER_PATTERNS));
        shield.set(DataComponents.BASE_COLOR, ((BannerItem) banner.getItem()).getColor());
        return shield;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHIELD_DECORATION.get();
    }
}
