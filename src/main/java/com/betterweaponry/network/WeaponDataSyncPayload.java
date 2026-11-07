package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponData;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Envoie au client les données des armes du serveur (stats et règles), pour que les tooltips,
 * la recharge affichée et la prédiction des coups correspondent au serveur.
 * Envoyé pendant la connexion ({@link SyncWeaponDataTask}) puis après chaque {@code /reload}.
 * <p>
 * Les données voyagent en JSON : le même format que les fichiers, simple à déboguer.
 */
public record WeaponDataSyncPayload(WeaponData.Snapshot snapshot) implements CustomPacketPayload {
    public static final Type<WeaponDataSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "weapon_data"));

    /** Taille maximale du JSON (1 million de caractères) : très au-dessus des besoins réels. */
    private static final int MAX_LENGTH = 1 << 20;

    public static final StreamCodec<ByteBuf, WeaponDataSyncPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(MAX_LENGTH).map(WeaponDataSyncPayload::fromJson, WeaponDataSyncPayload::toJson);

    @Override
    public Type<WeaponDataSyncPayload> type() {
        return TYPE;
    }

    /** Côté client : remplace les données en vigueur par celles du serveur. */
    public static void handle(WeaponDataSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WeaponData.install(payload.snapshot()));
    }

    private static WeaponDataSyncPayload fromJson(String json) {
        WeaponData.Snapshot snapshot = WeaponData.Snapshot.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(message -> new IllegalStateException("Invalid weapon data received from the server: " + message));
        return new WeaponDataSyncPayload(snapshot);
    }

    private String toJson() {
        return WeaponData.Snapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot)
                .getOrThrow(message -> new IllegalStateException("Could not encode weapon data: " + message))
                .toString();
    }
}
