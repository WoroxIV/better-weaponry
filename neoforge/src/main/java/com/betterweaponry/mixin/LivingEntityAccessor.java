package com.betterweaponry.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accès à la charge de l'attaque (protégée dans LivingEntity), pour les GameTests : frapper "chargé à fond". */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("attackStrengthTicker")
    void betterweaponry$setAttackStrengthTicker(int ticks);

    @Accessor("useItemRemaining")
    void betterweaponry$setUseItemRemaining(int ticks);
}
