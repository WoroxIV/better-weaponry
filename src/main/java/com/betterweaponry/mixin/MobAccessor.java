package com.betterweaponry.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accès direct aux mains d'un mob, pour échanger ses armes sans événement d'équipement. */
@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("handItems")
    NonNullList<ItemStack> betterweaponry$getHandItems();

    @Accessor("handDropChances")
    float[] betterweaponry$getHandDropChances();
}
