package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.editor.EditorService;
import com.betterweaponry.editor.EditorTarget;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → serveur : enregistrer un fichier modifié dans l'éditeur. Le serveur vérifie les droits et les valeurs.
 *
 * @param documentId id du type d'arme, ou "rules"
 * @param target     {@link EditorTarget} (index)
 * @param json       valeurs complètes du fichier
 */
public record SaveDocumentPayload(String documentId, int target, String json) implements CustomPacketPayload {
    public static final Type<SaveDocumentPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "save_document"));

    private static final int MAX_ID_LENGTH = 64;
    /** Un fichier d'arme complet fait environ 2 000 caractères : large marge. */
    private static final int MAX_JSON_LENGTH = 1 << 16;

    public static final StreamCodec<ByteBuf, SaveDocumentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_ID_LENGTH), SaveDocumentPayload::documentId,
            ByteBufCodecs.VAR_INT, SaveDocumentPayload::target,
            ByteBufCodecs.stringUtf8(MAX_JSON_LENGTH), SaveDocumentPayload::json,
            SaveDocumentPayload::new);

    @Override
    public Type<SaveDocumentPayload> type() {
        return TYPE;
    }

    public static void handle(SaveDocumentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                EditorService.handleSave(player, payload.documentId(), EditorTarget.byIndex(payload.target()), payload.json());
            }
        });
    }
}
