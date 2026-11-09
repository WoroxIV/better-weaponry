package com.betterweaponry.client.tutorial;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.tutorial.TutorialScenes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tous les tutoriels, avec une coche sur ceux déjà vus. */
final class TutorialListScreen extends Screen {
    private static final ResourceLocation SEEN_SPRITE = ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "tutorial/seen");
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int COLUMNS = 2;
    private static final int TOP = 32;
    private static final int TITLE_Y = 12;
    private static final int SPRITE_SIZE = 10;
    private static final int SPRITE_MARGIN = 5;
    private static final int TITLE_COLOR = 0xFFFFFFFF;

    @Nullable private final Screen tutorialParent;
    @Nullable private final Screen returnTo;
    private final Map<String, Button> buttons = new LinkedHashMap<>();

    /**
     * @param tutorialParent écran auquel revient un tutoriel ouvert depuis cette liste
     * @param returnTo       écran auquel revient la liste quand on la ferme
     */
    TutorialListScreen(@Nullable Screen tutorialParent, @Nullable Screen returnTo) {
        super(Component.translatable(BetterWeaponry.MODID + ".tutorial.ui.list"));
        this.tutorialParent = tutorialParent;
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        buttons.clear();
        List<String> ids = TutorialScenes.IDS;
        int left = (width - COLUMNS * BUTTON_WIDTH - (COLUMNS - 1) * GAP) / 2;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            int x = left + i % COLUMNS * (BUTTON_WIDTH + GAP);
            int y = TOP + i / COLUMNS * (BUTTON_HEIGHT + GAP);
            buttons.put(id, addRenderableWidget(Button.builder(TutorialScenes.title(id), pressed -> minecraft.setScreen(new TutorialScreen(tutorialParent, id)))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        }
        int bottom = TOP + (ids.size() + COLUMNS - 1) / COLUMNS * (BUTTON_HEIGHT + GAP) + GAP;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, pressed -> onClose())
                .bounds((width - BUTTON_WIDTH) / 2, bottom, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, TITLE_Y, TITLE_COLOR);
        buttons.forEach((id, button) -> {
            if (ClientConfig.isTutorialSeen(id)) {
                graphics.blitSprite(SEEN_SPRITE, button.getX() + SPRITE_MARGIN, button.getY() + SPRITE_MARGIN, SPRITE_SIZE, SPRITE_SIZE);
            }
        });
    }

    @Override
    public void onClose() {
        minecraft.setScreen(returnTo);
    }
}
