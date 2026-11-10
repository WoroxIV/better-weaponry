package com.betterweaponry.command;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.Docs;
import com.betterweaponry.data.AbilityPenalty;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataEvents;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Commande /betterweaponry :
 * <ul>
 *   <li>{@code dps} : dégâts par seconde de chaque arme, comparés à l'épée en fer vanilla ;</li>
 *   <li>{@code reload} : recharge seulement les données des armes (opérateurs).</li>
 * </ul>
 */
public final class BetterWeaponryCommand {
    private static final String KEY = BetterWeaponry.MODID + ".command.";
    private static final int OPERATOR_LEVEL = 2;

    private BetterWeaponryCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("betterweaponry")
                .then(Commands.literal("dps").executes(BetterWeaponryCommand::dps))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .executes(BetterWeaponryCommand::reload)));
    }

    private static int dps(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable(KEY + "dps.header", Docs.number(WeaponDefinition.VANILLA_IRON_SWORD_DPS))
                .withStyle(ChatFormatting.GOLD), false);
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = WeaponData.weapon(type);
            double penalty = AbilityPenalty.of(type);
            double iron = definition.dps(WeaponMaterials.IRON, penalty);
            double netherite = definition.dps(WeaponMaterials.NETHERITE, penalty);
            Component line = Component.translatable(KEY + "dps.line",
                    Component.translatable(BetterWeaponry.MODID + ".configuration." + type.id()),
                    Docs.number(iron), Docs.number(netherite), Docs.number(iron / WeaponDefinition.VANILLA_IRON_SWORD_DPS));
            source.sendSuccess(() -> line, false);
        }
        return WeaponType.values().length;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        WeaponDataLoader.Result result = WeaponDataLoader.load(context.getSource().getServer().getResourceManager());
        WeaponDataLoader.install(result);
        WeaponDataEvents.syncToAll(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "reloaded", result.problems().size()), true);
        return 1;
    }
}
