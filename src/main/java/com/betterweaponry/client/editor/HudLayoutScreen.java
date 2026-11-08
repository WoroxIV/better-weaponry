package com.betterweaponry.client.editor;

import com.betterweaponry.client.CombatHud;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.config.HudElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Écran de placement : on attrape chaque élément de l'interface à la souris pour le déplacer. */
final class HudLayoutScreen extends Screen {
    private static final String KEY = WeaponEditorScreen.KEY + "hud.";
    private static final int PADDING = 3;
    private static final int OUTLINE_COLOR = 0x80FFFFFF;
    private static final int HOVER_COLOR = 0xFFFFFF55;
    private static final int HIDDEN_COLOR = 0x40FF5555;
    private static final int BACKGROUND_IN_GAME = 0x50000000;
    private static final int BACKGROUND_IN_MENU = 0xFF202020;
    private static final int CROSSHAIR_COLOR = 0x80FFFFFF;
    private static final int CROSSHAIR_SIZE = 4;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTONS_TOP = 32;
    private static final int LABEL_COLOR = 0xFFE0E0E0;

    private final Screen parent;
    @Nullable private HudElement dragged;
    private double grabOffsetX;
    private double grabOffsetY;

    HudLayoutScreen(Screen parent) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // En haut : les éléments se placent plutôt en bas et au centre de l'écran.
        int y = BUTTONS_TOP;
        addRenderableWidget(Button.builder(Component.translatable(KEY + "reset"), pressed -> ClientConfig.resetHudPositions())
                .bounds(width / 2 - BUTTON_WIDTH - 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), pressed -> onClose())
                .bounds(width / 2 + 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, minecraft.level != null ? BACKGROUND_IN_GAME : BACKGROUND_IN_MENU);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(KEY + "instructions"), width / 2, 20, 0xA0A0A0);

        // Repère : le viseur, pour placer les éléments par rapport au centre de l'écran.
        int middleX = width / 2;
        int middleY = height / 2;
        graphics.fill(middleX - CROSSHAIR_SIZE, middleY, middleX + CROSSHAIR_SIZE + 1, middleY + 1, CROSSHAIR_COLOR);
        graphics.fill(middleX, middleY - CROSSHAIR_SIZE, middleX + 1, middleY + CROSSHAIR_SIZE + 1, CROSSHAIR_COLOR);

        HudElement hovered = elementAt(mouseX, mouseY);
        for (HudElement element : HudElement.values()) {
            CombatHud.Size size = CombatHud.sampleSize(element);
            int[] center = CombatHud.centerOf(element, size, width, height);
            CombatHud.drawSample(graphics, element, center[0], center[1]);

            boolean shown = ClientConfig.get(ClientConfig.hud(element).show());
            int left = center[0] - size.width() / 2 - PADDING;
            int top = center[1] - size.height() / 2 - PADDING;
            if (!shown) graphics.fill(left, top, left + size.width() + 2 * PADDING, top + size.height() + 2 * PADDING, HIDDEN_COLOR);
            graphics.renderOutline(left, top, size.width() + 2 * PADDING, size.height() + 2 * PADDING, element == hovered || element == dragged ? HOVER_COLOR : OUTLINE_COLOR);
            if (element == hovered && dragged == null) {
                graphics.renderTooltip(font, Component.translatable(WeaponEditorScreen.KEY + "hud.element." + element.id()), mouseX, mouseY);
            }
        }
    }

    @Nullable
    private HudElement elementAt(double mouseX, double mouseY) {
        for (HudElement element : HudElement.values()) {
            CombatHud.Size size = CombatHud.sampleSize(element);
            int[] center = CombatHud.centerOf(element, size, width, height);
            if (Math.abs(mouseX - center[0]) <= size.width() / 2.0D + PADDING && Math.abs(mouseY - center[1]) <= size.height() / 2.0D + PADDING) {
                return element;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        HudElement element = elementAt(mouseX, mouseY);
        if (element == null) return false;
        int[] center = CombatHud.centerOf(element, CombatHud.sampleSize(element), width, height);
        dragged = element;
        grabOffsetX = mouseX - center[0];
        grabOffsetY = mouseY - center[1];
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragged == null) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        ClientConfig.hud(dragged).x().set(Mth.clamp((mouseX - grabOffsetX) / width, 0.0D, 1.0D));
        ClientConfig.hud(dragged).y().set(Mth.clamp((mouseY - grabOffsetY) / height, 0.0D, 1.0D));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragged != null) {
            dragged = null;
            ClientConfig.SPEC.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ClientConfig.SPEC.save();
        minecraft.setScreen(parent);
    }
}
