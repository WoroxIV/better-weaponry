package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.ShieldSystem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Une flèche qui va toucher un joueur en parade parfaite repart vers son tireur. */
@Mixin(Projectile.class)
public abstract class ProjectileFabricMixin {

    @Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$reflect(HitResult result, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (ShieldSystem.onProjectileImpact((Projectile) (Object) this, result)) cir.setReturnValue(ProjectileDeflection.NONE);
    }
}
