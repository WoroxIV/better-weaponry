package com.betterweaponry.network;

import com.betterweaponry.editor.EditorHooks;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Serveur → client : ouvrir l'éditeur (commande /betterweaponry).
 *
 * @param canEdit true = éditeur, false = Codex en lecture seule
 */
public record OpenEditorPayload(boolean canEdit) implements Payload {
    public static OpenEditorPayload read(FriendlyByteBuf buffer) {
        return new OpenEditorPayload(buffer.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(canEdit);
    }

    public static void handle(OpenEditorPayload payload) {
        EditorHooks.openEditor(payload.canEdit());
    }
}
