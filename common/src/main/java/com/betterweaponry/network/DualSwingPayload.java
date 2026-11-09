package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.PlayerCombatState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Serveur → joueurs qui voient l'attaquant : il vient de porter un coup à deux armes. Leur client animera
 * les deux bras de l'attaquant au coup de bras suivant (le paquet vanilla d'animation n'en connaît qu'un).
 *
 * @param entityId identifiant de l'attaquant
 */
public record DualSwingPayload(int entityId) implements CustomPacketPayload {
    public static final Type<DualSwingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "dual_swing"));

    public static final StreamCodec<ByteBuf, DualSwingPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DualSwingPayload::new, DualSwingPayload::entityId);

    @Override
    public Type<DualSwingPayload> type() {
        return TYPE;
    }

    public static void handle(DualSwingPayload payload, Player receiver) {
        Entity entity = receiver.level().getEntity(payload.entityId());
        if (entity instanceof Player player) {
            PlayerCombatState.of(player).dualSwingPending = true;
        }
    }
}
