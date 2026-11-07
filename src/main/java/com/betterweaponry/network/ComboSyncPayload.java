package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.PlayerCombatState;
import com.betterweaponry.item.WeaponType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Serveur → joueur, après chacun de ses coups : où en est son combo. Le client suit déjà son combo tout seul
 * (pour l'animation et l'indicateur), ce paquet corrige seulement un éventuel désaccord (coup chargé à 89 %
 * côté client et 90 % côté serveur, par exemple).
 *
 * @param step      coup du combo qui va être porté
 * @param ticksLeft temps avant que le combo reparte du début
 * @param typeIndex type d'arme du combo (-1 = aucun combo)
 */
public record ComboSyncPayload(int step, int ticksLeft, int typeIndex) implements CustomPacketPayload {
    public static final Type<ComboSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "combo_sync"));

    public static final StreamCodec<ByteBuf, ComboSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ComboSyncPayload::step,
            ByteBufCodecs.VAR_INT, ComboSyncPayload::ticksLeft,
            ByteBufCodecs.VAR_INT, ComboSyncPayload::typeIndex,
            ComboSyncPayload::new);

    public static ComboSyncPayload of(Player player, PlayerCombatState state) {
        long ticksLeft = Math.max(0L, state.comboExpiresAt - player.level().getGameTime());
        int typeIndex = state.comboType == null ? -1 : state.comboType.ordinal();
        return new ComboSyncPayload(state.comboStep, (int) Math.min(Integer.MAX_VALUE, ticksLeft), typeIndex);
    }

    @Override
    public Type<ComboSyncPayload> type() {
        return TYPE;
    }

    /** Côté client : remplace l'état du combo du joueur par celui du serveur. */
    public static void handle(ComboSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            PlayerCombatState state = PlayerCombatState.of(player);
            WeaponType[] types = WeaponType.values();
            state.comboType = payload.typeIndex() >= 0 && payload.typeIndex() < types.length ? types[payload.typeIndex()] : null;
            state.comboStep = payload.step();
            state.comboExpiresAt = player.level().getGameTime() + payload.ticksLeft();
        });
    }
}
