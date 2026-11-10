package com.betterweaponry.fabric;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.network.Payload;
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
    private static final Map<Class<? extends Payload>, ResourceLocation> CHANNELS = Map.of(WeaponDataSyncPayload.class, channel("weapon_data"));

    private FabricNetwork() {
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
