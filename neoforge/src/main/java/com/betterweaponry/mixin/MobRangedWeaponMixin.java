package com.betterweaponry.mixin;

import com.betterweaponry.item.WeaponBowItem;
import com.betterweaponry.item.WeaponCrossbowItem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ces mobs ne savent tirer qu'avec l'arc ou l'arbalète vanilla : on leur apprend à utiliser ceux du mod. */
@Mixin({AbstractSkeleton.class, Pillager.class, Piglin.class})
public abstract class MobRangedWeaponMixin {

    @Inject(method = "canFireProjectileWeapon", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$canFireModWeapons(ProjectileWeaponItem weapon, CallbackInfoReturnable<Boolean> cir) {
        boolean archer = (Object) this instanceof AbstractSkeleton;
        if (archer ? weapon instanceof WeaponBowItem : weapon instanceof WeaponCrossbowItem) cir.setReturnValue(true);
    }
}
