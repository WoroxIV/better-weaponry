package com.betterweaponry.forge;

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
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Relie les événements client de Forge au code commun. */
public final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    /** Première personne : la main secondaire est cachée avec une arme à deux mains, et les parades sont dessinées à part. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
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
    public static void onComputeFov(ComputeFovModifierEvent event) {
        event.setNewFovModifier(RangedWeaponClient.modifyFov(event.getPlayer(), event.getNewFovModifier()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) TutorialHintTracker.onClientTick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        AttackZoneRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        TooltipEventHandler.onTooltip(event.getItemStack(), event.getToolTip(), event.getEntity());
    }

    /** En quittant un serveur, on oublie ses stats : on revient aux valeurs par défaut du mod. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WeaponData.resetToBuiltin();
    }

    /** Une connexion qui échoue pendant le chargement ne déclenche pas LoggingOut, alors que les stats du serveur sont déjà reçues. */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen && Minecraft.getInstance().level == null) WeaponData.resetToBuiltin();
    }
}
