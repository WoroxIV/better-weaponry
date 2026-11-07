package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.editor.EditorHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Serveur → client : ouvrir l'éditeur (commande /betterweaponry).
 *
 * @param canEdit true = éditeur, false = Codex en lecture seule
 */
public record OpenEditorPayload(boolean canEdit) implements CustomPacketPayload {
    public static final Type<OpenEditorPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "open_editor"));

    public static final StreamCodec<ByteBuf, OpenEditorPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(OpenEditorPayload::new, OpenEditorPayload::canEdit);

    @Override
    public Type<OpenEditorPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEditorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EditorHooks.openEditor(payload.canEdit()));
    }
}
