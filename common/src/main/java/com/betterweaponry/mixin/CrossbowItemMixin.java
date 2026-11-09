package com.betterweaponry.mixin;

import com.betterweaponry.item.RangedWeapon;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Temps de chargement et vitesse de carreau des arbalètes du mod. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    /** L'or se charge plus vite. Sert aux joueurs, aux mobs et à l'animation. */
    @ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
    private static int betterweaponry$materialChargeTime(int ticks, ItemStack stack) {
        return stack.getItem() instanceof RangedWeapon weapon ? Math.max(1, (int) Math.round(ticks / weapon.drawSpeed())) : ticks;
    }

    @WrapOperation(method = "shootProjectile", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"))
    private static void betterweaponry$materialSpeed(Projectile projectile, double x, double y, double z, float velocity, float inaccuracy, Operation<Void> original,
                                                     @Local(argsOnly = true) LivingEntity shooter, @Local(argsOnly = true, ordinal = 0) ItemStack crossbow) {
        float speed = crossbow.getItem() instanceof RangedWeapon weapon ? weapon.velocityFor(shooter, velocity) : velocity;
        original.call(projectile, x, y, z, speed, inaccuracy);
    }
}
