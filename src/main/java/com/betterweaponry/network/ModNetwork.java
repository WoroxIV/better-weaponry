package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Enregistre les paquets réseau du mod et les étapes de connexion (événements du bus du mod, routés automatiquement). */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class ModNetwork {
    /** À augmenter quand le format d'un paquet change : un client et un serveur différents refuseront de se connecter. */
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                // "common" : utilisable pendant la connexion (configuration) et en jeu (après /reload)
                .commonToClient(WeaponDataSyncPayload.TYPE, WeaponDataSyncPayload.STREAM_CODEC, WeaponDataSyncPayload::handle);
    }

    @SubscribeEvent
    public static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SyncWeaponDataTask(event.getListener()));
    }
}
