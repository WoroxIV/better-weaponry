package com.betterweaponry.client.editor;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.CombatHud;
import com.betterweaponry.client.ModKeyMappings;
import com.betterweaponry.client.tutorial.TutorialScreen;
import com.betterweaponry.compat.CombatCompat;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.config.ComboHudStyle;
import com.betterweaponry.config.ConfigFolder;
import com.betterweaponry.config.Docs;
import com.betterweaponry.config.EditorFiles;
import com.betterweaponry.config.HudElement;
import com.betterweaponry.config.TooltipCategory;
import com.betterweaponry.config.TooltipPlacement;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.DataField;
import com.betterweaponry.data.DataProblem;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.RulesPreset;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponComparison;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.editor.EditorDocument;
import com.betterweaponry.editor.EditorTarget;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.network.SaveDocumentPayload;
import com.betterweaponry.registry.ModItems;
import com.betterweaponry.tutorial.TutorialScenes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Éditeur des stats des armes et des règles, et Codex (même écran en lecture seule).
 * <ul>
 *   <li>en jeu : les valeurs sont celles du serveur ; "Enregistrer" les lui envoie (opérateurs et hôte d'une partie
 *       solo), pour tous les mondes (config/better_weaponry) ou pour ce monde seulement (datapack du monde) ;</li>
 *   <li>au menu principal (bouton "Config" de la liste des mods) : les fichiers de config/better_weaponry sont
 *       modifiés directement.</li>
 * </ul>
 * Les champs et leur aide viennent des déclarations des données ({@link FieldSet}) : un nouveau champ apparaît ici
 * tout seul.
 */
public final class WeaponEditorScreen extends Screen {
    static final String KEY = BetterWeaponry.MODID + ".editor.";
    private static final String CLIENT_PAGE = "client";
    private static final String TOOLTIPS_PAGE = "tooltips";
    private static final String COMPARISON_PAGE = "comparison";
    private static final String TUTORIALS_PAGE = "tutorials";
    private static final int PLACEMENT_BUTTON_WIDTH = 88;
    private static final int COLOR_BUTTON_WIDTH = 34;
    private static final int RESET_BUTTON_WIDTH = 20;
    private static final int PREVIEW_BUTTON_WIDTH = 100;
    private static final int STYLE_BUTTON_WIDTH = 100;
    private static final int OPERATOR_LEVEL = 2;
    private static final int MARGIN = 8;
    private static final int DOCUMENT_LIST_WIDTH = 112;
    private static final int TOP = 34;
    private static final int BOTTOM_SPACE = 54;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int MAX_BUTTON_WIDTH = 100;
    private static final int SUCCESS_COLOR = 0x55FF55;
    private static final int ERROR_COLOR = 0xFF5555;
    private static final int INFO_COLOR = 0xA0A0A0;
    private static final int WARNING_COLOR = 0xFFFF55;

    /** Où sont les données : chez le serveur (en jeu) ou dans les fichiers locaux (menu principal). */
    private enum Mode { IN_GAME, MAIN_MENU }

    @Nullable private final Screen parent;
    private final Mode mode;
    private final boolean canEdit;
    private final EditorModel model;
    private final Docs docs;
    /** Sections repliées ou dépliées par le joueur (sinon : règle par défaut, voir {@link #isCollapsed}). */
    private final Map<String, Boolean> sectionToggles = new HashMap<>();
    /** Fichiers envoyés au serveur et pas encore confirmés : id → JSON envoyé. */
    private final Map<String, String> pendingSaves = new HashMap<>();

    private String selected = EditorDocument.RULES_ID;
    /** Arme montrée dans l'aperçu de la page Tooltips. */
    private WeaponType previewType = WeaponType.KATANA;
    private WeaponComparison.Column comparisonColumn = WeaponComparison.Column.DPS;
    private boolean comparisonDescending = true;
    private WeaponMaterial comparisonMaterial = WeaponMaterials.IRON;
    private EditorTarget target = EditorTarget.ALL_WORLDS;
    private Component status = Component.empty();
    private int statusColor = INFO_COLOR;
    private boolean confirmQuit;
    private int cachedVersion = -1;
    private String cachedFor = "";
    private Component cachedInfo = Component.empty();

    private FieldList fieldList;

    private WeaponEditorScreen(@Nullable Screen parent, Mode mode, boolean canEdit) {
        super(Component.translatable(KEY + (canEdit ? "title" : "codex_title")));
        this.parent = parent;
        this.mode = mode;
        this.canEdit = canEdit;
        Minecraft minecraft = Minecraft.getInstance();
        this.docs = Docs.load(minecraft.options.languageCode);
        this.model = new EditorModel(mode == Mode.IN_GAME ? document -> document.json(WeaponData.current()) : WeaponEditorScreen::localValues);
        if (!canEdit) status(Component.translatable(KEY + "codex_note"), INFO_COLOR);
    }

    /** Ouvert par la commande /betterweaponry (le serveur a vérifié les droits). */
    public static WeaponEditorScreen inGame(@Nullable Screen parent, boolean canEdit) {
        return new WeaponEditorScreen(parent, Mode.IN_GAME, canEdit);
    }

    /** Ouvert par le bouton "Config" de la liste des mods, en jeu ou au menu principal. */
    public static Screen fromModList(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return new WeaponEditorScreen(parent, Mode.MAIN_MENU, true);
        boolean canEdit = minecraft.hasSingleplayerServer() || minecraft.player.hasPermissions(OPERATOR_LEVEL);
        return new WeaponEditorScreen(parent, Mode.IN_GAME, canEdit);
    }

    /** Menu principal : valeurs du jar + fichiers de config/better_weaponry (pas de monde, donc pas de datapack). */
    private static JsonObject localValues(EditorDocument document) {
        JsonObject merged = WeaponDataLoader.localJson(document.fields(), document.relativePath(), document.configFile(), new ArrayList<>());
        return JsonTree.expand(document.fields(), merged);
    }

    // --- Mise en page ---

    @Override
    protected void init() {
        int listHeight = height - TOP - BOTTOM_SPACE;
        DocumentList documentList = new DocumentList(minecraft, DOCUMENT_LIST_WIDTH, listHeight, TOP, this::select,
                id -> !isLocalPage(id) && model.isDirty(id), id -> !isLocalPage(id) && model.hasInvalid(id));
        documentList.setX(MARGIN);
        documentList.add(EditorDocument.RULES_ID, Component.translatable(KEY + "rules"));
        documentList.add(EditorDocument.GEAR_ID, Component.translatable(KEY + "gear"));
        documentList.add(COMPARISON_PAGE, Component.translatable(KEY + "comparison"));
        documentList.add(TUTORIALS_PAGE, Component.translatable(KEY + "tutorials"));
        for (EditorDocument document : model.documents()) {
            if (document.type() != null) {
                documentList.add(document.id(), Component.translatable(BetterWeaponry.MODID + ".configuration." + document.id()));
            }
        }
        documentList.add(TOOLTIPS_PAGE, Component.translatable(KEY + "tooltips"));
        documentList.add(CLIENT_PAGE, Component.translatable(KEY + "client"));
        documentList.select(selected);
        addRenderableWidget(documentList);

        int fieldLeft = MARGIN + DOCUMENT_LIST_WIDTH + MARGIN;
        fieldList = new FieldList(minecraft, this, width - fieldLeft - MARGIN, listHeight, TOP);
        fieldList.setX(fieldLeft);
        addRenderableWidget(fieldList);

        addButtons();
        rebuildFields(false);
    }

    private void addButtons() {
        List<Button> buttons = new ArrayList<>();
        if (canEdit) {
            buttons.add(button(KEY + "save", KEY + "save.tooltip", this::save));
            Button targetButton = Button.builder(targetLabel(), pressed -> {
                target = target == EditorTarget.ALL_WORLDS ? EditorTarget.THIS_WORLD : EditorTarget.ALL_WORLDS;
                pressed.setMessage(targetLabel());
            }).tooltip(Tooltip.create(Component.translatable(mode == Mode.IN_GAME ? KEY + "target.tooltip" : KEY + "target.main_menu"))).build();
            targetButton.active = mode == Mode.IN_GAME;
            buttons.add(targetButton);
        }
        buttons.add(button(KEY + "copy", KEY + "copy.tooltip", this::copyJson));
        if (canEdit) {
            buttons.add(button(KEY + "paste", KEY + "paste.tooltip", this::pasteJson));
            buttons.add(button(KEY + "reset", KEY + "reset.tooltip", this::resetDocument));
        }
        buttons.add(Button.builder(Component.translatable("gui.done"), pressed -> onClose()).build());

        int count = buttons.size();
        int buttonWidth = Math.min(MAX_BUTTON_WIDTH, (width - 2 * MARGIN - (count - 1) * BUTTON_GAP) / count);
        int x = (width - (count * buttonWidth + (count - 1) * BUTTON_GAP)) / 2;
        int y = height - BUTTON_HEIGHT - 6;
        for (Button button : buttons) {
            button.setRectangle(buttonWidth, BUTTON_HEIGHT, x, y);
            addRenderableWidget(button);
            x += buttonWidth + BUTTON_GAP;
        }
    }

    private Button button(String key, String tooltipKey, Runnable action) {
        return Button.builder(Component.translatable(key), pressed -> action.run())
                .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                .build();
    }

    private Component targetLabel() {
        return Component.translatable(KEY + (target == EditorTarget.ALL_WORLDS ? "target.all_worlds" : "target.this_world"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawString(font, font.plainSubstrByWidth(infoLine().getString(), fieldList.getWidth()),
                fieldList.getX(), TOP - 12, INFO_COLOR);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(status.getString(), width - 2 * MARGIN),
                width / 2, height - BOTTOM_SPACE + 10, statusColor);
    }

    // --- Contenu ---

    private void select(String id) {
        if (id.equals(selected)) return;
        selected = id;
        rebuildFields(true);
    }

    void toggleSection(String key) {
        sectionToggles.put(key, !isCollapsed(key));
        rebuildFields(false);
    }

    /**
     * Sections repliées par défaut quand elles ne servent pas à l'arme affichée : parade et dash selon "ability",
     * combo s'il est désactivé. Le joueur peut toujours les ouvrir.
     */
    private boolean isCollapsed(String key) {
        Boolean toggled = sectionToggles.get(key);
        if (toggled != null) return toggled;
        if (isLocalPage(selected) || model.document(selected).type() == null) return false;
        JsonObject values = model.working(selected);
        String ability = values.has(WeaponDefinition.ABILITY.name()) ? values.get(WeaponDefinition.ABILITY.name()).getAsString() : "";
        return switch (key) {
            case "parry" -> !WeaponAbility.PARRY.getSerializedName().equals(ability);
            case "dash" -> !WeaponAbility.DASH.getSerializedName().equals(ability);
            case "combo" -> !isTrue(FieldPath.ROOT.child("combo").child(ComboDefinition.ENABLED.name()).get(values));
            default -> false;
        };
    }

    private static boolean isTrue(@Nullable JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsBoolean();
    }

    private void rebuildFields(boolean resetScroll) {
        fieldList.clear(resetScroll);
        if (selected.equals(CLIENT_PAGE)) {
            addClientRows();
        } else if (selected.equals(TOOLTIPS_PAGE)) {
            addTooltipRows();
        } else if (selected.equals(COMPARISON_PAGE)) {
            addComparisonRows();
        } else if (selected.equals(TUTORIALS_PAGE)) {
            addTutorialRows();
        } else {
            CombatCompat.externalCombatModName().ifPresent(name -> fieldList.add(fieldList.new NoteRow(
                    Component.translatable(KEY + "external_combat_mod", name),
                    Component.translatable(KEY + "external_combat_mod.tooltip", name), WARNING_COLOR, 0)));
            if (selected.equals(EditorDocument.RULES_ID) && canEdit) addPresetRow();
            EditorDocument document = model.document(selected);
            String sectionTitle = document.type() == null ? KEY + "section.rules" : KEY + "section.stats";
            addSection("", Component.translatable(sectionTitle), null, 0);
            if (!isCollapsed("")) {
                for (DataField<?> field : FieldList.simpleFields(document.fields())) {
                    addValueRow(document.fields(), field, FieldPath.ROOT.child(field.name()), 1);
                }
            }
            for (DataField<?> field : document.fields().fields()) {
                if (field.children() != null) addComplexField(document.fields(), field, FieldPath.ROOT.child(field.name()), 0);
            }
        }
        fieldList.keepScrollInRange();
    }

    /** Objet (ex : "sweep") ou liste d'objets (ex : "on_hit_effects") : une section et ses sous-champs. */
    private void addComplexField(FieldSet set, DataField<?> field, FieldPath path, int depth) {
        String key = path.key();
        addSection(key, Component.literal(docs.fieldName(set, field)), tooltip(set, field), depth);
        if (isCollapsed(key)) return;

        FieldSet children = field.children();
        if (field.kind() == DataField.Kind.OBJECT) {
            for (DataField<?> child : children.fields()) {
                FieldPath childPath = path.child(child.name());
                if (child.children() != null) addComplexField(children, child, childPath, depth + 1);
                else addValueRow(children, child, childPath, depth + 1);
            }
            return;
        }

        JsonElement list = model.get(selected, path);
        int size = list != null && list.isJsonArray() ? list.getAsJsonArray().size() : 0;
        for (int i = 0; i < size; i++) {
            int index = i;
            Component title = Component.translatable(KEY + "item." + field.name(), i + 1);
            fieldList.add(fieldList.new ItemRow(title, i, size, canEdit,
                    () -> editList(path, array -> FieldList.swapped(array, index, -1)),
                    () -> editList(path, array -> FieldList.swapped(array, index, 1)),
                    () -> editList(path, array -> FieldList.without(array, index)),
                    depth + 1));
            for (DataField<?> child : children.fields()) {
                addValueRow(children, child, path.child(i).child(child.name()), depth + 2);
            }
        }
        if (canEdit) {
            fieldList.add(fieldList.new AddRow(Component.translatable(KEY + "add." + field.name()), size < field.max(),
                    () -> editList(path, array -> {
                        JsonArray copy = array.deepCopy();
                        copy.add(newElement(children));
                        return copy;
                    }), depth + 1));
        }
    }

    /** Nouvel élément de liste : valeurs par défaut, et le premier exemple pour un champ obligatoire (ex : l'effet). */
    private static JsonObject newElement(FieldSet children) {
        JsonObject element = JsonTree.expand(children, new JsonObject());
        for (DataField<?> child : children.fields()) {
            if (child.isRequired() && !child.examples().isEmpty()) element.addProperty(child.name(), child.examples().getFirst());
        }
        return element;
    }

    private void editList(FieldPath path, java.util.function.UnaryOperator<JsonArray> change) {
        JsonElement list = model.get(selected, path);
        JsonArray array = list != null && list.isJsonArray() ? list.getAsJsonArray() : new JsonArray();
        model.clearInvalidUnder(selected, path);
        model.set(selected, path, change.apply(array));
        rebuildFields(false);
    }

    private void addSection(String key, Component title, @Nullable Component tooltip, int depth) {
        fieldList.add(fieldList.new SectionRow(key, title, tooltip, isCollapsed(key), depth, tutorialAction(key)));
    }

    private void addValueRow(FieldSet set, DataField<?> field, FieldPath path, int depth) {
        fieldList.add(fieldList.new ValueRow(model, selected, path, field, Component.literal(docs.fieldName(set, field)),
                tooltip(set, field), canEdit, depth, tutorialAction(path.key())));
    }

    /** Ouvre le tutoriel lié à cette option, ou null si elle n'en a pas. */
    @Nullable
    private Runnable tutorialAction(String fieldKey) {
        String scene = TutorialScenes.sceneForField(fieldKey);
        return scene == null ? null : () -> minecraft.setScreen(new TutorialScreen(this, scene));
    }

    /** Page Tutoriels : tous les tutoriels, aussi accessibles par les boutons « ? » à côté des options. */
    private void addTutorialRows() {
        for (String id : TutorialScenes.IDS) {
            fieldList.add(fieldList.new ButtonsRow(TutorialScenes.title(id), null, List.of(Button.builder(Component.translatable(KEY + "tutorials.open"),
                    pressed -> minecraft.setScreen(new TutorialScreen(this, id))).size(PREVIEW_BUTTON_WIDTH, BUTTON_HEIGHT).build())));
        }
    }

    /** Aide d'un champ : description, puis plage et valeur par défaut. */
    private Component tooltip(FieldSet set, DataField<?> field) {
        MutableComponent text = Component.literal(docs.fieldName(set, field)).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("\n" + docs.fieldHelp(set, field)).withStyle(ChatFormatting.WHITE));
        String limits = docs.fieldLimits(field);
        if (!limits.isEmpty()) text.append(Component.literal("\n" + limits).withStyle(ChatFormatting.GRAY));
        return text;
    }

    /** Préréglages de la page Règles. Comme toute modification, ils ne sont appliqués qu'une fois enregistrés. */
    private void addPresetRow() {
        List<Button> presets = new ArrayList<>();
        for (RulesPreset preset : RulesPreset.values()) {
            presets.add(Button.builder(Component.translatable(KEY + "preset." + preset.id()), pressed -> applyPreset(preset))
                    .tooltip(Tooltip.create(Component.translatable(KEY + "preset." + preset.id() + ".tooltip"))).build());
        }
        fieldList.add(fieldList.new WideButtonsRow(presets));
    }

    private void applyPreset(RulesPreset preset) {
        JsonObject rules = model.working(EditorDocument.RULES_ID).deepCopy();
        preset.applyToRules(rules);
        model.replace(EditorDocument.RULES_ID, rules);
        JsonObject gear = model.working(EditorDocument.GEAR_ID).deepCopy();
        preset.applyToGear(gear);
        model.replace(EditorDocument.GEAR_ID, gear);
        rebuildFields(false);
        status(Component.translatable(KEY + "preset.applied", Component.translatable(KEY + "preset." + preset.id())), WARNING_COLOR);
    }

    /** Tableau comparatif : valeurs en cours de modification, donc un changement non enregistré s'y voit tout de suite. */
    private void addComparisonRows() {
        List<WeaponMaterial> materials = WeaponMaterials.active();
        ItemStack sample = new ItemStack(ModItems.get(WeaponType.KATANA, comparisonMaterial).get());
        fieldList.add(fieldList.new ItemPreviewRow(Component.translatable(KEY + "comparison.material", sample.getHoverName()), () -> sample,
                Button.builder(Component.translatable(KEY + "comparison.next_material"), pressed -> {
                    comparisonMaterial = materials.get((materials.indexOf(comparisonMaterial) + 1) % materials.size());
                    rebuildFields(false);
                }).size(PREVIEW_BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        GearDefinition gear = GearDefinition.CODEC.parse(JsonOps.INSTANCE, model.working(EditorDocument.GEAR_ID)).result().orElse(WeaponData.gear());
        fieldList.add(fieldList.new NoteRow(Component.translatable(KEY + "comparison.projectile_speed", Docs.number(gear.projectileSpeedMultiplier(comparisonMaterial))), 0));

        List<Button> header = new ArrayList<>();
        for (WeaponComparison.Column column : WeaponComparison.Column.values()) {
            String arrow = column != comparisonColumn ? "" : comparisonDescending ? " ▼" : " ▲";
            header.add(Button.builder(Component.translatable(KEY + "comparison.column." + column.id()).append(arrow), pressed -> sortComparison(column))
                    .tooltip(Tooltip.create(Component.translatable(KEY + "comparison.column." + column.id() + ".tooltip"))).build());
        }
        fieldList.add(fieldList.new TableHeaderRow(header));

        Map<WeaponType, WeaponDefinition> weapons = new EnumMap<>(WeaponType.class);
        for (EditorDocument document : model.documents()) {
            if (document.type() != null) {
                weapons.put(document.type(), WeaponDefinition.CODEC.parse(JsonOps.INSTANCE, model.working(document.id())).result().orElse(WeaponData.weapon(document.type())));
            }
        }
        int highlighted = comparisonColumn.ordinal() - 1;
        for (WeaponComparison.Row row : WeaponComparison.rows(weapons, comparisonMaterial, comparisonColumn, comparisonDescending)) {
            fieldList.add(fieldList.new TableRow(new ItemStack(ModItems.get(row.type(), comparisonMaterial).get()),
                    Component.translatable(BetterWeaponry.MODID + ".configuration." + row.type().id()),
                    List.of(Docs.number(row.damage()), Docs.number(row.speed()), Docs.number(row.dps()), row.pairDps() > 0.0D ? Docs.number(row.pairDps()) : "—",
                            Docs.number(row.reach()), Docs.number(row.penetration())), highlighted));
        }
    }

    /** Cliquer sur la colonne déjà triée inverse le sens. Les noms montent, les nombres descendent. */
    private void sortComparison(WeaponComparison.Column column) {
        comparisonDescending = column == comparisonColumn ? !comparisonDescending : column != WeaponComparison.Column.NAME;
        comparisonColumn = column;
        rebuildFields(false);
    }

    private void addClientRows() {
        fieldList.add(fieldList.new NoteRow(Component.translatable(KEY + "client.note"), 0));
        for (HudElement element : HudElement.values()) {
            addToggle(Component.translatable(KEY + "hud.element." + element.id()), Component.translatable(KEY + "hud.element." + element.id() + ".tooltip"),
                    ClientConfig.hud(element).show());
        }
        fieldList.add(fieldList.new ButtonsRow(Component.translatable(KEY + "hud.style"), Component.translatable(KEY + "hud.style.tooltip"),
                List.of(cycleButton(ClientConfig.COMBO_HUD_STYLE, List.of(ComboHudStyle.values()),
                        value -> Component.translatable(KEY + "hud.style." + value.id()), STYLE_BUTTON_WIDTH)),
                (graphics, x, y) -> CombatHud.drawSample(graphics, HudElement.COMBO, x, y)));
        addToggle(Component.translatable(KEY + "hud.attack_zones"), Component.translatable(KEY + "hud.attack_zones.tooltip"), ClientConfig.SHOW_ATTACK_ZONES);
        addToggle(Component.translatable(KEY + "hud.tutorial_hints"), Component.translatable(KEY + "hud.tutorial_hints.tooltip"), ClientConfig.TUTORIAL_HINTS);
        fieldList.add(fieldList.new WideButtonsRow(List.of(Button.builder(Component.translatable(KEY + "hud.open_layout"),
                pressed -> minecraft.setScreen(new HudLayoutScreen(this))).build())));
    }

    /** Page Tooltips : aperçu, préréglages, puis placement et couleur de chaque catégorie de lignes. */
    private void addTooltipRows() {
        fieldList.add(fieldList.new NoteRow(Component.translatable(KEY + "tooltips.note", ModKeyMappings.weaponDetailsKeyName()),
                Component.translatable(KEY + "tooltips.note.tooltip"), INFO_COLOR, 0));
        fieldList.add(fieldList.new ItemPreviewRow(Component.translatable(KEY + "tooltips.preview"),
                () -> new ItemStack(ModItems.get(previewType, WeaponMaterials.IRON).get()),
                Button.builder(Component.translatable(KEY + "tooltips.next_weapon"), pressed -> {
                    WeaponType[] types = WeaponType.values();
                    previewType = types[(previewType.ordinal() + 1) % types.length];
                }).size(PREVIEW_BUTTON_WIDTH, BUTTON_HEIGHT).build()));

        List<Button> presets = new ArrayList<>();
        for (ClientConfig.TooltipPreset preset : ClientConfig.TooltipPreset.values()) {
            presets.add(Button.builder(Component.translatable(KEY + "tooltips.preset." + preset.id()), pressed -> {
                ClientConfig.applyPreset(preset);
                rebuildFields(false);
            }).tooltip(Tooltip.create(Component.translatable(KEY + "tooltips.preset." + preset.id() + ".tooltip"))).build());
        }
        fieldList.add(fieldList.new WideButtonsRow(presets));

        for (TooltipCategory category : TooltipCategory.values()) {
            ModConfigSpec.EnumValue<TooltipPlacement> placement = ClientConfig.placement(category);
            ModConfigSpec.EnumValue<ChatFormatting> color = ClientConfig.color(category);
            Button reset = Button.builder(Component.literal("↺"), pressed -> {
                placement.set(category.defaultPlacement());
                color.set(category.defaultColor());
                ClientConfig.SPEC.save();
                rebuildFields(false);
            }).size(RESET_BUTTON_WIDTH, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable(KEY + "reset_field"))).build();
            fieldList.add(fieldList.new ButtonsRow(Component.translatable(KEY + "tooltips.category." + category.id()), null, List.of(
                    cycleButton(placement, List.of(TooltipPlacement.values()),
                            value -> Component.translatable(KEY + "tooltips.placement." + value.id()), PLACEMENT_BUTTON_WIDTH),
                    cycleButton(color, ClientConfig.TOOLTIP_COLORS,
                            value -> Component.literal("■■■").withStyle(value), COLOR_BUTTON_WIDTH),
                    reset)));
        }
    }

    /** Bouton qui fait défiler les valeurs d'une préférence (enregistrée tout de suite). */
    private <T extends Enum<T>> Button cycleButton(ModConfigSpec.EnumValue<T> value, List<T> values, Function<T, Component> label, int buttonWidth) {
        return Button.builder(label.apply(value.get()), pressed -> {
            T next = values.get((values.indexOf(value.get()) + 1) % values.size());
            value.set(next);
            ClientConfig.SPEC.save();
            pressed.setMessage(label.apply(next));
        }).size(buttonWidth, BUTTON_HEIGHT).build();
    }

    /** Pages des préférences du joueur, qui ne correspondent à aucun fichier d'arme. */
    private static boolean isLocalPage(String id) {
        return id.equals(CLIENT_PAGE) || id.equals(TOOLTIPS_PAGE) || id.equals(COMPARISON_PAGE) || id.equals(TUTORIALS_PAGE);
    }

    private void addToggle(Component label, Component tooltip, ModConfigSpec.BooleanValue value) {
        fieldList.add(fieldList.new ToggleRow(label, tooltip, value, ClientConfig.SPEC));
    }

    /** Ligne au-dessus des champs : DPS de l'arme affichée (comparé à l'épée en fer vanilla), ou une description. */
    private Component infoLine() {
        if (cachedVersion == model.version() && cachedFor.equals(selected)) return cachedInfo;
        cachedVersion = model.version();
        cachedFor = selected;
        if (selected.equals(TUTORIALS_PAGE)) {
            cachedInfo = Component.translatable(KEY + "info.tutorials");
        } else if (selected.equals(COMPARISON_PAGE)) {
            cachedInfo = Component.translatable(KEY + "info.comparison");
        } else if (isLocalPage(selected)) {
            cachedInfo = Component.translatable(KEY + "info.client");
        } else if (selected.equals(EditorDocument.RULES_ID)) {
            cachedInfo = Component.translatable(KEY + "info.rules");
        } else if (selected.equals(EditorDocument.GEAR_ID)) {
            cachedInfo = Component.translatable(KEY + "info.gear");
        } else {
            Optional<WeaponDefinition> definition = WeaponDefinition.CODEC.parse(JsonOps.INSTANCE, model.working(selected)).result();
            cachedInfo = definition.map(weapon -> {
                double dps = weapon.dps(WeaponMaterials.IRON);
                return (Component) Component.translatable(KEY + "info.dps", Docs.number(dps),
                        Docs.number(WeaponDefinition.VANILLA_IRON_SWORD_DPS), Docs.number(dps / WeaponDefinition.VANILLA_IRON_SWORD_DPS));
            }).orElse(Component.empty());
        }
        return cachedInfo;
    }

    // --- Boutons ---

    private void save() {
        if (model.anyInvalid()) {
            status(Component.translatable(KEY + "status.fix_invalid"), ERROR_COLOR);
            return;
        }
        List<String> dirty = model.dirtyIds();
        if (dirty.isEmpty()) {
            status(Component.translatable(KEY + "status.nothing_to_save"), INFO_COLOR);
            return;
        }
        confirmQuit = false;
        if (mode == Mode.MAIN_MENU) {
            saveLocally(dirty);
            return;
        }
        for (String id : dirty) {
            String json = model.working(id).toString();
            pendingSaves.put(id, json);
            PacketDistributor.sendToServer(new SaveDocumentPayload(id, target.ordinal(), json));
        }
        status(Component.translatable(KEY + "status.saving", dirty.size()), INFO_COLOR);
    }

    /** Menu principal : écrit directement les différences avec les valeurs du mod dans config/better_weaponry. */
    private void saveLocally(List<String> ids) {
        try {
            for (String id : ids) {
                EditorDocument document = model.document(id);
                JsonObject differences = JsonTree.diff(document.fields(), model.working(id), model.defaults(id));
                EditorFiles.writeGlobal(document, differences);
                model.markSaved(id);
            }
            status(Component.translatable(KEY + "status.saved_locally", ConfigFolder.displayPath(ConfigFolder.root())), SUCCESS_COLOR);
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("[Better Weaponry] The editor could not write the config folder", e);
            status(Component.translatable(KEY + "write_failed", e.getMessage()), ERROR_COLOR);
        }
    }

    /** Réponse du serveur : en cas de réussite, l'éditeur reprend les valeurs désormais en vigueur. */
    public void onSaveResult(String documentId, boolean success, Component message) {
        String sent = pendingSaves.remove(documentId);
        Optional<EditorDocument> document = EditorDocument.byId(documentId);
        if (success && document.isPresent()) {
            JsonObject effective = document.get().json(WeaponData.current());
            // Si le joueur n'a rien changé depuis l'envoi, on affiche les valeurs réellement appliquées.
            if (sent != null && model.working(documentId).toString().equals(sent)) model.replace(documentId, effective);
            model.markSaved(documentId, effective);
            if (documentId.equals(selected)) rebuildFields(false);
        }
        status(message, success ? SUCCESS_COLOR : ERROR_COLOR);
    }

    /** Copie les différences avec les valeurs du mod, prêtes à coller dans un fichier ou dans l'éditeur. */
    private void copyJson() {
        if (isLocalPage(selected)) return;
        EditorDocument document = model.document(selected);
        JsonObject differences = JsonTree.diff(document.fields(), model.working(selected), model.defaults(selected));
        minecraft.keyboardHandler.setClipboard(JsonUtil.PRETTY.toJson(differences));
        status(Component.translatable(KEY + "status.copied"), SUCCESS_COLOR);
    }

    /** Colle du JSON par-dessus les valeurs affichées : seuls les champs valides sont repris. */
    private void pasteJson() {
        if (isLocalPage(selected)) return;
        EditorDocument document = model.document(selected);
        JsonObject pasted;
        try {
            JsonElement element = JsonUtil.parseLenient(minecraft.keyboardHandler.getClipboard());
            if (!element.isJsonObject()) throw new JsonParseException("not an object");
            pasted = element.getAsJsonObject();
        } catch (JsonParseException e) {
            status(Component.translatable(KEY + "status.paste_invalid"), ERROR_COLOR);
            return;
        }
        List<DataProblem> problems = new ArrayList<>();
        JsonObject merged = document.fields().merge(List.of(
                new FieldSet.Layer(document.relativePath(), model.working(selected)),
                new FieldSet.Layer(Component.translatable(KEY + "clipboard").getString(), pasted)), problems);
        model.replace(selected, JsonTree.expand(document.fields(), merged));
        rebuildFields(false);
        if (problems.isEmpty()) {
            status(Component.translatable(KEY + "status.pasted"), SUCCESS_COLOR);
        } else {
            status(Component.translatable(KEY + "status.pasted_with_problems", problems.size(), problems.getFirst().toComponent()), WARNING_COLOR);
        }
    }

    private void resetDocument() {
        if (isLocalPage(selected)) return;
        model.resetToDefaults(selected);
        rebuildFields(false);
        status(Component.translatable(KEY + "status.reset"), INFO_COLOR);
    }

    /** Quitter avec des modifications non enregistrées demande un second clic. */
    @Override
    public void onClose() {
        if (canEdit && !model.dirtyIds().isEmpty() && !confirmQuit) {
            confirmQuit = true;
            status(Component.translatable(KEY + "status.unsaved"), WARNING_COLOR);
            return;
        }
        minecraft.setScreen(parent);
    }

    private void status(Component message, int color) {
        status = message;
        statusColor = color;
    }
}
