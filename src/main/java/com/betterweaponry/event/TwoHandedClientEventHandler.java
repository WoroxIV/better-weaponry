package com.betterweaponry.event;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.TwoHandedSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Vue première personne : ne dessine pas la main secondaire quand on tient une arme à deux mains.
 * (La vue troisième personne est gérée par ItemInHandLayerMixin et PlayerRendererMixin.)
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class TwoHandedClientEventHandler {

    private TwoHandedClientEventHandler() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.OFF_HAND) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && TwoHandedSystem.isOffhandLocked(player)) {
            event.setCanceled(true);
        }
    }
}
