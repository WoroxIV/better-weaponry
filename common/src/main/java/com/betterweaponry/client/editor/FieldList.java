package com.betterweaponry.client.editor;

import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.data.DataField;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.data.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Liste des champs du fichier choisi, à droite de l'éditeur. Chaque ligne a son aide au survol du nom.
 * Nom en jaune : différent de la valeur par défaut du mod ; en rouge : saisie invalide (non enregistrable).
 */
final class FieldList extends ContainerObjectSelectionList<FieldList.Row> {
    static final int ROW_HEIGHT = 22;
    private static final int INDENT = 10;
    private static final int WIDGET_HEIGHT = 20;
    private static final int SMALL_BUTTON = 20;
    private static final int NUMBER_WIDTH = 70;
    private static final int TEXT_WIDTH = 140;
    private static final int TOOLTIP_WIDTH = 240;
    private static final int PREVIEW_WIDTH = 34;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int CHANGED_COLOR = 0xFFFF55;
    private static final int INVALID_COLOR = 0xFF5555;
    private static final int SECTION_COLOR = 0xFFAA00;
    private static final int NOTE_COLOR = 0xA0A0A0;
    private static final int TEXT_COLOR = 0xE0E0E0;

    private final WeaponEditorScreen screen;

    FieldList(Minecraft minecraft, WeaponEditorScreen screen, int width, int height, int y) {
        super(minecraft, width, height, y, ROW_HEIGHT);
        this.screen = screen;
    }

    @Override
    public int getRowWidth() {
        return width - 14;
    }

    @Override
    protected int getScrollbarPosition() {
        return getX() + width - 6;
    }

    void clear(boolean resetScroll) {
        double scroll = getScrollAmount();
        clearEntries();
        setScrollAmount(resetScroll ? 0.0D : scroll);
    }

    void add(Row row) {
        addEntry(row);
    }

    /** Remet le défilement dans les limites après un changement du nombre de lignes. */
    void keepScrollInRange() {
        setScrollAmount(getScrollAmount());
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Nom coupé avec "..." s'il dépasse la place disponible. */
    private static String fit(String text, int maxWidth) {
        Font font = font();
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    // --- Lignes ---

    /** Bouton « ? » qui ouvre le tutoriel d'une option, ou null si elle n'en a pas. */
    @Nullable
    private static Button helpButton(@Nullable Runnable openTutorial) {
        if (openTutorial == null) return null;
        return Button.builder(Component.literal("?"), pressed -> openTutorial.run())
                .size(SMALL_BUTTON, WIDGET_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(WeaponEditorScreen.KEY + "tutorial_button")))
                .build();
    }

    abstract class Row extends ContainerObjectSelectionList.Entry<Row> {
        protected final int depth;
        protected final List<AbstractWidget> widgets = new ArrayList<>();

        Row(int depth) {
            this.depth = depth;
        }

        int labelX(int left) {
            return left + 2 + depth * INDENT;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }

        void drawLabel(GuiGraphics graphics, String text, int x, int top, int maxWidth, int color) {
            graphics.drawString(font(), fit(text, maxWidth), x, top + 6, color);
        }

        void tooltipIfHovered(int mouseX, int mouseY, int x, int top, int labelWidth, @Nullable Component tooltip) {
            if (tooltip == null) return;
            if (mouseX >= x && mouseX < x + labelWidth && mouseY >= top && mouseY < top + ROW_HEIGHT - 2) {
                screen.setTooltipForNextRenderPass(font().split(tooltip, TOOLTIP_WIDTH));
            }
        }
    }

    /** Titre de section, cliquable pour la replier ou la déplier. */
    final class SectionRow extends Row {
        private final String key;
        private final Component title;
        @Nullable private final Component tooltip;
        private final boolean collapsed;
        @Nullable private final Button helpButton;

        SectionRow(String key, Component title, @Nullable Component tooltip, boolean collapsed, int depth, @Nullable Runnable openTutorial) {
            super(depth);
            this.helpButton = helpButton(openTutorial);
            if (helpButton != null) widgets.add(helpButton);
            this.key = key;
            this.title = title;
            this.tooltip = tooltip;
            this.collapsed = collapsed;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int x = labelX(left);
            String text = (collapsed ? "▶ " : "▼ ") + title.getString();
            int available = width - (x - left);
            if (helpButton != null) {
                helpButton.setPosition(left + width - 2 - SMALL_BUTTON, top);
                available -= SMALL_BUTTON + 4;
                helpButton.render(graphics, mouseX, mouseY, partialTick);
            }
            drawLabel(graphics, text, x, top, available, SECTION_COLOR);
            tooltipIfHovered(mouseX, mouseY, x, top, font().width(text), tooltip);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (helpButton != null && helpButton.mouseClicked(mouseX, mouseY, button)) return true;
            screen.toggleSection(key);
            return true;
        }
    }

    /** Texte d'information en gris. */
    final class NoteRow extends Row {
        private final Component text;
        private final Component tooltip;
        private final int color;

        NoteRow(Component text, int depth) {
            this(text, text, NOTE_COLOR, depth);
        }

        /** Note avec une explication plus longue au survol. */
        NoteRow(Component text, Component tooltip, int color, int depth) {
            super(depth);
            this.text = text;
            this.tooltip = tooltip;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int x = labelX(left);
            drawLabel(graphics, text.getString(), x, top, width - (x - left), color);
            tooltipIfHovered(mouseX, mouseY, x, top, width - (x - left), tooltip);
        }
    }

    /** Un champ : nom, zone de saisie (ou bouton) et bouton "valeur par défaut". */
    final class ValueRow extends Row {
        private final EditorModel model;
        private final String documentId;
        private final FieldPath path;
        private final DataField<?> field;
        private final Component label;
        private final Component tooltip;
        private final boolean editable;
        private final AbstractWidget editor;
        @Nullable private final Button resetButton;
        @Nullable private final Button helpButton;

        ValueRow(EditorModel model, String documentId, FieldPath path, DataField<?> field, Component label, Component tooltip,
                 boolean editable, int depth, @Nullable Runnable openTutorial) {
            super(depth);
            this.helpButton = helpButton(openTutorial);
            if (helpButton != null) widgets.add(helpButton);
            this.model = model;
            this.documentId = documentId;
            this.path = path;
            this.field = field;
            this.label = label;
            this.tooltip = tooltip;
            this.editable = editable;
            this.editor = createEditor();
            widgets.add(editor);
            if (editable) {
                resetButton = Button.builder(Component.literal("↺"), button -> resetToDefault())
                        .size(SMALL_BUTTON, WIDGET_HEIGHT)
                        .tooltip(Tooltip.create(Component.translatable(WeaponEditorScreen.KEY + "reset_field")))
                        .build();
                widgets.add(resetButton);
            } else {
                resetButton = null;
            }
        }

        private AbstractWidget createEditor() {
            return switch (field.kind()) {
                case BOOLEAN -> Button.builder(booleanLabel(), button -> {
                    model.set(documentId, path, new JsonPrimitive(!currentBoolean()));
                    button.setMessage(booleanLabel());
                }).size(NUMBER_WIDTH, WIDGET_HEIGHT).build();
                case CHOICE -> Button.builder(choiceLabel(), button -> {
                    List<String> values = field.examples();
                    int next = (values.indexOf(currentText()) + 1) % values.size();
                    model.set(documentId, path, new JsonPrimitive(values.get(next)));
                    button.setMessage(choiceLabel());
                }).size(NUMBER_WIDTH, WIDGET_HEIGHT).build();
                default -> createTextBox();
            };
        }

        private EditBox createTextBox() {
            boolean number = field.kind() == DataField.Kind.NUMBER || field.kind() == DataField.Kind.INTEGER;
            EditBox box = new EditBox(font(), 0, 0, number ? NUMBER_WIDTH : TEXT_WIDTH, WIDGET_HEIGHT, label);
            box.setMaxLength(1024);
            // Une saisie invalide survit à un changement de page : sinon la case afficherait une valeur correcte tout en bloquant Enregistrer.
            String invalidText = model.invalidText(documentId, path);
            box.setValue(invalidText != null ? invalidText : displayText());
            if (invalidText != null) box.setTextColor(INVALID_COLOR);
            box.setEditable(editable);
            box.setResponder(this::onTextChanged);
            return box;
        }

        private void onTextChanged(String text) {
            Optional<JsonElement> parsed = parse(text);
            boolean valid = parsed.isPresent() && field.validate(parsed.get()).isEmpty();
            if (valid) {
                model.set(documentId, path, parsed.get());
            } else {
                model.setInvalid(documentId, path, text);
            }
            ((EditBox) editor).setTextColor(valid ? TEXT_COLOR : INVALID_COLOR);
        }

        /** Texte saisi → valeur JSON du bon type (nombre avec point ou virgule, identifiant, objet). */
        private Optional<JsonElement> parse(String text) {
            String trimmed = text.trim();
            try {
                return switch (field.kind()) {
                    case NUMBER -> Optional.of(new JsonPrimitive(Double.parseDouble(trimmed.replace(',', '.'))));
                    case INTEGER -> {
                        double value = Double.parseDouble(trimmed.replace(',', '.'));
                        yield value == Math.rint(value) ? Optional.of(new JsonPrimitive((int) value)) : Optional.empty();
                    }
                    case ID -> trimmed.isEmpty() ? Optional.empty() : Optional.of(new JsonPrimitive(trimmed));
                    case NUMBER_MAP -> Optional.of(JsonUtil.parseLenient(trimmed.isEmpty() ? "{}" : trimmed));
                    case ITEM_LIST -> {
                        JsonArray entries = new JsonArray();
                        for (String entry : trimmed.split(",")) {
                            if (!entry.isBlank()) entries.add(entry.trim());
                        }
                        yield Optional.of(entries);
                    }
                    default -> Optional.empty();
                };
            } catch (NumberFormatException | JsonParseException e) {
                return Optional.empty();
            }
        }

        private String displayText() {
            JsonElement value = model.get(documentId, path);
            if (value == null) return "";
            if (field.kind() == DataField.Kind.ITEM_LIST && value.isJsonArray()) {
                List<String> entries = new ArrayList<>();
                value.getAsJsonArray().forEach(entry -> entries.add(entry.getAsString()));
                return String.join(", ", entries);
            }
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                // "50" et "1.6" plutôt que "50.0", sans arrondir.
                return new BigDecimal(value.getAsJsonPrimitive().getAsString()).stripTrailingZeros().toPlainString();
            }
            return value.isJsonPrimitive() ? value.getAsString() : value.toString();
        }

        private boolean currentBoolean() {
            JsonElement value = model.get(documentId, path);
            return value != null && value.isJsonPrimitive() && value.getAsBoolean();
        }

        private String currentText() {
            JsonElement value = model.get(documentId, path);
            return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
        }

        private Component booleanLabel() {
            return CommonComponents.optionStatus(currentBoolean());
        }

        private Component choiceLabel() {
            return Component.literal(currentText());
        }

        private boolean differsFromDefault() {
            JsonElement defaultValue = model.defaultAt(documentId, path, field);
            return defaultValue != null && !JsonTree.sameValue(model.get(documentId, path), defaultValue);
        }

        private void resetToDefault() {
            JsonElement defaultValue = model.defaultAt(documentId, path, field);
            if (defaultValue == null) return;
            model.set(documentId, path, defaultValue.deepCopy());
            refresh();
        }

        /** Met le widget à jour d'après la valeur du modèle. */
        private void refresh() {
            if (editor instanceof EditBox box) {
                box.setValue(displayText());
                box.setTextColor(TEXT_COLOR);
            } else if (editor instanceof Button button) {
                button.setMessage(field.kind() == DataField.Kind.BOOLEAN ? booleanLabel() : choiceLabel());
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int right = left + width - 2;
            if (resetButton != null) {
                resetButton.setPosition(right - SMALL_BUTTON, top);
                resetButton.active = differsFromDefault();
                right -= SMALL_BUTTON + 4;
            }
            editor.setPosition(right - editor.getWidth(), top);
            editor.active = editable;
            int controlsLeft = editor.getX();
            if (helpButton != null) {
                controlsLeft -= SMALL_BUTTON + 2;
                helpButton.setPosition(controlsLeft, top);
            }

            int x = labelX(left);
            int labelWidth = controlsLeft - x - 4;
            int color = model.isInvalid(documentId, path) ? INVALID_COLOR : differsFromDefault() ? CHANGED_COLOR : LABEL_COLOR;
            drawLabel(graphics, label.getString(), x, top, labelWidth, color);
            tooltipIfHovered(mouseX, mouseY, x, top, labelWidth, tooltip);
            for (AbstractWidget widget : widgets) widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** En-tête d'un élément de liste ("Coup 2") avec ses boutons monter / descendre / supprimer. */
    final class ItemRow extends Row {
        private final Component title;

        ItemRow(Component title, int index, int size, boolean editable, Runnable moveUp, Runnable moveDown, Runnable remove, int depth) {
            super(depth);
            this.title = title;
            if (editable) {
                widgets.add(smallButton("↑", WeaponEditorScreen.KEY + "move_up", moveUp, index > 0));
                widgets.add(smallButton("↓", WeaponEditorScreen.KEY + "move_down", moveDown, index < size - 1));
                widgets.add(smallButton("✕", WeaponEditorScreen.KEY + "remove", remove, true));
            }
        }

        private Button smallButton(String text, String tooltipKey, Runnable action, boolean active) {
            Button button = Button.builder(Component.literal(text), pressed -> action.run())
                    .size(SMALL_BUTTON, WIDGET_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                    .build();
            button.active = active;
            return button;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int right = left + width - 2;
            for (int i = widgets.size() - 1; i >= 0; i--) {
                AbstractWidget widget = widgets.get(i);
                widget.setPosition(right - SMALL_BUTTON, top);
                right -= SMALL_BUTTON + 2;
            }
            int x = labelX(left);
            drawLabel(graphics, title.getString(), x, top, right - x, LABEL_COLOR);
            for (AbstractWidget widget : widgets) widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Bouton "+ Ajouter..." en fin de liste. */
    final class AddRow extends Row {
        AddRow(Component label, boolean active, Runnable add, int depth) {
            super(depth);
            Button button = Button.builder(Component.literal("+ ").append(label), pressed -> add.run()).size(TEXT_WIDTH, WIDGET_HEIGHT).build();
            button.active = active;
            widgets.add(button);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            AbstractWidget button = widgets.getFirst();
            button.setPosition(labelX(left), top);
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Préférence d'affichage du joueur (config client), enregistrée tout de suite. */
    final class ToggleRow extends Row {
        private final Component label;
        private final Component tooltip;
        private final Button button;

        ToggleRow(Component label, Component tooltip, ClientConfig.Value<Boolean> value) {
            super(0);
            this.label = label;
            this.tooltip = tooltip;
            this.button = Button.builder(CommonComponents.optionStatus(value.get()), pressed -> {
                value.set(!value.get());
                ClientConfig.save();
                pressed.setMessage(CommonComponents.optionStatus(value.get()));
            }).size(NUMBER_WIDTH, WIDGET_HEIGHT).build();
            widgets.add(button);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int right = left + width - 2;
            button.setPosition(right - button.getWidth(), top);
            int x = labelX(left);
            int labelWidth = button.getX() - x - 4;
            drawLabel(graphics, label.getString(), x, top, labelWidth, LABEL_COLOR);
            tooltipIfHovered(mouseX, mouseY, x, top, labelWidth, tooltip);
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Un nom et des boutons alignés à droite (ex : placement et couleur d'une ligne de tooltip). */
    final class ButtonsRow extends Row {
        private final Component label;
        @Nullable private final Component tooltip;
        @Nullable private final RowPreview preview;

        ButtonsRow(Component label, @Nullable Component tooltip, List<? extends AbstractWidget> buttons) {
            this(label, tooltip, buttons, null);
        }

        /** @param preview petit aperçu dessiné à gauche des boutons */
        ButtonsRow(Component label, @Nullable Component tooltip, List<? extends AbstractWidget> buttons, @Nullable RowPreview preview) {
            super(0);
            this.label = label;
            this.tooltip = tooltip;
            this.preview = preview;
            widgets.addAll(buttons);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int right = left + width - 2;
            for (int i = widgets.size() - 1; i >= 0; i--) {
                AbstractWidget widget = widgets.get(i);
                widget.setPosition(right - widget.getWidth(), top);
                right -= widget.getWidth() + 2;
            }
            int x = labelX(left);
            if (preview != null) {
                preview.draw(graphics, right - PREVIEW_WIDTH / 2, top + WIDGET_HEIGHT / 2);
                right -= PREVIEW_WIDTH;
            }
            int labelWidth = right - x - 2;
            drawLabel(graphics, label.getString(), x, top, labelWidth, LABEL_COLOR);
            tooltipIfHovered(mouseX, mouseY, x, top, labelWidth, tooltip);
            for (AbstractWidget widget : widgets) widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Aperçu dessiné dans une ligne, centré sur ce point. */
    @FunctionalInterface
    interface RowPreview {
        void draw(GuiGraphics graphics, int centerX, int centerY);
    }

    /** Une ligne de boutons répartis sur toute la largeur. */
    final class WideButtonsRow extends Row {
        WideButtonsRow(List<? extends AbstractWidget> buttons) {
            super(0);
            widgets.addAll(buttons);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int count = widgets.size();
            int buttonWidth = (width - 2 - (count - 1) * 2) / count;
            for (int i = 0; i < count; i++) {
                AbstractWidget widget = widgets.get(i);
                widget.setWidth(buttonWidth);
                widget.setPosition(left + i * (buttonWidth + 2), top);
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    /** Largeurs des colonnes du tableau comparatif : l'icône et le nom prennent ce que les autres laissent. */
    private static final int TABLE_VALUE_WIDTH = 32;
    private static final int TABLE_ICON_SPACE = 20;

    private static int tableNameWidth(int width, int valueColumns) {
        return width - 2 - valueColumns * TABLE_VALUE_WIDTH;
    }

    /** En-tête du tableau comparatif : cliquer sur une colonne trie le tableau. */
    final class TableHeaderRow extends Row {
        TableHeaderRow(List<Button> columns) {
            super(0);
            widgets.addAll(columns);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int nameWidth = tableNameWidth(width, widgets.size() - 1);
            int x = left;
            for (int i = 0; i < widgets.size(); i++) {
                AbstractWidget widget = widgets.get(i);
                widget.setWidth(i == 0 ? nameWidth : TABLE_VALUE_WIDTH);
                widget.setPosition(x, top);
                widget.render(graphics, mouseX, mouseY, partialTick);
                x += widget.getWidth();
            }
        }
    }

    /** Ligne du tableau comparatif : icône, nom et valeurs alignées à droite. Survoler l'icône ou le nom montre le tooltip de l'arme. */
    final class TableRow extends Row {
        private final ItemStack stack;
        private final Component name;
        private final List<String> values;
        private final int highlighted;

        /** @param highlighted indice de la valeur de la colonne triée, ou -1 */
        TableRow(ItemStack stack, Component name, List<String> values, int highlighted) {
            super(0);
            this.stack = stack;
            this.name = name;
            this.values = values;
            this.highlighted = highlighted;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int nameWidth = tableNameWidth(width, values.size());
            graphics.renderItem(stack, left + 1, top + 1);
            drawLabel(graphics, name.getString(), left + TABLE_ICON_SPACE, top, nameWidth - TABLE_ICON_SPACE - 2, LABEL_COLOR);
            for (int i = 0; i < values.size(); i++) {
                int right = left + nameWidth + (i + 1) * TABLE_VALUE_WIDTH - 4;
                String value = values.get(i);
                graphics.drawString(font(), value, right - font().width(value), top + 5, i == highlighted ? CHANGED_COLOR : TEXT_COLOR);
            }
            if (mouseX >= left && mouseX < left + nameWidth && mouseY >= top && mouseY < top + ROW_HEIGHT - 2) {
                screen.setTooltipForNextRenderPass(Lists.transform(Screen.getTooltipFromItem(minecraft, stack), Component::getVisualOrderText));
            }
        }
    }

    /** Aperçu d'un objet : son tooltip réel s'affiche quand on le survole. */
    final class ItemPreviewRow extends Row {
        private static final int ICON_SIZE = 16;
        private final Component label;
        private final Supplier<ItemStack> stack;

        ItemPreviewRow(Component label, Supplier<ItemStack> stack, AbstractWidget button) {
            super(0);
            this.label = label;
            this.stack = stack;
            widgets.add(button);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            AbstractWidget button = widgets.getFirst();
            button.setPosition(left + width - 2 - button.getWidth(), top);
            int x = labelX(left);
            graphics.renderItem(stack.get(), x, top + 1);
            int textX = x + ICON_SIZE + 4;
            drawLabel(graphics, label.getString(), textX, top, button.getX() - textX - 4, LABEL_COLOR);
            if (mouseX >= x && mouseX < button.getX() - 4 && mouseY >= top && mouseY < top + ROW_HEIGHT - 2) {
                screen.setTooltipForNextRenderPass(Lists.transform(Screen.getTooltipFromItem(minecraft, stack.get()), Component::getVisualOrderText));
            }
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Copie d'une liste JSON sans l'élément {@code index}. */
    static JsonArray without(JsonArray array, int index) {
        JsonArray copy = new JsonArray();
        for (int i = 0; i < array.size(); i++) {
            if (i != index) copy.add(array.get(i).deepCopy());
        }
        return copy;
    }

    /** Copie d'une liste JSON avec les éléments {@code index} et {@code index + offset} échangés. */
    static JsonArray swapped(JsonArray array, int index, int offset) {
        JsonArray copy = array.deepCopy();
        int other = index + offset;
        if (other < 0 || other >= copy.size()) return copy;
        JsonElement moved = copy.get(index);
        copy.set(index, copy.get(other));
        copy.set(other, moved);
        return copy;
    }

    /** Champs simples (ni objet ni liste) d'un ensemble, sauf la version du format. */
    static List<DataField<?>> simpleFields(FieldSet set) {
        List<DataField<?>> fields = new ArrayList<>();
        for (DataField<?> field : set.fields()) {
            if (field.children() == null && !field.name().equals("format_version")) fields.add(field);
        }
        return fields;
    }
}
