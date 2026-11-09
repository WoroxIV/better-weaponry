package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.AdoptedItemEvents;
import com.betterweaponry.combat.CombatEvents;
import com.betterweaponry.combat.EnchantmentEvents;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.ShieldSystem;
import com.betterweaponry.combat.TwoHandedSystem;
import com.betterweaponry.command.BetterWeaponryCommand;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.data.WeaponDataEvents;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.editor.EditorService;
import com.betterweaponry.event.VanillaGearEvents;
import com.betterweaponry.platform.IncomingDamage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Relie les événements du jeu de NeoForge au code commun. */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
final class NeoForgeGameEvents {

    private NeoForgeGameEvents() {
    }

    // --- Combat ---

    /** Avant les autres calculs : une parade adverse réduit ainsi le coup déjà augmenté. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onHitWithPerfectBonus(LivingIncomingDamageEvent event) {
        ParrySystem.onHitWithPerfectBonus(new Damage(event));
    }

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        CombatEvents.onIncomingDamage(new Damage(event));
    }

    @SubscribeEvent
    static void onDamageBeforeHealth(LivingDamageEvent.Pre event) {
        event.setNewDamage(CombatEvents.capPvpHit(event.getEntity(), event.getSource(), event.getNewDamage()));
    }

    @SubscribeEvent
    static void onDamageDealt(LivingDamageEvent.Post event) {
        EnchantmentEvents.onDamageDealt(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    @SubscribeEvent
    static void onSweepAttack(SweepAttackEvent event) {
        if (CombatEvents.preventsSweep(event.getEntity())) event.setSweeping(false);
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CombatEvents.onChangedDimension(player);
    }

    @SubscribeEvent
    static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        ShieldSystem.onStartUsingItem(event.getEntity(), event.getItem());
    }

    @SubscribeEvent
    static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() && ShieldSystem.onShieldBlock(event.getEntity(), event.getDamageSource(), event.getOriginalBlockedDamage())) {
            event.setShieldDamage(0.0F);
        }
    }

    @SubscribeEvent
    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (ShieldSystem.onProjectileImpact(event.getProjectile(), event.getRayTraceResult())) event.setCanceled(true);
    }

    // --- Interactions ---

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (cancelIfLocked(event)) return;
        if (AdoptedItemEvents.onRightClickItem(event.getEntity(), event.getHand(), event.getItemStack(), event.getLevel())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfLocked(event);
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfLocked(event);
    }

    @SubscribeEvent
    static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelIfLocked(event);
    }

    private static <E extends PlayerInteractEvent & net.neoforged.bus.api.ICancellableEvent> boolean cancelIfLocked(E event) {
        if (!TwoHandedSystem.isLockedHand(event.getEntity(), event.getHand())) return false;
        event.setCanceled(true);
        return true;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        TwoHandedSystem.stopLockedOffhandUse(event.getEntity());
    }

    @SubscribeEvent
    static void onItemAttributes(ItemAttributeModifierEvent event) {
        AdoptedItemEvents.collectModifiers(event.getItemStack(), (attribute, modifier) -> event.addModifier(attribute, modifier, EquipmentSlotGroup.MAINHAND));
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
        if (LegacyConfigMigration.migrateWorld(server.getWorldPath(LevelResource.ROOT))) EditorService.enableEditorPack(server);
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
