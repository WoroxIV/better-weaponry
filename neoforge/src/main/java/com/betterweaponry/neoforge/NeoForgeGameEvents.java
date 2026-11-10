package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.EnchantmentEvents;
import com.betterweaponry.command.BetterWeaponryCommand;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.data.WeaponDataEvents;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.event.VanillaGearEvents;
import com.betterweaponry.platform.IncomingDamage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Relie les événements du jeu de NeoForge au code commun. */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
final class NeoForgeGameEvents {

    private NeoForgeGameEvents() {
    }

    // --- Combat ---

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        EnchantmentEvents.onIncomingDamage(new Damage(event));
    }

    @SubscribeEvent
    static void onDamageDealt(LivingDamageEvent.Post event) {
        EnchantmentEvents.onDamageDealt(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    // --- Monde ---

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        VanillaGearEvents.onLogin(event.getEntity());
    }

    @SubscribeEvent
    static void onVillagerTrades(VillagerTradesEvent event) {
        VanillaGearEvents.onVillagerTrades(event.getType(), event.getTrades());
    }

    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        VanillaGearEvents.onEntityJoin(event.getEntity(), event.loadedFromDisk());
    }

    // --- Données et commandes ---

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WeaponDataLoader());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        WeaponDataEvents.onDatapackSync(event.getPlayer() == null, event.getRelevantPlayers());
    }

    /** Reprend les réglages 1.x propres à ce monde. Après le démarrage : les datapacks sont déjà chargés avant ServerAboutToStart. */
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        LegacyConfigMigration.migrateWorld(server.getWorldPath(LevelResource.ROOT));
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        BetterWeaponryCommand.register(event.getDispatcher());
    }

    /** Un coup qui arrive : la version NeoForge de l'interface commune. */
    private record Damage(LivingIncomingDamageEvent event) implements IncomingDamage {
        @Override
        public LivingEntity target() {
            return event.getEntity();
        }

        @Override
        public DamageSource source() {
            return event.getSource();
        }

        @Override
        public float amount() {
            return event.getAmount();
        }

        @Override
        public void setAmount(float amount) {
            event.setAmount(amount);
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }

        @Override
        public boolean isCanceled() {
            return event.isCanceled();
        }

        @Override
        public void scaleArmorReduction(float kept) {
            event.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * kept);
        }

        @Override
        public void setPostAttackInvulnerabilityTicks(int ticks) {
            event.getContainer().setPostAttackInvulnerabilityTicks(ticks);
        }
    }
}
