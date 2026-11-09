package com.betterweaponry.network;

import com.betterweaponry.editor.EditorHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Serveur → client : résultat d'un enregistrement fait dans l'éditeur (message traduit côté client). */
public record EditorResultPayload(String documentId, boolean success, Component message) implements Payload {
    public static EditorResultPayload read(FriendlyByteBuf buffer) {
        return new EditorResultPayload(buffer.readUtf(), buffer.readBoolean(), buffer.readComponent());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(documentId);
        buffer.writeBoolean(success);
        buffer.writeComponent(message);
    }

    public static void handle(EditorResultPayload payload) {
        EditorHooks.onSaveResult(payload.documentId(), payload.success(), payload.message());
    }
}
