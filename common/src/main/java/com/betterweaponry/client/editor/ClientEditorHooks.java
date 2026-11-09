package com.betterweaponry.client.editor;

import com.betterweaponry.editor.EditorHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Ce que fait le client quand le serveur ouvre l'éditeur ou répond à un enregistrement. */
public final class ClientEditorHooks implements EditorHooks.ClientSide {

    @Override
    public void openEditor(boolean canEdit) {
        Minecraft.getInstance().setScreen(WeaponEditorScreen.inGame(null, canEdit));
    }

    /** Affiché dans l'éditeur s'il est encore ouvert, sinon dans le chat. */
    @Override
    public void onSaveResult(String documentId, boolean success, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof WeaponEditorScreen editor) {
            editor.onSaveResult(documentId, success, message);
        } else if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }
    }
}
