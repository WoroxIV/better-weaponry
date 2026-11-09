package com.betterweaponry.client.tutorial;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.ModKeyMappings;
import com.betterweaponry.tutorial.TutorialScenes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Petite notification en haut à droite : le tutoriel proposé pour l'arme obtenue, et la touche pour l'ouvrir. */
final class TutorialHintToast implements Toast {
    private static final long DISPLAY_MILLIS = 6000L;
    private static final int TEXT_X = 30;
    private static final int TEXT_MARGIN = 4;
    private static final int TITLE_COLOR = 0xFFFFFF00;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final ItemStack icon;
    private final String scene;

    TutorialHintToast(ItemStack icon, String scene) {
        this.icon = icon;
        this.scene = scene;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        Font font = toasts.getMinecraft().font;
        int textWidth = width() - TEXT_X - TEXT_MARGIN;
        graphics.blit(TEXTURE, 0, 0, 0, 0, width(), height());
        graphics.renderFakeItem(icon, 8, 8);
        graphics.drawString(font, font.split(TutorialScenes.title(scene), textWidth).get(0), TEXT_X, 7, TITLE_COLOR, false);
        Component action = Component.translatable(BetterWeaponry.MODID + ".tutorial.ui.hint", ModKeyMappings.OPEN_TUTORIAL.getTranslatedKeyMessage());
        graphics.drawString(font, font.split(action, textWidth).get(0), TEXT_X, 18, TEXT_COLOR, false);
        return timeSinceLastVisible >= DISPLAY_MILLIS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
