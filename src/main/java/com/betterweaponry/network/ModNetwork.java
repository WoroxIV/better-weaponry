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
    private static final String PROTOCOL_VERSION = "3";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                // "common" : utilisable pendant la connexion (configuration) et en jeu (après /reload)
                .commonToClient(WeaponDataSyncPayload.TYPE, WeaponDataSyncPayload.STREAM_CODEC, WeaponDataSyncPayload::handle)
                // en jeu seulement : combo corrigé par le serveur, animation à deux bras des autres joueurs
                .playToClient(ComboSyncPayload.TYPE, ComboSyncPayload.STREAM_CODEC, ComboSyncPayload::handle)
                .playToClient(DualSwingPayload.TYPE, DualSwingPayload.STREAM_CODEC, DualSwingPayload::handle)
                // éditeur en jeu : ouverture (commande), enregistrement, réponse
                .playToClient(OpenEditorPayload.TYPE, OpenEditorPayload.STREAM_CODEC, OpenEditorPayload::handle)
                .playToServer(SaveDocumentPayload.TYPE, SaveDocumentPayload.STREAM_CODEC, SaveDocumentPayload::handle)
                .playToClient(EditorResultPayload.TYPE, EditorResultPayload.STREAM_CODEC, EditorResultPayload::handle);
    }

    @SubscribeEvent
    public static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SyncWeaponDataTask(event.getListener()));
    }
}
