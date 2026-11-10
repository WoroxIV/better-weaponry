package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.ShieldSystem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Une flèche qui va toucher un joueur en parade parfaite repart vers son tireur. */
@Mixin(Projectile.class)
public abstract class ProjectileFabricMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$reflect(HitResult result, CallbackInfo ci) {
        if (ShieldSystem.onProjectileImpact((Projectile) (Object) this, result)) ci.cancel();
    }
}
