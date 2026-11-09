package com.betterweaponry.platform;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Un coup qui arrive sur une créature, avant l'armure : chaque chargeur le remplit à sa façon. */
public interface IncomingDamage {

    LivingEntity target();

    DamageSource source();

    float amount();

    void setAmount(float amount);

    /** Le coup est annulé : ni dégâts, ni recul. */
    void cancel();

    boolean isCanceled();

    /** L'armure ne retire plus que cette part de ce qu'elle retirerait (0.7 = 30 % de pénétration). */
    void scaleArmorReduction(float kept);

    /** Durée de la protection de la cible après ce coup, en ticks. */
    void setPostAttackInvulnerabilityTicks(int ticks);
}
