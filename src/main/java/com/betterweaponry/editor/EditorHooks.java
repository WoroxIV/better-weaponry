package com.betterweaponry.editor;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Pont vers l'écran de l'éditeur, qui n'existe que côté client : les paquets reçus par le client passent par ici.
 * Le point d'entrée client y branche son implémentation ; sur un serveur dédié, rien n'est branché.
 */
public final class EditorHooks {

    /** Ce que le client sait faire pour l'éditeur. */
    public interface ClientSide {
        /** Ouvre l'éditeur (canEdit) ou le Codex en lecture seule. */
        void openEditor(boolean canEdit);

        /** Réponse du serveur à un enregistrement. */
        void onSaveResult(String documentId, boolean success, Component message);
    }

    @Nullable
    private static volatile ClientSide client;

    private EditorHooks() {
    }

    public static void setClient(ClientSide clientSide) {
        client = clientSide;
    }

    public static void openEditor(boolean canEdit) {
        ClientSide current = client;
        if (current != null) current.openEditor(canEdit);
    }

    public static void onSaveResult(String documentId, boolean success, Component message) {
        ClientSide current = client;
        if (current != null) current.onSaveResult(documentId, success, message);
    }
}
