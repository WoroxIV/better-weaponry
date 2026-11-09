package com.betterweaponry.item;

import com.betterweaponry.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Bouclier + bannière = bouclier décoré. La recette vanilla n'accepte que le bouclier vanilla. */
public class ModShieldDecorationRecipe extends CustomRecipe {

    public ModShieldDecorationRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        ItemStack shield = ItemStack.EMPTY;
        ItemStack banner = ItemStack.EMPTY;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BannerItem) {
                if (!banner.isEmpty()) return false;
                banner = stack;
            } else {
                boolean undecorated = stack.getItem() instanceof WeaponShieldItem && BlockItem.getBlockEntityData(stack) == null;
                if (!undecorated || !shield.isEmpty()) return false;
                shield = stack;
            }
        }
        return !shield.isEmpty() && !banner.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        ItemStack shield = ItemStack.EMPTY;
        ItemStack banner = ItemStack.EMPTY;
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof BannerItem) banner = stack;
            else if (stack.getItem() instanceof WeaponShieldItem) shield = stack.copy();
        }
        if (shield.isEmpty() || banner.isEmpty()) return ItemStack.EMPTY;
        CompoundTag bannerData = BlockItem.getBlockEntityData(banner);
        CompoundTag decoration = bannerData == null ? new CompoundTag() : bannerData.copy();
        decoration.putInt("Base", ((BannerItem) banner.getItem()).getColor().getId());
        BlockItem.setBlockEntityData(shield, BlockEntityType.BANNER, decoration);
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
