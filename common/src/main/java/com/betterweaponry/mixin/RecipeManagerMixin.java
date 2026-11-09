package com.betterweaponry.mixin;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * L'arc, l'arbalète et le bouclier vanilla ne se fabriquent plus : ce sont les versions du mod. Retirés ici plutôt que remplacés
 * par un fichier du mod, car Forge fournit ses propres versions de ces recettes et passerait devant.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Unique private static final List<ResourceLocation> VANILLA_GEAR = List.of(new ResourceLocation("bow"), new ResourceLocation("crossbow"), new ResourceLocation("shield"));

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void betterweaponry$disableVanillaGear(Map<ResourceLocation, JsonElement> recipes, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        VANILLA_GEAR.forEach(recipes::remove);
    }
}
