package com.betterweaponry.mixin;

import com.betterweaponry.combat.PlayerCombatState;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** À la réapparition, le jeu crée un nouveau joueur : les shurikens cassés le suivent, même après la mort. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void betterweaponry$keepBrokenShurikens(ServerPlayer previous, boolean keepEverything, CallbackInfo ci) {
        PlayerCombatState.of((ServerPlayer) (Object) this).brokenShurikens = PlayerCombatState.of(previous).brokenShurikens;
    }
}
