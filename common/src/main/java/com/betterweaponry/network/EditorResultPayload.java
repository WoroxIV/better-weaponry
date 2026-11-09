package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.editor.EditorHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur → client : résultat d'un enregistrement fait dans l'éditeur (message traduit côté client). */
public record EditorResultPayload(String documentId, boolean success, Component message) implements CustomPacketPayload {
    public static final Type<EditorResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "editor_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditorResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EditorResultPayload::documentId,
            ByteBufCodecs.BOOL, EditorResultPayload::success,
            ComponentSerialization.TRUSTED_STREAM_CODEC, EditorResultPayload::message,
            EditorResultPayload::new);

    @Override
    public Type<EditorResultPayload> type() {
        return TYPE;
    }

    public static void handle(EditorResultPayload payload) {
        EditorHooks.onSaveResult(payload.documentId(), payload.success(), payload.message());
    }
}
