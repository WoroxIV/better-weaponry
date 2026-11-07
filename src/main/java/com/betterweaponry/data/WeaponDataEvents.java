package com.betterweaponry.data;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.network.WeaponDataSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Branche le chargement des données sur le jeu : rechargement, synchronisation, messages d'erreur. */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class WeaponDataEvents {
    /** Au-delà, le chat serait illisible : le reste est dans le log. */
    private static final int MAX_PROBLEMS_IN_CHAT = 8;
    /** Niveau d'opérateur nécessaire pour voir les erreurs de config (celui des commandes de triche). */
    private static final int OPERATOR_LEVEL = 2;

    private WeaponDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new WeaponDataLoader());
    }

    /**
     * Appelé à la connexion d'un joueur (player non null) et après chaque /reload (player null).
     * À la connexion, les données ont déjà été envoyées pendant la configuration ({@code SyncWeaponDataTask}).
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        boolean afterReload = event.getPlayer() == null;
        event.getRelevantPlayers().forEach(player -> {
            if (afterReload) {
                PacketDistributor.sendToPlayer(player, new WeaponDataSyncPayload(WeaponData.current()));
            }
            if (canSeeProblems(player)) {
                sendProblems(player, WeaponDataLoader.lastProblems());
            }
        });
    }

    /** Un ancien réglage propre à un monde (serverconfig) n'est pas repris automatiquement : on le signale. */
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Path legacy = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(LegacyConfigMigration.LEGACY_FILE_NAME);
        if (Files.isRegularFile(legacy)) {
            BetterWeaponry.LOGGER.warn("[Better Weaponry] {} is a 1.x config specific to this world. It is no longer read: "
                    + "copy its changed values into a data pack (data/better_weaponry/better_weaponry/weapons/<weapon>.json).", legacy);
        }
    }

    /** Les opérateurs, et le joueur qui héberge une partie solo (même sans triches), voient les erreurs. */
    private static boolean canSeeProblems(ServerPlayer player) {
        return player.hasPermissions(OPERATOR_LEVEL) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    private static void sendProblems(ServerPlayer player, List<DataProblem> problems) {
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
