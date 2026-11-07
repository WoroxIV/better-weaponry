package com.betterweaponry.command;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.Docs;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.editor.EditorService;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.network.OpenEditorPayload;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Commande /betterweaponry :
 * <ul>
 *   <li>{@code /betterweaponry} : l'éditeur pour les opérateurs (et l'hôte d'une partie solo), le Codex pour les autres ;</li>
 *   <li>{@code editor} / {@code codex} : l'un ou l'autre explicitement ;</li>
 *   <li>{@code dps} : dégâts par seconde de chaque arme, comparés à l'épée en fer vanilla ;</li>
 *   <li>{@code reload} : recharge seulement les données des armes (opérateurs).</li>
 * </ul>
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class BetterWeaponryCommand {
    private static final String KEY = BetterWeaponry.MODID + ".command.";
    private static final int OPERATOR_LEVEL = 2;

    private BetterWeaponryCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("betterweaponry")
                .executes(context -> open(context, null))
                .then(Commands.literal("editor").executes(context -> open(context, true)))
                .then(Commands.literal("codex").executes(context -> open(context, false)))
                .then(Commands.literal("dps").executes(BetterWeaponryCommand::dps))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .executes(BetterWeaponryCommand::reload)));
    }

    /** @param editor true = éditeur, false = Codex, null = selon les droits du joueur */
    private static int open(CommandContext<CommandSourceStack> context, @Nullable Boolean editor) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean canEdit = EditorService.canEdit(player);
        if (Boolean.TRUE.equals(editor) && !canEdit) {
            context.getSource().sendFailure(Component.translatable(KEY + "no_permission"));
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new OpenEditorPayload(editor == null ? canEdit : editor));
        return 1;
    }

    private static int dps(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable(KEY + "dps.header", Docs.number(WeaponDefinition.VANILLA_IRON_SWORD_DPS))
                .withStyle(ChatFormatting.GOLD), false);
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            double iron = definition.dps(WeaponMaterials.IRON);
            double netherite = definition.dps(WeaponMaterials.NETHERITE);
            Component line = Component.translatable(KEY + "dps.line",
                    Component.translatable(BetterWeaponry.MODID + ".configuration." + type.id()),
                    Docs.number(iron), Docs.number(netherite), Docs.number(iron / WeaponDefinition.VANILLA_IRON_SWORD_DPS));
            source.sendSuccess(() -> line, false);
        }
        return WeaponType.values().length;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        WeaponDataLoader.Result result = EditorService.reloadWeaponData(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "reloaded", result.problems().size()), true);
        return 1;
    }
}
