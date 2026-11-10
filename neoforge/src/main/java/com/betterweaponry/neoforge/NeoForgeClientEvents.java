package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.RangedWeaponClient;
import com.betterweaponry.data.WeaponData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Relie les événements client de NeoForge au code commun. */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
final class NeoForgeClientEvents {

    private NeoForgeClientEvents() {
    }

    @SubscribeEvent
    static void onComputeFov(ComputeFovModifierEvent event) {
        event.setNewFovModifier(RangedWeaponClient.modifyFov(event.getPlayer(), event.getNewFovModifier()));
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
