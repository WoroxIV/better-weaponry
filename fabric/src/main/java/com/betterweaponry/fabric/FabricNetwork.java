package com.betterweaponry.fabric;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.network.ComboSyncPayload;
import com.betterweaponry.network.DualSwingPayload;
import com.betterweaponry.network.EditorResultPayload;
import com.betterweaponry.network.OpenEditorPayload;
import com.betterweaponry.network.SaveDocumentPayload;
import com.betterweaponry.network.WeaponDataSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.function.Consumer;

/** Paquets réseau du mod, côté commun : déclaration des formats et réception des paquets envoyés au serveur. */
final class FabricNetwork {

    private FabricNetwork() {
    }

    static void register() {
        // Pendant la connexion, et en jeu après un /reload.
        PayloadTypeRegistry.configurationS2C().register(WeaponDataSyncPayload.TYPE, WeaponDataSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(WeaponDataSyncPayload.TYPE, WeaponDataSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ComboSyncPayload.TYPE, ComboSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DualSwingPayload.TYPE, DualSwingPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenEditorPayload.TYPE, OpenEditorPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EditorResultPayload.TYPE, EditorResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SaveDocumentPayload.TYPE, SaveDocumentPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SaveDocumentPayload.TYPE, (payload, context) -> SaveDocumentPayload.handle(payload, context.player()));

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, WeaponDataSyncPayload.TYPE)) handler.addTask(new SyncWeaponDataTask(handler));
        });
    }

    /** Étape de la connexion : les stats sont déjà bonnes quand JEI/EMI et l'onglet créatif construisent leurs index. */
    private record SyncWeaponDataTask(ServerConfigurationPacketListenerImpl handler) implements ConfigurationTask {
        private static final Type TYPE = new Type(BetterWeaponry.MODID + ":sync_weapon_data");

        @Override
        public void start(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new WeaponDataSyncPayload(WeaponData.current())));
            handler.completeTask(TYPE);
        }

        @Override
        public Type type() {
            return TYPE;
        }
    }
}
