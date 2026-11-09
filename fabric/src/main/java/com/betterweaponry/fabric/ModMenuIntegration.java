package com.betterweaponry.fabric;

import com.betterweaponry.client.editor.WeaponEditorScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Bouton de configuration dans la liste des mods de Mod Menu : ouvre l'éditeur des armes. */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WeaponEditorScreen::fromModList;
    }
}
