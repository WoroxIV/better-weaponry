package com.betterweaponry.forge;

import com.betterweaponry.client.RangedWeaponClient;
import com.betterweaponry.client.ReachTooltip;
import com.betterweaponry.data.WeaponData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Relie les événements client de Forge au code commun. */
public final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        event.setNewFovModifier(RangedWeaponClient.modifyFov(event.getPlayer(), event.getNewFovModifier()));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ReachTooltip.onTooltip(event.getItemStack(), event.getToolTip());
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
