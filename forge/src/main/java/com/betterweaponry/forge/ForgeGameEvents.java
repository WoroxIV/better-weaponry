package com.betterweaponry.forge;

import com.betterweaponry.combat.AdoptedItemEvents;
import com.betterweaponry.combat.CombatEvents;
import com.betterweaponry.combat.EnchantmentEvents;
import com.betterweaponry.combat.ShieldSystem;
import com.betterweaponry.combat.TwoHandedSystem;
import com.betterweaponry.command.BetterWeaponryCommand;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.data.WeaponDataEvents;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.editor.EditorService;
import com.betterweaponry.event.VanillaGearEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.stream.Stream;

/** Relie les événements du jeu de Forge au code commun. Les dégâts sont dans LivingEntityDamageMixin : Forge n'a pas d'événement équivalent. */
public final class ForgeGameEvents {
    private ForgeGameEvents() {
    }

    // --- Combat ---

    /** En dernier : le montant final, une fois tous les autres mods passés. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageDealt(LivingDamageEvent event) {
        EnchantmentEvents.onDamageDealt(event.getEntity(), event.getSource(), event.getAmount());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CombatEvents.onChangedDimension(player);
    }

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        ShieldSystem.onStartUsingItem(event.getEntity(), event.getItem());
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (ShieldSystem.onProjectileImpact(event.getProjectile(), event.getRayTraceResult())) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        }
    }

    // --- Interactions ---

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (cancelIfLocked(event)) return;
        if (AdoptedItemEvents.onRightClickItem(event.getEntity(), event.getHand(), event.getItemStack(), event.getLevel())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfLocked(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfLocked(event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelIfLocked(event);
    }

    private static boolean cancelIfLocked(PlayerInteractEvent event) {
        if (!TwoHandedSystem.isLockedHand(event.getEntity(), event.getHand())) return false;
        event.setCanceled(true);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) TwoHandedSystem.stopLockedOffhandUse(event.player);
    }

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() == EquipmentSlot.MAINHAND) AdoptedItemEvents.collectModifiers(event.getItemStack(), event::addModifier);
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
        if (LegacyConfigMigration.migrateWorld(server.getWorldPath(LevelResource.ROOT))) EditorService.enableEditorPack(server);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BetterWeaponryCommand.register(event.getDispatcher());
    }
}
