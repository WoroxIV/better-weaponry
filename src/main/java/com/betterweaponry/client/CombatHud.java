package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.AbilityCooldowns;
import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.DashAbility;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.config.ComboHudStyle;
import com.betterweaponry.config.HudElement;
import com.betterweaponry.data.DashDefinition;
import com.betterweaponry.data.ParryDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Optional;

/**
 * Éléments de combat à l'écran : avancement du combo, garde brisée, recharge de capacité.
 * Chacun se désactive et se déplace (page Interface de l'éditeur). Le dessin est séparé des conditions pour que
 * l'écran de placement montre exactement la même chose qu'en jeu.
 */
public final class CombatHud {
    private static final String KEY = BetterWeaponry.MODID + ".hud.";
    private static final int DONE_COLOR = 0xFFFFFFFF;
    private static final int SPECIAL_COLOR = 0xFFFFAA00;
    private static final int REMAINING_COLOR = 0x80FFFFFF;
    private static final int GUARD_BREAK_TEXT_COLOR = 0xFFFF7766;
    private static final int COOLDOWN_TEXT_COLOR = 0xFFE0E0E0;

    private static final int DOT_SIZE = 2;
    private static final int DOT_SPACING = 4;
    private static final int SQUARE_SIZE = 5;
    private static final int SQUARE_SPACING = 7;
    private static final int BAR_SEGMENT_WIDTH = 12;
    private static final int BAR_HEIGHT = 3;
    private static final int BAR_SPACING = 14;
    private static final int RING_RADIUS = 12;
    /** Blanc entre deux arcs de l'anneau, en degrés. */
    private static final double RING_GAP = 14.0D;
    private static final double RING_DEGREES_PER_POINT = 5.0D;
    private static final int ICON_SIZE = 16;
    private static final float ICON_SCALE = 0.75F;
    private static final int ICON_TEXT_GAP = 3;
    private static final int SCREEN_MARGIN = 2;

    private CombatHud() {
    }

    /** Taille d'un élément, pour le centrer et le rendre saisissable dans l'écran de placement. */
    public record Size(int width, int height) {
    }

    /** Couche d'interface enregistrée par le point d'entrée client. */
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) return;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

        if (isShown(HudElement.COMBO)) {
            ComboSystem.Progress progress = ComboSystem.progress(player);
            if (progress != null) drawAtConfiguredPosition(graphics, HudElement.COMBO, comboSize(progress), (x, y) -> drawCombo(graphics, progress, x, y));
        }

        ItemStack stack = player.getMainHandItem();
        float cooldown = player.getCooldowns().getCooldownPercent(stack.getItem(), partialTick);
        if (cooldown <= 0.0F) return;
        Optional<ParryDefinition> parry = ParrySystem.parryOf(stack);
        if (parry.isPresent() && isShown(HudElement.GUARD_BREAK)) {
            String seconds = seconds(AbilityCooldowns.remainingSeconds(player, stack.getItem(), cooldown));
            drawAtConfiguredPosition(graphics, HudElement.GUARD_BREAK, guardBreakSize(seconds), (x, y) -> drawGuardBreak(graphics, stack, seconds, x, y));
        }
        Optional<DashDefinition> dash = DashAbility.dashOf(stack);
        if (dash.isPresent() && isShown(HudElement.ABILITY_COOLDOWN)) {
            Component text = cooldownText(minecraft, Component.translatable(KEY + "ability.dash"), seconds(AbilityCooldowns.remainingSeconds(player, stack.getItem(), cooldown)));
            drawAtConfiguredPosition(graphics, HudElement.ABILITY_COOLDOWN, textSize(text), (x, y) -> drawText(graphics, text, COOLDOWN_TEXT_COLOR, x, y));
        }
    }

    private static boolean isShown(HudElement element) {
        return ClientConfig.get(ClientConfig.hud(element).show());
    }

    /** Ce qu'on dessine à une position donnée (centre de l'élément). */
    @FunctionalInterface
    public interface Drawing {
        void at(int centerX, int centerY);
    }

    private static void drawAtConfiguredPosition(GuiGraphics graphics, HudElement element, Size size, Drawing drawing) {
        int[] center = centerOf(element, size, graphics.guiWidth(), graphics.guiHeight());
        drawing.at(center[0], center[1]);
    }

    /**
     * Centre de l'élément à l'écran, sans qu'il puisse en sortir. L'anneau entoure le viseur tant que le joueur
     * n'a pas déplacé l'indicateur.
     */
    public static int[] centerOf(HudElement element, Size size, int screenWidth, int screenHeight) {
        double x = ClientConfig.get(ClientConfig.hud(element).x());
        double y = ClientConfig.get(ClientConfig.hud(element).y());
        boolean untouched = x == element.defaultX() && y == element.defaultY();
        if (element == HudElement.COMBO && untouched && ClientConfig.get(ClientConfig.COMBO_HUD_STYLE) == ComboHudStyle.RING) {
            return new int[]{screenWidth / 2, screenHeight / 2};
        }
        return new int[]{clampToScreen(x * screenWidth, size.width(), screenWidth), clampToScreen(y * screenHeight, size.height(), screenHeight)};
    }

    /** Position du centre pour que l'élément reste entier à l'écran, avec une petite marge. */
    private static int clampToScreen(double center, int size, int screen) {
        int halfSize = size / 2 + SCREEN_MARGIN;
        return Mth.clamp((int) Math.round(center), halfSize, Math.max(halfSize, screen - halfSize - size % 2));
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static String seconds(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static Component cooldownText(Minecraft minecraft, Component ability, String seconds) {
        return Component.translatable(KEY + "ability_cooldown", minecraft.options.keyUse.getTranslatedKeyMessage(), ability, seconds);
    }

    // --- Dessin ---

    public static Size textSize(Component text) {
        return new Size(font().width(text), font().lineHeight);
    }

    public static void drawText(GuiGraphics graphics, Component text, int color, int centerX, int centerY) {
        graphics.drawString(font(), text, centerX - font().width(text) / 2, centerY - font().lineHeight / 2, color);
    }

    public static Size guardBreakSize(String seconds) {
        int icon = Math.round(ICON_SIZE * ICON_SCALE);
        return new Size(icon + ICON_TEXT_GAP + font().width(guardBreakText(seconds)), icon);
    }

    private static Component guardBreakText(String seconds) {
        return Component.translatable(KEY + "seconds", seconds);
    }

    /** Petit et discret : l'arme concernée et le temps restant. */
    public static void drawGuardBreak(GuiGraphics graphics, ItemStack stack, String seconds, int centerX, int centerY) {
        Size size = guardBreakSize(seconds);
        int left = centerX - size.width() / 2;
        int top = centerY - size.height() / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0F);
        graphics.pose().scale(ICON_SCALE, ICON_SCALE, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
        graphics.drawString(font(), guardBreakText(seconds), left + size.height() + ICON_TEXT_GAP, centerY - font().lineHeight / 2, GUARD_BREAK_TEXT_COLOR);
    }

    public static Size comboSize(ComboSystem.Progress progress) {
        return switch (ClientConfig.get(ClientConfig.COMBO_HUD_STYLE)) {
            case DOTS -> new Size(progress.steps() * DOT_SPACING - (DOT_SPACING - DOT_SIZE), DOT_SIZE);
            case SQUARES -> new Size(progress.steps() * SQUARE_SPACING - (SQUARE_SPACING - SQUARE_SIZE), SQUARE_SIZE);
            case BAR -> new Size(progress.steps() * BAR_SPACING - (BAR_SPACING - BAR_SEGMENT_WIDTH), BAR_HEIGHT);
            case RING -> new Size(2 * RING_RADIUS + 2, 2 * RING_RADIUS + 2);
            case COUNTER -> textSize(counterText(progress));
        };
    }

    private static Component counterText(ComboSystem.Progress progress) {
        return Component.literal((progress.done() + 1) + "/" + progress.steps());
    }

    /** Couleur d'un coup du combo : déjà porté, prochain coup spécial, ou encore à venir. */
    private static int stepColor(ComboSystem.Progress progress, int step) {
        if (step < progress.done()) return DONE_COLOR;
        return step == progress.done() && progress.nextIsSpecial() ? SPECIAL_COLOR : REMAINING_COLOR;
    }

    public static void drawCombo(GuiGraphics graphics, ComboSystem.Progress progress, int centerX, int centerY) {
        ComboHudStyle style = ClientConfig.get(ClientConfig.COMBO_HUD_STYLE);
        Size size = comboSize(progress);
        int left = centerX - size.width() / 2;
        int top = centerY - size.height() / 2;
        switch (style) {
            case DOTS -> drawRow(graphics, progress, left, top, DOT_SIZE, DOT_SIZE, DOT_SPACING);
            case SQUARES -> drawRow(graphics, progress, left, top, SQUARE_SIZE, SQUARE_SIZE, SQUARE_SPACING);
            case BAR -> drawRow(graphics, progress, left, top, BAR_SEGMENT_WIDTH, BAR_HEIGHT, BAR_SPACING);
            case RING -> drawRing(graphics, progress, centerX, centerY);
            case COUNTER -> drawText(graphics, counterText(progress), progress.nextIsSpecial() ? SPECIAL_COLOR : DONE_COLOR, centerX, centerY);
        }
    }

    private static void drawRow(GuiGraphics graphics, ComboSystem.Progress progress, int left, int top, int width, int height, int spacing) {
        for (int i = 0; i < progress.steps(); i++) {
            int x = left + i * spacing;
            graphics.fill(x, top, x + width, top + height, stepColor(progress, i));
        }
    }

    /** Un arc par coup du combo, à partir du haut et dans le sens des aiguilles d'une montre. */
    private static void drawRing(GuiGraphics graphics, ComboSystem.Progress progress, int centerX, int centerY) {
        double arc = 360.0D / progress.steps();
        for (int i = 0; i < progress.steps(); i++) {
            int color = stepColor(progress, i);
            double start = i * arc + RING_GAP / 2.0D;
            double end = (i + 1) * arc - RING_GAP / 2.0D;
            for (double angle = start; angle <= end; angle += RING_DEGREES_PER_POINT) {
                int x = centerX + (int) Math.round(Math.sin(Math.toRadians(angle)) * RING_RADIUS);
                int y = centerY - (int) Math.round(Math.cos(Math.toRadians(angle)) * RING_RADIUS);
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    // --- Exemples, pour l'écran de placement et l'aperçu des styles ---

    private static final ComboSystem.Progress SAMPLE_PROGRESS = new ComboSystem.Progress(3, 2, true);
    private static final String SAMPLE_SECONDS = "1.5";

    public static Size sampleSize(HudElement element) {
        return switch (element) {
            case COMBO -> comboSize(SAMPLE_PROGRESS);
            case GUARD_BREAK -> guardBreakSize(SAMPLE_SECONDS);
            case ABILITY_COOLDOWN -> textSize(sampleCooldownText());
        };
    }

    public static void drawSample(GuiGraphics graphics, HudElement element, int centerX, int centerY) {
        switch (element) {
            case COMBO -> drawCombo(graphics, SAMPLE_PROGRESS, centerX, centerY);
            case GUARD_BREAK -> drawGuardBreak(graphics, new ItemStack(Items.IRON_SWORD), SAMPLE_SECONDS, centerX, centerY);
            case ABILITY_COOLDOWN -> drawText(graphics, sampleCooldownText(), COOLDOWN_TEXT_COLOR, centerX, centerY);
        }
    }

    private static Component sampleCooldownText() {
        return cooldownText(Minecraft.getInstance(), Component.translatable(KEY + "ability.dash"), SAMPLE_SECONDS);
    }
}
