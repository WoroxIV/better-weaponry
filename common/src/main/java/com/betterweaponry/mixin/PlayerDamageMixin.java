package com.betterweaponry.mixin;

import com.betterweaponry.combat.CombatEvents;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Après l'armure : plafond des dégâts entre joueurs. */
@Mixin(Player.class)
public abstract class PlayerDamageMixin {

    @ModifyExpressionValue(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float betterweaponry$capPvpHit(float damage, @Local(argsOnly = true) DamageSource source) {
        return CombatEvents.capPvpHit((Player) (Object) this, source, damage);
    }
}
