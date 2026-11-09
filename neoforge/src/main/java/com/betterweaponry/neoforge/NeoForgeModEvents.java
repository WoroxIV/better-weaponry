package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.network.ComboSyncPayload;
import com.betterweaponry.network.DualSwingPayload;
import com.betterweaponry.network.EditorResultPayload;
import com.betterweaponry.network.OpenEditorPayload;
import com.betterweaponry.network.SaveDocumentPayload;
import com.betterweaponry.network.SyncWeaponDataTask;
import com.betterweaponry.network.WeaponDataSyncPayload;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Événements du bus du mod (au démarrage) : attributs, paquets réseau. */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
final class NeoForgeModEvents {
    /** À augmenter quand le format d'un paquet change : un client et un serveur différents refuseront de se connecter. */
    private static final String PROTOCOL_VERSION = "4";

    private NeoForgeModEvents() {
    }

    /** Toutes les créatures reçoivent l'attribut (valeur 0) : joueurs, mais aussi mobs qui tiendraient une arme. */
    @SubscribeEvent
    static void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (!event.has(type, ModAttributes.ARMOR_PENETRATION.holder())) event.add(type, ModAttributes.ARMOR_PENETRATION.holder());
        }
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                // "common" : utilisable pendant la connexion (configuration) et en jeu (après /reload)
                .commonToClient(WeaponDataSyncPayload.TYPE, WeaponDataSyncPayload.STREAM_CODEC, onMainThread(WeaponDataSyncPayload::handle))
                // en jeu seulement : combo corrigé par le serveur, animation à deux bras des autres joueurs
                .playToClient(ComboSyncPayload.TYPE, ComboSyncPayload.STREAM_CODEC, withPlayer(ComboSyncPayload::handle))
                .playToClient(DualSwingPayload.TYPE, DualSwingPayload.STREAM_CODEC, withPlayer(DualSwingPayload::handle))
                // éditeur en jeu : ouverture (commande), enregistrement, réponse
                .playToClient(OpenEditorPayload.TYPE, OpenEditorPayload.STREAM_CODEC, onMainThread(OpenEditorPayload::handle))
                .playToServer(SaveDocumentPayload.TYPE, SaveDocumentPayload.STREAM_CODEC, withPlayer(NeoForgeModEvents::handleSave))
                .playToClient(EditorResultPayload.TYPE, EditorResultPayload.STREAM_CODEC, onMainThread(EditorResultPayload::handle));
    }

    private static void handleSave(SaveDocumentPayload payload, Player player) {
        if (player instanceof ServerPlayer serverPlayer) SaveDocumentPayload.handle(payload, serverPlayer);
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> onMainThread(Consumer<T> handler) {
        return (payload, context) -> context.enqueueWork(() -> handler.accept(payload));
    }

    private static <T extends CustomPacketPayload> IPayloadHandler<T> withPlayer(BiConsumer<T, Player> handler) {
        return (payload, context) -> context.enqueueWork(() -> handler.accept(payload, context.player()));
    }

    @SubscribeEvent
    static void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new SyncWeaponDataTask(event.getListener()));
    }
}
