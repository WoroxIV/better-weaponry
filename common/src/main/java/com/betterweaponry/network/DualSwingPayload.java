package com.betterweaponry.network;

import com.betterweaponry.combat.PlayerCombatState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Serveur → joueurs qui voient l'attaquant : il vient de porter un coup à deux armes. Leur client animera
 * les deux bras de l'attaquant au coup de bras suivant (le paquet vanilla d'animation n'en connaît qu'un).
 *
 * @param entityId identifiant de l'attaquant
 */
public record DualSwingPayload(int entityId) implements Payload {
    public static DualSwingPayload read(FriendlyByteBuf buffer) {
        return new DualSwingPayload(buffer.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
    }

    public static void handle(DualSwingPayload payload, Player receiver) {
        Entity entity = receiver.level().getEntity(payload.entityId());
        if (entity instanceof Player player) {
            PlayerCombatState.of(player).dualSwingPending = true;
        }
    }
}
