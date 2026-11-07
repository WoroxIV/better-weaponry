package com.betterweaponry.event;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.TwoHandedSystem;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Empêche toute utilisation de la main secondaire pendant qu'on tient une arme à deux mains.
 * Ces événements sont déclenchés côté client et serveur, donc le blocage est cohérent des deux côtés.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class TwoHandedEventHandler {

    private TwoHandedEventHandler() {
    }

    /** Utiliser un objet : lever un bouclier, manger, tirer à l'arc... */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isLockedOffhand(event)) event.setCanceled(true);
    }

    /** Interagir avec un bloc : poser une torche, ouvrir un coffre... */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isLockedOffhand(event)) event.setCanceled(true);
    }

    /** Interagir avec une entité : mettre une selle, nourrir un animal... */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isLockedOffhand(event)) event.setCanceled(true);
    }

    /** Interaction précise avec une entité (ex : poser un objet sur un porte-armure), qui n'était pas couverte. */
    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isLockedOffhand(event)) event.setCanceled(true);
    }

    private static boolean isLockedOffhand(PlayerInteractEvent event) {
        return event.getHand() == InteractionHand.OFF_HAND && TwoHandedSystem.isOffhandLocked(event.getEntity());
    }
}
