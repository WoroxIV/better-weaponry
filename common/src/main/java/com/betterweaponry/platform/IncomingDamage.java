package com.betterweaponry.platform;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Un coup sur le point d'être encaissé, avant le bouclier et l'armure. */
public interface IncomingDamage {

    LivingEntity target();

    DamageSource source();

    float amount();

    void setAmount(float amount);
}
