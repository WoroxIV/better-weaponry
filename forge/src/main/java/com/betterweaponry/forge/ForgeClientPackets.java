package com.betterweaponry.forge;

import com.betterweaponry.network.ComboSyncPayload;
import com.betterweaponry.network.DualSwingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Paquets qui ont besoin du joueur local : classe réservée au client. */
final class ForgeClientPackets {
    private ForgeClientPackets() {
    }

    static void combo(ComboSyncPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) ComboSyncPayload.handle(payload, player);
    }

    static void dualSwing(DualSwingPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) DualSwingPayload.handle(payload, player);
    }
}
