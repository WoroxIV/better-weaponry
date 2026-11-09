package com.betterweaponry.compat.emi;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.RecipeViewerInfo;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Lu seulement si EMI est installé. */
@EmiEntrypoint
public final class BetterWeaponryEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        if (Minecraft.getInstance().level == null) return;
        for (RecipeViewerInfo.Entry entry : RecipeViewerInfo.entries(Minecraft.getInstance().level.registryAccess())) {
            List<EmiIngredient> stacks = entry.stacks().stream().<EmiIngredient>map(EmiStack::of).toList();
            registry.addRecipe(new EmiInfoRecipe(stacks, entry.lines(), new ResourceLocation(BetterWeaponry.MODID, "/info/" + entry.id())));
        }
    }
}
