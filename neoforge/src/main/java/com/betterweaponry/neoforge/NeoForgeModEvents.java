package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.network.SyncWeaponDataTask;
import com.betterweaponry.network.WeaponDataSyncPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.function.Consumer;

/** Événements du bus du mod (au démarrage) : attributs, paquets réseau. */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
final class NeoForgeModEvents {
    /** À augmenter quand le format d'un paquet change : un client et un serveur différents refuseront de se connecter. */
    private static final String PROTOCOL_VERSION = "5";

    private NeoForgeModEvents() {
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                // "common" : utilisable pendant la connexion (configuration) et en jeu (après /reload)
                .commonToClient(WeaponDataSyncPayload.TYPE, WeaponDataSyncPayload.STREAM_CODEC, onMainThread(WeaponDataSyncPayload::handle));
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> onMainThread(Consumer<T> handler) {
        return (payload, context) -> context.enqueueWork(() -> handler.accept(payload));
    }

    @SubscribeEvent
    static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SyncWeaponDataTask(event.getListener()));
    }
}
