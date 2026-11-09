package com.betterweaponry.mixin;

import com.betterweaponry.combat.DualWieldSystem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vue première personne : en vanilla l'arme descend en bas de l'écran pendant la recharge.
 * En dual wield on frappe en alternance, donc on garde les deux armes toujours levées.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F")
    )
    private float betterweaponry$keepWeaponsRaised(LocalPlayer player, float adjustTicks, Operation<Float> original) {
        return DualWieldSystem.isDualWielding(player) ? 1.0F : original.call(player, adjustTicks);
    }
}
