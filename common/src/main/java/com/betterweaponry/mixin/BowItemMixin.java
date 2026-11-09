package com.betterweaponry.mixin;

import com.betterweaponry.item.RangedWeapon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vitesse de flèche des arcs du mod : le jeu la calcule au milieu de {@code releaseUsing}. */
@Mixin(BowItem.class)
public abstract class BowItemMixin {

    @WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void betterweaponry$materialSpeed(AbstractArrow arrow, Entity shooter, float xRot, float yRot, float roll, float velocity, float inaccuracy, Operation<Void> original) {
        float speed = (Object) this instanceof RangedWeapon weapon && shooter instanceof LivingEntity living ? weapon.velocityFor(living, velocity) : velocity;
        original.call(arrow, shooter, xRot, yRot, roll, speed, inaccuracy);
    }
}
