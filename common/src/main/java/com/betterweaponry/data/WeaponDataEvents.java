package com.betterweaponry.data;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.network.WeaponDataSyncPayload;
import com.betterweaponry.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** Branche le chargement des données sur le jeu : rechargement, synchronisation, messages d'erreur. */
public final class WeaponDataEvents {
    /** Au-delà, le chat serait illisible : le reste est dans le log. */
    private static final int MAX_PROBLEMS_IN_CHAT = 8;
    /** Niveau d'opérateur nécessaire pour voir les erreurs de config (celui des commandes de triche). */
    private static final int OPERATOR_LEVEL = 2;
    /** Joueurs déjà prévenus depuis le lancement du serveur. */
    private static final Set<UUID> NOTIFIED = new HashSet<>();

    private WeaponDataEvents() {
    }

    /**
     * Après le chargement des données : à la connexion d'un joueur et après chaque /reload.
     *
     * @param afterReload vrai pour un /reload, faux pour un joueur qui se connecte
     * @param players     les joueurs concernés
     */
    public static void onDatapackSync(boolean afterReload, Stream<ServerPlayer> players) {
        players.forEach(player -> {
            if (afterReload) Platform.get().sendToPlayer(player, new WeaponDataSyncPayload(WeaponData.current()));
            if (canSeeProblems(player)) sendProblems(player, WeaponDataLoader.lastProblems());
            if (!afterReload) sendCombatNotice(player);
        });
    }

    public static void syncToAll(MinecraftServer server) {
        WeaponDataSyncPayload payload = new WeaponDataSyncPayload(WeaponData.current());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Platform.get().sendToPlayer(player, payload);
            if (canSeeProblems(player)) sendProblems(player, WeaponDataLoader.lastProblems());
        }
    }

    /** Une seule fois par joueur et par lancement, désactivable dans compat.json : le combat de la 2.0 est maintenant dans Combat Reforged. */
    private static void sendCombatNotice(ServerPlayer player) {
        if (BwCompat.combatReforged() || !WeaponData.compat().combatReforgedNotice() || !NOTIFIED.add(player.getUUID())) return;
        player.sendSystemMessage(Component.translatable(BetterWeaponry.MODID + ".notice.combat_reforged").withStyle(ChatFormatting.GOLD));
    }

    /** Les opérateurs, et le joueur qui héberge une partie solo (même sans triches), voient les erreurs. */
    public static boolean canSeeProblems(ServerPlayer player) {
        return player.hasPermissions(OPERATOR_LEVEL) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static void sendProblems(ServerPlayer player, List<DataProblem> problems) {
        if (problems.isEmpty()) return;
        player.sendSystemMessage(Component.translatable("better_weaponry.data.problems.header", problems.size())
                .withStyle(ChatFormatting.GOLD));
        problems.stream().limit(MAX_PROBLEMS_IN_CHAT).forEach(problem -> player.sendSystemMessage(problem.toComponent()));
        if (problems.size() > MAX_PROBLEMS_IN_CHAT) {
            player.sendSystemMessage(Component.translatable("better_weaponry.data.problems.more",
                    problems.size() - MAX_PROBLEMS_IN_CHAT).withStyle(ChatFormatting.GRAY));
        }
    }
}
