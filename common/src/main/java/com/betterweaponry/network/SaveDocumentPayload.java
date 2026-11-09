package com.betterweaponry.network;

import com.betterweaponry.editor.EditorService;
import com.betterweaponry.editor.EditorTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → serveur : enregistrer un fichier modifié dans l'éditeur. Le serveur vérifie les droits et les valeurs.
 *
 * @param documentId id du type d'arme, ou "rules"
 * @param target     {@link EditorTarget} (index)
 * @param json       valeurs complètes du fichier
 * @param last       dernier fichier de ce clic sur Enregistrer : le serveur recharge alors les données, une seule fois
 */
public record SaveDocumentPayload(String documentId, int target, String json, boolean last) implements Payload {
    private static final int MAX_ID_LENGTH = 64;
    /** Un fichier d'arme complet fait environ 2 000 caractères : large marge. */
    private static final int MAX_JSON_LENGTH = 1 << 16;

    public static SaveDocumentPayload read(FriendlyByteBuf buffer) {
        return new SaveDocumentPayload(buffer.readUtf(MAX_ID_LENGTH), buffer.readVarInt(), buffer.readUtf(MAX_JSON_LENGTH), buffer.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(documentId, MAX_ID_LENGTH);
        buffer.writeVarInt(target);
        buffer.writeUtf(json, MAX_JSON_LENGTH);
        buffer.writeBoolean(last);
    }

    public static void handle(SaveDocumentPayload payload, ServerPlayer player) {
        EditorService.handleSave(player, payload.documentId(), EditorTarget.byIndex(payload.target()), payload.json(), payload.last());
    }
}
