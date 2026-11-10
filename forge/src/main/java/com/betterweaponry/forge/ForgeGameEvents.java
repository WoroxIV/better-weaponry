package com.betterweaponry.forge;

import com.betterweaponry.combat.EnchantmentEvents;
import com.betterweaponry.command.BetterWeaponryCommand;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.data.WeaponDataEvents;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.event.VanillaGearEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.stream.Stream;

/** Relie les événements du jeu de Forge au code commun. L'Exécution est dans LivingEntityDamageMixin : Forge n'a pas d'événement équivalent. */
public final class ForgeGameEvents {
    private ForgeGameEvents() {
    }

    /** En dernier : le montant final, une fois tous les autres mods passés. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageDealt(LivingDamageEvent event) {
        EnchantmentEvents.onDamageDealt(event.getEntity(), event.getSource(), event.getAmount());
    }

    // --- Monde ---

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        VanillaGearEvents.onLogin(event.getEntity());
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        VanillaGearEvents.onVillagerTrades(event.getType(), event.getTrades());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        VanillaGearEvents.onEntityJoin(event.getEntity(), event.loadedFromDisk());
    }

    // --- Données et commandes ---

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WeaponDataLoader());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        WeaponDataEvents.onDatapackSync(player == null, player != null ? Stream.of(player) : event.getPlayerList().getPlayers().stream());
    }

    /** Reprend les réglages 1.x propres à ce monde. Après le démarrage : les datapacks sont déjà chargés avant ServerAboutToStart. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        LegacyConfigMigration.migrateWorld(server.getWorldPath(LevelResource.ROOT));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BetterWeaponryCommand.register(event.getDispatcher());
    }
}
