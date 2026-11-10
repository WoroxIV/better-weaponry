package com.betterweaponry.fabric;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.network.ComboSyncPayload;
import com.betterweaponry.network.DualSwingPayload;
import com.betterweaponry.network.EditorResultPayload;
import com.betterweaponry.network.OpenEditorPayload;
import com.betterweaponry.network.Payload;
import com.betterweaponry.network.SaveDocumentPayload;
import com.betterweaponry.network.WeaponDataSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Les paquets du mod sur Fabric : un canal par type de paquet. */
final class FabricNetwork {
    private static final Map<Class<? extends Payload>, ResourceLocation> CHANNELS = Map.of(
            WeaponDataSyncPayload.class, channel("weapon_data"),
            ComboSyncPayload.class, channel("combo"),
            DualSwingPayload.class, channel("dual_swing"),
            OpenEditorPayload.class, channel("open_editor"),
            EditorResultPayload.class, channel("editor_result"),
            SaveDocumentPayload.class, channel("save_document"));

    private FabricNetwork() {
    }

    static void register() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNELS.get(SaveDocumentPayload.class), (server, player, handler, buffer, responseSender) -> {
            // Le paquet est lu ici : le tampon n'existe plus une fois sur le fil principal.
            SaveDocumentPayload payload = SaveDocumentPayload.read(buffer);
            server.execute(() -> SaveDocumentPayload.handle(payload, player));
        });
    }

    /** Côté client uniquement : le traitement se fait sur le fil principal, une fois le joueur local connu. */
    static <T extends Payload> void toClient(Class<T> type, Function<FriendlyByteBuf, T> reader, BiConsumer<T, LocalPlayer> handler) {
        ClientPlayNetworking.registerGlobalReceiver(CHANNELS.get(type), (client, listener, buffer, responseSender) -> {
            T payload = reader.apply(buffer);
            client.execute(() -> {
                if (client.player != null) handler.accept(payload, client.player);
            });
        });
    }

    static void sendToPlayer(ServerPlayer player, Payload payload) {
        ServerPlayNetworking.send(player, CHANNELS.get(payload.getClass()), encode(payload));
    }

    static void sendToServer(Payload payload) {
        ClientPlayNetworking.send(CHANNELS.get(payload.getClass()), encode(payload));
    }

    private static FriendlyByteBuf encode(Payload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        payload.write(buffer);
        return buffer;
    }

    private static ResourceLocation channel(String name) {
        return new ResourceLocation(BetterWeaponry.MODID, name);
    }
}
