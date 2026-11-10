package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.datagen.WeaponCompatProvider;
import com.betterweaponry.datagen.ModItemModelProvider;
import com.betterweaponry.datagen.ModItemTagsProvider;
import com.betterweaponry.datagen.ModRecipeProvider;
import com.betterweaponry.datagen.lang.ModLanguageProvider;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModLootModifiers;
import com.betterweaponry.registry.ModStats;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Point d'entrée NeoForge (chargé côté client et serveur). */
@Mod(BetterWeaponry.MODID)
public final class BetterWeaponryNeoForge {

    public BetterWeaponryNeoForge(IEventBus modEventBus) {
        Platform.set(new NeoForgePlatform(modEventBus));
        BetterWeaponry.init();
        ModLootModifiers.SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::addToCreativeTab);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModStats::registerFormats);
        BetterWeaponry.prepareConfigFolder();
    }

    /** Le bouclier, l'arc et l'arbalète vanilla sont remplacés par ceux du mod, qui ont leur propre onglet. */
    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) return;

        for (Item replaced : List.of(Items.SHIELD, Items.BOW, Items.CROSSBOW)) {
            event.remove(new ItemStack(replaced), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    /** Lancé par "gradlew :neoforge:runData" : génère modèles, traductions, tags, recettes et fichiers Better Combat. */
    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, event.getExistingFileHelper()));

        // Fichiers de langue, construits depuis common/src/main/lang/ (voir ModLanguageProvider)
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output));

        generator.addProvider(event.includeServer(),
                new ModRecipeProvider(output, event.getLookupProvider()));

        // Fichiers lus par Better Combat et Epic Fight (mods optionnels)
        generator.addProvider(event.includeServer(), new WeaponCompatProvider(output));

        // Le mod n'a pas de blocs : on donne au provider d'items une liste de tags de blocs vide
        // au lieu de générer un BlockTagsProvider vide.
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, event.getLookupProvider(),
                        CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                        event.getExistingFileHelper()));
    }
}
