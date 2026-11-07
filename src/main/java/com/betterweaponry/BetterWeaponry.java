package com.betterweaponry;

import com.betterweaponry.config.ServerConfig;
import com.betterweaponry.datagen.ModItemModelProvider;
import com.betterweaponry.datagen.ModItemTagsProvider;
import com.betterweaponry.datagen.ModRecipeProvider;
import com.betterweaponry.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

/** Point d'entrée du mod (chargé côté client et serveur). */
@Mod(BetterWeaponry.MODID)
public final class BetterWeaponry {
    public static final String MODID = "better_weaponry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BetterWeaponry(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::addToCreativeTab);
        modEventBus.addListener(this::gatherData);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "better_weaponry-server.toml");
    }

    /** Place toutes les armes dans l'onglet Combat, juste avant le trident, dans l'ordre d'enregistrement. */
    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) return;

        ItemStack trident = new ItemStack(Items.TRIDENT);
        for (var item : ModItems.ITEMS.getEntries()) {
            event.insertBefore(trident, new ItemStack(item.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    /** Lancé par "gradlew runData" : génère modèles, tags et recettes dans src/generated/resources. */
    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, event.getExistingFileHelper()));

        generator.addProvider(event.includeServer(),
                new ModRecipeProvider(output, event.getLookupProvider()));

        // Le mod n'a pas de blocs : on donne au provider d'items une liste de tags de blocs vide
        // au lieu de générer un BlockTagsProvider vide.
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, event.getLookupProvider(),
                        CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                        event.getExistingFileHelper()));
    }
}
