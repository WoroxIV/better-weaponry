package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Touches du mod, réglables dans les options de contrôles du jeu. */
public final class ModKeyMappings {
    private static final String CATEGORY = "key.categories." + BetterWeaponry.MODID;

    /** Contexte "interface" : pas de conflit avec Maj = s'accroupir, qui ne compte qu'en jeu. */
    public static final KeyMapping WEAPON_DETAILS = new KeyMapping("key." + BetterWeaponry.MODID + ".weapon_details",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, CATEGORY);

    /** Ouvre le tutoriel qui vient d'être proposé, sinon la liste des tutoriels. Pas K : c'est la touche des shaders d'Iris. */
    public static final KeyMapping OPEN_TUTORIAL = new KeyMapping("key." + BetterWeaponry.MODID + ".open_tutorial",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    private ModKeyMappings() {
    }

    /**
     * Vrai si la touche "Détails de l'arme" est maintenue. On lit directement le clavier : le jeu ne met pas à jour
     * l'état des touches quand un menu est ouvert, or c'est justement là qu'on survole les objets.
     */
    public static boolean weaponDetailsHeld() {
        if (WEAPON_DETAILS.isUnbound()) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key key = WEAPON_DETAILS.getKey();
        return key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key.getValue());
    }

    /** Nom de la touche, tel que le joueur l'a réglée. */
    public static Component weaponDetailsKeyName() {
        return WEAPON_DETAILS.getTranslatedKeyMessage();
    }
}
