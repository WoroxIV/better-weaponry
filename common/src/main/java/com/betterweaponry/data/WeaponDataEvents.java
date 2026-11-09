package com.betterweaponry.data;

import com.betterweaponry.platform.Platform;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.network.WeaponDataSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Stream;

/** Branche le chargement des données sur le jeu : rechargement, synchronisation, messages d'erreur. */
public final class WeaponDataEvents {
    /** Au-delà, le chat serait illisible : le reste est dans le log. */
    private static final int MAX_PROBLEMS_IN_CHAT = 8;
    /** Niveau d'opérateur nécessaire pour voir les erreurs de config (celui des commandes de triche). */
    private static final int OPERATOR_LEVEL = 2;

    private WeaponDataEvents() {
    }

    /**
     * Après le chargement des données : à la connexion d'un joueur et après chaque /reload.
     * À la connexion, les données ont déjà été envoyées pendant la configuration.
     *
     * @param afterReload vrai pour un /reload, faux pour un joueur qui se connecte
     * @param players     les joueurs concernés
     */
    public static void onDatapackSync(boolean afterReload, Stream<ServerPlayer> players) {
        players.forEach(player -> {
            if (afterReload) {
                Platform.get().sendToPlayer(player, new WeaponDataSyncPayload(WeaponData.current()));
                WeaponItem.refreshEquippedModifiers(player);
            }
            if (canSeeProblems(player)) {
                sendProblems(player, WeaponDataLoader.lastProblems());
            }
        });
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
