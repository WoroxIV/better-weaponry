package com.betterweaponry.fabric.mixin;

import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.platform.Platform;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Le serveur refuse un coup au-delà de 6 blocs, alors qu'une arme peut porter plus loin. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    /** Marge pour le décalage entre client et serveur, la même que Forge. */
    @Unique private static final double REACH_MARGIN = 3.0D;

    @Shadow public ServerPlayer player;

    @ModifyExpressionValue(method = "handleInteract", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;MAX_INTERACTION_DISTANCE:D"))
    private double betterweaponry$weaponReach(double maxDistanceSqr) {
        if (!BwCompat.reachActive()) return maxDistanceSqr;
        double reach = Platform.get().entityReach(player) + REACH_MARGIN;
        return Math.max(maxDistanceSqr, reach * reach);
    }
}
