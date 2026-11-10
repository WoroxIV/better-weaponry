package com.betterweaponry.forge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.compat.EpicFightFiles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModStats;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

/** Point d'entrée Forge (chargé côté client et serveur). */
@Mod(BetterWeaponry.MODID)
public final class BetterWeaponryForge {

    public BetterWeaponryForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Platform.set(new ForgePlatform(modEventBus));
        BetterWeaponry.init();
        ForgeNetwork.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addToCreativeTab);
        EpicFightFiles.generate();
        modEventBus.addListener(this::addPacks);
        MinecraftForge.EVENT_BUS.register(ForgeGameEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) BetterWeaponryForgeClient.init(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModStats::registerFormats);
        BetterWeaponry.prepareConfigFolder();
    }

    /** Le bouclier, l'arc et l'arbalète vanilla sont remplacés par ceux du mod, qui ont leur propre onglet. */
    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) return;
        for (Item replaced : List.of(Items.SHIELD, Items.BOW, Items.CROSSBOW)) {
            event.getEntries().remove(new ItemStack(replaced));
        }
    }

    /** Fichiers Epic Fight des matériaux des autres mods : générés au démarrage, seulement pour ceux qui sont chargés. */
    private void addPacks(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !BwCompat.epicFight()) return;
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate("better_weaponry_epic_fight", Component.literal("Better Weaponry: Epic Fight"), true,
                    id -> new PathPackResources(id, EpicFightFiles.packFolder(), true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }
}
