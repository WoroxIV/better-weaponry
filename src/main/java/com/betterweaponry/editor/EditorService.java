package com.betterweaponry.editor;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.EditorFiles;
import com.betterweaponry.data.DataProblem;
import com.betterweaponry.data.FieldSet;
import com.betterweaponry.data.JsonTree;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataEvents;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.network.EditorResultPayload;
import com.betterweaponry.network.WeaponDataSyncPayload;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Côté serveur de l'éditeur : enregistre un fichier modifié, recharge les données des armes et prévient les joueurs.
 * <ul>
 *   <li>droits : opérateurs (niveau 2) et joueur qui héberge une partie solo ;</li>
 *   <li>seules les différences avec les couches inférieures sont écrites ({@link JsonTree#diff}) ;</li>
 *   <li>seules les données des armes sont rechargées, pas tout le /reload (sauf la première fois qu'on enregistre
 *       pour "ce monde seulement" : le datapack de l'éditeur doit alors être activé).</li>
 * </ul>
 */
public final class EditorService {
    private static final int OPERATOR_LEVEL = 2;
    private static final int MAX_OVERRIDDEN_SHOWN = 3;
    private static final String KEY = BetterWeaponry.MODID + ".editor.";

    private EditorService() {
    }

    /** Résultat d'un enregistrement, montré dans l'éditeur. */
    public record Outcome(boolean success, Component message) {
    }

    public static boolean canEdit(ServerPlayer player) {
        return player.hasPermissions(OPERATOR_LEVEL) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    /** Paquet reçu d'un joueur : vérifie ses droits, enregistre, puis lui répond. */
    public static void handleSave(ServerPlayer player, String documentId, EditorTarget target, String json) {
        if (!canEdit(player)) {
            reply(player, documentId, new Outcome(false, Component.translatable(KEY + "no_permission")));
            return;
        }
        save(player.server, documentId, target, json, outcome -> reply(player, documentId, outcome));
    }

    /**
     * Enregistre un fichier modifié dans l'éditeur. Le résultat est donné à {@code done} sur le thread du serveur
     * (tout de suite, ou après l'activation du datapack de l'éditeur).
     *
     * @param json valeurs complètes du fichier telles qu'affichées dans l'éditeur
     */
    public static void save(MinecraftServer server, String documentId, EditorTarget target, String json, Consumer<Outcome> done) {
        Optional<EditorDocument> found = EditorDocument.byId(documentId);
        if (found.isEmpty()) {
            done.accept(failure("unknown_document", documentId));
            return;
        }
        EditorDocument document = found.get();
        Optional<JsonObject> edited = parse(json);
        if (edited.isEmpty()) {
            done.accept(failure("unreadable", documentId));
            return;
        }

        // Mêmes vérifications qu'au chargement des fichiers : rien d'invalide n'est écrit.
        List<DataProblem> problems = new ArrayList<>();
        JsonObject valid = document.fields().merge(List.of(new FieldSet.Layer(document.relativePath(), edited.get())), problems);
        if (!problems.isEmpty()) {
            done.accept(new Outcome(false, Component.translatable(KEY + "invalid", problems.getFirst().toComponent())));
            return;
        }
        JsonObject requested = JsonTree.expand(document.fields(), valid);

        WeaponDataLoader.Base base = target == EditorTarget.ALL_WORLDS ? WeaponDataLoader.Base.MOD_DEFAULTS : WeaponDataLoader.Base.BELOW_EDITOR_PACK;
        JsonObject below = JsonTree.expand(document.fields(), WeaponDataLoader.mergedJson(server.getResourceManager(),
                document.fields(), document.relativePath(), document.configFile(), base));
        JsonObject differences = JsonTree.diff(document.fields(), requested, below);

        Path file;
        try {
            file = target == EditorTarget.ALL_WORLDS
                    ? EditorFiles.writeGlobal(document, differences)
                    : EditorFiles.writeWorld(server.getWorldPath(LevelResource.ROOT), document, differences);
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("[Better Weaponry] The editor could not write {}", document.relativePath(), e);
            done.accept(failure("write_failed", e.getMessage()));
            return;
        }

        Runnable finish = () -> done.accept(outcomeAfterReload(document, requested, file));
        if (target == EditorTarget.THIS_WORLD && !server.getPackRepository().getSelectedIds().contains(WeaponDataLoader.EDITOR_PACK_ID)) {
            // Premier réglage propre à ce monde : le datapack vient d'être créé, il faut l'activer (rechargement complet,
            // qui envoie aussi les nouvelles données aux joueurs).
            enableEditorPack(server).whenComplete((ignored, error) -> server.execute(finish));
        } else {
            reloadWeaponData(server);
            finish.run();
        }
    }

    /** Recharge seulement les données des armes et les envoie à tous les joueurs (problèmes aux opérateurs). */
    public static WeaponDataLoader.Result reloadWeaponData(MinecraftServer server) {
        WeaponDataLoader.Result result = WeaponDataLoader.load(server.getResourceManager());
        WeaponDataLoader.install(result);
        WeaponDataSyncPayload payload = new WeaponDataSyncPayload(WeaponData.current());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
            WeaponItem.refreshEquippedModifiers(player);
            if (WeaponDataEvents.canSeeProblems(player)) WeaponDataEvents.sendProblems(player, result.problems());
        }
        return result;
    }

    /**
     * Message de réussite, avec les champs qui n'ont pas pris la valeur demandée (remplacés par une couche plus
     * prioritaire, ex : un datapack du monde pour un réglage "tous les mondes").
     */
    private static Outcome outcomeAfterReload(EditorDocument document, JsonObject requested, Path file) {
        JsonObject effective = document.json(WeaponData.current());
        List<String> overridden = JsonTree.paths(JsonTree.diff(document.fields(), requested, effective));
        Component saved = Component.translatable(KEY + "saved", EditorFiles.displayPath(file));
        if (overridden.isEmpty()) return new Outcome(true, saved);
        String shown = String.join(", ", overridden.subList(0, Math.min(MAX_OVERRIDDEN_SHOWN, overridden.size())));
        return new Outcome(true, saved.copy().append(" ").append(Component.translatable(KEY + "overridden", shown)));
    }

    private static CompletableFuture<Void> enableEditorPack(MinecraftServer server) {
        PackRepository repository = server.getPackRepository();
        repository.reload();
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        if (!selected.contains(WeaponDataLoader.EDITOR_PACK_ID)) selected.add(WeaponDataLoader.EDITOR_PACK_ID);
        return server.reloadResources(selected);
    }

    private static Optional<JsonObject> parse(String json) {
        try {
            JsonElement element = JsonUtil.parseLenient(json);
            return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
        } catch (JsonParseException e) {
            return Optional.empty();
        }
    }

    private static Outcome failure(String reason, String detail) {
        return new Outcome(false, Component.translatable(KEY + reason, detail));
    }

    private static void reply(ServerPlayer player, String documentId, Outcome outcome) {
        PacketDistributor.sendToPlayer(player, new EditorResultPayload(documentId, outcome.success(), outcome.message()));
    }
}
