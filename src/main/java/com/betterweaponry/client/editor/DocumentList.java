package com.betterweaponry.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Liste de gauche de l'éditeur : règles globales, armes, préférences. Une étoile marque un fichier modifié. */
final class DocumentList extends ObjectSelectionList<DocumentList.Entry> {
    private static final int ITEM_HEIGHT = 14;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int INVALID_COLOR = 0xFF5555;

    private final Consumer<String> onSelect;
    private final Predicate<String> isDirty;
    private final Predicate<String> isInvalid;

    DocumentList(Minecraft minecraft, int width, int height, int y, Consumer<String> onSelect,
                 Predicate<String> isDirty, Predicate<String> isInvalid) {
        super(minecraft, width, height, y, ITEM_HEIGHT);
        this.onSelect = onSelect;
        this.isDirty = isDirty;
        this.isInvalid = isInvalid;
    }

    void add(String id, Component name) {
        addEntry(new Entry(id, name));
    }

    void select(String id) {
        for (Entry entry : children()) {
            if (entry.id.equals(id)) setSelected(entry);
        }
    }

    @Override
    public int getRowWidth() {
        return width - 12;
    }

    @Override
    protected int getScrollbarPosition() {
        return getX() + width - 6;
    }

    final class Entry extends ObjectSelectionList.Entry<Entry> {
        private final String id;
        private final Component name;

        Entry(String id, Component name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            String text = name.getString() + (isDirty.test(id) ? " *" : "");
            int color = isInvalid.test(id) ? INVALID_COLOR : TEXT_COLOR;
            graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(text, width - 4), left + 2, top + 2, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            setSelected(this);
            onSelect.accept(id);
            return true;
        }

        @Override
        public Component getNarration() {
            return name;
        }
    }
}
