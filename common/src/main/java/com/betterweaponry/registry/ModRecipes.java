package com.betterweaponry.registry;

import com.betterweaponry.platform.Registered;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.item.ModShieldDecorationRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

/** Types de recettes propres au mod. */
public final class ModRecipes {
    public static final Registered<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ModShieldDecorationRecipe>> SHIELD_DECORATION =
            Platform.get().register(Registries.RECIPE_SERIALIZER, "shield_decoration", () -> new SimpleCraftingRecipeSerializer<>(ModShieldDecorationRecipe::new));

    private ModRecipes() {
    }

    /** Force la déclaration des recettes. */
    public static void init() {
    }
}
