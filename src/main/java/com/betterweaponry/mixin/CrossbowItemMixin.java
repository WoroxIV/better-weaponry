package com.betterweaponry.mixin;

import com.betterweaponry.item.RangedWeapon;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Temps de chargement des arbalètes du mod (l'or se charge plus vite). Sert aux joueurs, aux mobs et à l'animation. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
    private static int betterweaponry$materialChargeTime(int ticks, ItemStack stack, LivingEntity shooter) {
        return stack.getItem() instanceof RangedWeapon weapon ? Math.max(1, (int) Math.round(ticks / weapon.drawSpeed())) : ticks;
    }
}
