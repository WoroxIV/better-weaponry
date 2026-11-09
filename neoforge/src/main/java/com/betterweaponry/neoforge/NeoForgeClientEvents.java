package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.AttackZoneRenderer;
import com.betterweaponry.client.DualParryHandRenderer;
import com.betterweaponry.client.RangedWeaponClient;
import com.betterweaponry.client.tutorial.TutorialHintTracker;
import com.betterweaponry.combat.TwoHandedSystem;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.event.TooltipEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Relie les événements client de NeoForge au code commun. */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
final class NeoForgeClientEvents {

    private NeoForgeClientEvents() {
    }

    /** Première personne : la main secondaire est cachée avec une arme à deux mains, et les parades sont dessinées à part. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.getHand() == InteractionHand.OFF_HAND && player != null && TwoHandedSystem.isOffhandLocked(player)) {
            event.setCanceled(true);
            return;
        }
        if (DualParryHandRenderer.render(event.getHand(), event.getItemStack(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getEquipProgress(), false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onComputeFov(ComputeFovModifierEvent event) {
        event.setNewFovModifier(RangedWeaponClient.modifyFov(event.getPlayer(), event.getNewFovModifier()));
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        TutorialHintTracker.onClientTick();
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        AttackZoneRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        TooltipEventHandler.onTooltip(event.getItemStack(), event.getToolTip(), event.getEntity());
    }

    /** En quittant un serveur, on oublie ses stats : on revient aux valeurs par défaut du mod. */
    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WeaponData.resetToBuiltin();
    }

    /** Une connexion qui échoue pendant le chargement ne déclenche pas LoggingOut, alors que les stats du serveur sont déjà reçues. */
    @SubscribeEvent
    static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen && Minecraft.getInstance().level == null) WeaponData.resetToBuiltin();
    }
}
