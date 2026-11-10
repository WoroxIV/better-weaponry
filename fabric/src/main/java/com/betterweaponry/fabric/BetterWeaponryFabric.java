package com.betterweaponry.fabric;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.command.BetterWeaponryCommand;
import com.betterweaponry.data.WeaponDataEvents;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.event.VanillaGearEvents;
import com.betterweaponry.item.VanillaGear;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModItems;
import com.betterweaponry.registry.ModStats;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/** Point d'entrée Fabric (chargé côté client et serveur). */
public final class BetterWeaponryFabric implements ModInitializer {
    private static final int RANGED_WEAPON_BURN_TICKS = 300;

    @Override
    public void onInitialize() {
        Platform.set(new FabricPlatform());
        FabricAttributes.init();
        BetterWeaponry.init();
        ModStats.registerFormats();
        // Côté client, on attend que le jeu soit prêt pour connaître sa langue (BetterWeaponryFabricClient).
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) BetterWeaponry.prepareConfigFolder();

        registerWorld();
        registerData();
    }

    private static void registerWorld() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> VanillaGearEvents.onLogin(handler.player));
        // Les nouvelles entités passent par ServerLevelMixin, qui sait qu'elles ne viennent pas du disque.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!FabricEntityJoin.isFreshEntityBeingAdded()) VanillaGearEvents.onEntityJoin(entity, true);
        });

        replaceTrades(VillagerProfession.ARMORER);
        replaceTrades(VillagerProfession.FLETCHER);

        // Comme le bouclier, l'arc et l'arbalète vanilla sont remplacés, ils quittent l'onglet Combat.
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.getDisplayStacks().removeIf(VanillaGear::isReplaced);
            entries.getSearchTabStacks().removeIf(VanillaGear::isReplaced);
        });

        FuelRegistry.INSTANCE.add(ModItems.bow(WeaponMaterials.WOODEN).get(), RANGED_WEAPON_BURN_TICKS);
        FuelRegistry.INSTANCE.add(ModItems.crossbow(WeaponMaterials.WOODEN).get(), RANGED_WEAPON_BURN_TICKS);
    }

    /** Fabric ne permet que d'ajouter des échanges : pour remplacer ceux du jeu, on modifie directement sa table. */
    private static void replaceTrades(VillagerProfession profession) {
        Int2ObjectMap<VillagerTrades.ItemListing[]> table = VillagerTrades.TRADES.get(profession);
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<VillagerTrades.ItemListing[]> level : table.int2ObjectEntrySet()) {
            trades.put(level.getIntKey(), new ArrayList<>(Arrays.asList(level.getValue())));
        }
        VanillaGearEvents.onVillagerTrades(profession, trades);
        for (Int2ObjectMap.Entry<List<VillagerTrades.ItemListing>> level : trades.int2ObjectEntrySet()) {
            table.put(level.getIntKey(), level.getValue().toArray(VillagerTrades.ItemListing[]::new));
        }
    }

    private static void registerData() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new WeaponDataReloadListener());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> WeaponDataEvents.onDatapackSync(!joined, Stream.of(player)));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> BetterWeaponryCommand.register(dispatcher));
    }

    /** Le chargeur de données, avec le nom que Fabric demande. */
    private static final class WeaponDataReloadListener implements IdentifiableResourceReloadListener {
        private static final ResourceLocation ID = new ResourceLocation(BetterWeaponry.MODID, "weapon_data");
        private final WeaponDataLoader loader = new WeaponDataLoader();

        @Override
        public ResourceLocation getFabricId() {
            return ID;
        }

        @Override
        public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager, ProfilerFiller preparationProfiler,
                                              ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
            return loader.reload(barrier, manager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }
    }
}
