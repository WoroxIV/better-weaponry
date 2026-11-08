package com.betterweaponry.client.tutorial;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.tutorial.TutorialScene;
import com.betterweaponry.tutorial.TutorialScenes;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Écran d'un tutoriel, à la manière des scènes de Create : une petite scène qui se joue, un texte qui la décrit, une frise
 * des étapes. Glisser la souris fait tourner la caméra, la molette zoome.
 */
public final class TutorialScreen extends Screen {
    private static final String KEY = BetterWeaponry.MODID + ".tutorial.ui.";
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_GAP = 4;
    private static final int STEP_BAR_HEIGHT = 5;
    private static final int STEP_GAP = 2;
    private static final int STEP_BAR_WIDTH = 240;
    private static final int CAPTION_WIDTH = 300;
    private static final int TIMING_BAR_WIDTH = 200;
    private static final int TIMING_BAR_HEIGHT = 6;
    private static final int TIMING_BAR_Y = 34;
    private static final int LINE_HEIGHT = 10;
    private static final int PANEL_PADDING = 4;
    private static final float DEFAULT_YAW = -35.0F;
    private static final float DEFAULT_PITCH = 30.0F;
    private static final float MIN_PITCH = 5.0F;
    private static final float MAX_PITCH = 85.0F;
    private static final float MIN_ZOOM = 0.6F;
    private static final float MAX_ZOOM = 1.8F;
    private static final float ZOOM_STEP = 0.1F;
    private static final float DRAG_SPEED = 0.8F;
    private static final float BLOCKS_ACROSS = 10.5F;
    private static final int SPARK_RAYS = 8;
    private static final float SPARK_RADIUS = 16.0F;
    private static final int BACKGROUND_TOP = 0xF0101018;
    private static final int BACKGROUND_BOTTOM = 0xF0202028;
    private static final int PANEL_COLOR = 0xC0000000;
    private static final int TRACK_COLOR = 0xFF404048;
    private static final int STEP_DONE_COLOR = 0xFF55FFFF;
    private static final int WINDOW_COLOR = 0xFF55FF55;
    private static final int LATE_COLOR = 0xFFFFAA00;
    private static final int HIT_COLOR = 0xFFFF5555;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFA0A0A0;

    @Nullable private final Screen parent;
    private TutorialRenderer renderer;
    private String sceneId;
    private TutorialScene scene;
    private int tick;
    private boolean playing = true;
    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float zoom = 1.0F;
    private boolean draggingCamera;
    private Button playButton;

    public TutorialScreen(@Nullable Screen parent, String sceneId) {
        super(TutorialScenes.title(sceneId));
        this.parent = parent;
        this.sceneId = sceneId;
        this.scene = TutorialScenes.create(sceneId);
    }

    @Override
    protected void init() {
        if (renderer == null) renderer = new TutorialRenderer();
        int y = height - BUTTON_SIZE - 6;
        int count = 5;
        int x = (width - (count * BUTTON_SIZE + (count - 1) * BUTTON_GAP)) / 2;
        addIconButton("«", "previous_step", x, y, this::previousStep);
        x += BUTTON_SIZE + BUTTON_GAP;
        playButton = addIconButton(playLabel(), "play_pause", x, y, this::togglePlaying);
        x += BUTTON_SIZE + BUTTON_GAP;
        addIconButton("↺", "replay", x, y, this::replay);
        x += BUTTON_SIZE + BUTTON_GAP;
        addIconButton("»", "next_step", x, y, this::nextStep);
        x += BUTTON_SIZE + BUTTON_GAP;
        addIconButton("✕", "close", x, y, this::onClose);

        addIconButton("<", "previous_tutorial", width - 2 * BUTTON_SIZE - BUTTON_GAP - 6, 6, () -> changeScene(-1));
        addIconButton(">", "next_tutorial", width - BUTTON_SIZE - 6, 6, () -> changeScene(1));
    }

    private Button addIconButton(String label, String tooltipKey, int x, int y, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), pressed -> action.run())
                .bounds(x, y, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable(KEY + tooltipKey)))
                .build());
    }

    private String playLabel() {
        return playing && tick < scene.duration() ? "‖" : "▶";
    }

    // --- Lecture ---

    @Override
    public void tick() {
        if (playing && tick < scene.duration()) tick++;
        if (playButton != null) playButton.setMessage(Component.literal(playLabel()));
    }

    private void togglePlaying() {
        if (tick >= scene.duration()) {
            replay();
            return;
        }
        playing = !playing;
    }

    private void replay() {
        tick = 0;
        playing = true;
    }

    /** Une étape en arrière, ou au début de l'étape en cours si on vient d'y entrer depuis un moment. */
    private void previousStep() {
        int index = scene.stepAt(tick);
        boolean justStarted = tick - scene.steps().get(index).start() < 10;
        seek(scene.steps().get(justStarted ? Math.max(0, index - 1) : index).start());
    }

    private void nextStep() {
        int index = scene.stepAt(tick);
        if (index + 1 < scene.steps().size()) seek(scene.steps().get(index + 1).start());
        else seek(scene.duration());
    }

    private void seek(int target) {
        tick = Mth.clamp(target, 0, scene.duration());
    }

    private void changeScene(int direction) {
        List<String> ids = TutorialScenes.IDS;
        sceneId = ids.get(Math.floorMod(ids.indexOf(sceneId) + direction, ids.size()));
        scene = TutorialScenes.create(sceneId);
        replay();
    }

    private float playhead(float partialTick) {
        return playing && tick < scene.duration() ? Math.min(scene.duration(), tick + partialTick) : tick;
    }

    // --- Dessin ---

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, BACKGROUND_TOP, BACKGROUND_BOTTOM);
        float scale = Math.min(width, height * 1.3F) / BLOCKS_ACROSS * zoom;
        renderer.render(graphics, scene, playhead(partialTick), width / 2, height / 2 + 6, scale, yaw, pitch);
        // Les widgets se dessinent à plat : la profondeur de la scène ne doit pas les masquer.
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        float time = playhead(partialTick);
        drawOverlays(graphics);
        graphics.drawString(font, title, 8, 8, TEXT_COLOR);
        drawTimingBars(graphics, time);
        drawStepBar(graphics, time);
        drawCaption(graphics, time);
    }

    private void drawOverlays(GuiGraphics graphics) {
        for (TutorialRenderer.ScreenSpark spark : renderer.sparks) {
            float reach = SPARK_RADIUS * spark.progress();
            float alpha = 1.0F - spark.progress();
            int color = ((int) (alpha * 255.0F)) << 24 | spark.color() & 0xFFFFFF;
            for (int i = 0; i < SPARK_RAYS; i++) {
                float angle = Mth.TWO_PI * i / SPARK_RAYS;
                int x = (int) (spark.x() + Mth.cos(angle) * reach);
                int y = (int) (spark.y() + Mth.sin(angle) * reach);
                graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
            }
        }
        for (TutorialRenderer.ScreenLabel label : renderer.labels) {
            graphics.drawCenteredString(font, label.text(), (int) label.x(), (int) label.y(), label.color());
        }
    }

    private int stepBarLeft() {
        return (width - STEP_BAR_WIDTH) / 2;
    }

    private int stepBarY() {
        return height - BUTTON_SIZE - 6 - STEP_BAR_HEIGHT - 4;
    }

    /** Fin de l'étape, ou de la scène pour la dernière. */
    private int stepEnd(int index) {
        return index + 1 < scene.steps().size() ? scene.steps().get(index + 1).start() : scene.duration();
    }

    private void drawStepBar(GuiGraphics graphics, float time) {
        int left = stepBarLeft();
        int y = stepBarY();
        for (int i = 0; i < scene.steps().size(); i++) {
            int segmentLeft = left + scene.steps().get(i).start() * STEP_BAR_WIDTH / scene.duration();
            int segmentRight = left + stepEnd(i) * STEP_BAR_WIDTH / scene.duration() - STEP_GAP;
            graphics.fill(segmentLeft, y, segmentRight, y + STEP_BAR_HEIGHT, TRACK_COLOR);
            float done = Mth.clamp((time - scene.steps().get(i).start()) / (stepEnd(i) - scene.steps().get(i).start()), 0.0F, 1.0F);
            if (done > 0.0F) graphics.fill(segmentLeft, y, segmentLeft + (int) ((segmentRight - segmentLeft) * done), y + STEP_BAR_HEIGHT, STEP_DONE_COLOR);
        }
    }

    private void drawCaption(GuiGraphics graphics, float time) {
        List<FormattedCharSequence> lines = font.split(scene.steps().get(scene.stepAt(time)).caption(), Math.min(CAPTION_WIDTH, width - 20));
        int blockHeight = lines.size() * LINE_HEIGHT;
        int bottom = stepBarY() - 6;
        int top = bottom - blockHeight;
        int maxWidth = 0;
        for (FormattedCharSequence line : lines) maxWidth = Math.max(maxWidth, font.width(line));
        graphics.fill((width - maxWidth) / 2 - PANEL_PADDING, top - PANEL_PADDING, (width + maxWidth) / 2 + PANEL_PADDING, bottom + PANEL_PADDING - 2, PANEL_COLOR);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawCenteredString(font, lines.get(i), width / 2, top + i * LINE_HEIGHT, TEXT_COLOR);
        }
    }

    /** Frise de la parade parfaite : la garde, la fenêtre en vert, le coup, et l'instant présent. */
    private void drawTimingBars(GuiGraphics graphics, float time) {
        for (TutorialScene.TimingBar bar : scene.bars()) {
            if (time < bar.from() || time >= bar.to()) continue;
            int left = (width - TIMING_BAR_WIDTH) / 2;
            int y = TIMING_BAR_Y;
            int length = bar.to() - bar.from();
            graphics.fill(left, y, left + TIMING_BAR_WIDTH, y + TIMING_BAR_HEIGHT, TRACK_COLOR);
            int windowLeft = left + (bar.guard() - bar.from()) * TIMING_BAR_WIDTH / length;
            int windowRight = left + (bar.windowEnd() - bar.from()) * TIMING_BAR_WIDTH / length;
            graphics.fill(windowLeft, y, windowRight, y + TIMING_BAR_HEIGHT, WINDOW_COLOR);
            int hitX = left + (bar.hit() - bar.from()) * TIMING_BAR_WIDTH / length;
            graphics.fill(hitX - 1, y - 3, hitX + 1, y + TIMING_BAR_HEIGHT + 3, bar.perfect() ? LATE_COLOR : HIT_COLOR);
            int playX = left + (int) ((time - bar.from()) * TIMING_BAR_WIDTH / length);
            graphics.fill(playX, y - 2, playX + 1, y + TIMING_BAR_HEIGHT + 2, TEXT_COLOR);
            graphics.drawCenteredString(font, Component.translatable(KEY + "perfect_window"), (windowLeft + windowRight) / 2, y + TIMING_BAR_HEIGHT + 4, WINDOW_COLOR);
            graphics.drawString(font, Component.translatable(KEY + "slow_motion", TutorialScenes.SLOW_MOTION), left, y - 11, MUTED_COLOR);
            graphics.drawCenteredString(font, Component.translatable(KEY + "hit"), hitX, y - 13, bar.perfect() ? LATE_COLOR : HIT_COLOR);
        }
    }

    // --- Souris ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        int left = stepBarLeft();
        int y = stepBarY();
        if (mouseX >= left && mouseX < left + STEP_BAR_WIDTH && mouseY >= y - 3 && mouseY < y + STEP_BAR_HEIGHT + 3) {
            seek((int) ((mouseX - left) * scene.duration() / STEP_BAR_WIDTH));
            return true;
        }
        draggingCamera = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingCamera = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingCamera) {
            yaw += (float) dragX * DRAG_SPEED;
            pitch = Mth.clamp(pitch + (float) dragY * DRAG_SPEED, MIN_PITCH, MAX_PITCH);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        zoom = Mth.clamp(zoom + (float) scrollY * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
