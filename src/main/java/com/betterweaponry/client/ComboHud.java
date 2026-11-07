package com.betterweaponry.client;

import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Indicateur du combo sous le viseur : un point par coup du combo, blancs pour les coups déjà portés,
 * doré pour le prochain s'il est spécial. N'apparaît qu'une fois le combo commencé.
 * Désactivable dans la config client ("show_combo_hud").
 */
public final class ComboHud {
    private static final int DOT_SIZE = 2;
    private static final int DOT_SPACING = 4;
    /** Sous l'indicateur de recharge vanilla du viseur. */
    private static final int OFFSET_BELOW_CROSSHAIR = 16;
    private static final int DONE_COLOR = 0xFFFFFFFF;
    private static final int SPECIAL_COLOR = 0xFFFFAA00;
    private static final int REMAINING_COLOR = 0x80FFFFFF;

    private ComboHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !minecraft.options.getCameraType().isFirstPerson()
                || !ClientConfig.get(ClientConfig.SHOW_COMBO_HUD)) return;
        ComboSystem.Progress progress = ComboSystem.progress(player);
        if (progress == null) return;

        int width = progress.steps() * DOT_SPACING - (DOT_SPACING - DOT_SIZE);
        int left = graphics.guiWidth() / 2 - width / 2;
        int top = graphics.guiHeight() / 2 + OFFSET_BELOW_CROSSHAIR;
        for (int i = 0; i < progress.steps(); i++) {
            int color = i < progress.done() ? DONE_COLOR
                    : i == progress.done() && progress.nextIsSpecial() ? SPECIAL_COLOR
                    : REMAINING_COLOR;
            int x = left + i * DOT_SPACING;
            graphics.fill(x, top, x + DOT_SIZE, top + DOT_SIZE, color);
        }
    }
}
