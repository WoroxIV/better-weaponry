package com.betterweaponry.combat;

import com.betterweaponry.data.WeaponData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Un mob qui a une arme de parade ou un bouclier lève parfois sa garde quand sa cible est proche. */
public class MobGuardGoal extends Goal {
    private static final double RANGE_SQUARED = 16.0D;
    private static final int ATTEMPT_INTERVAL_TICKS = 20;
    private static final int MIN_GUARD_TICKS = 20;
    private static final int EXTRA_GUARD_TICKS = 20;
    private static final int REST_TICKS = 60;
    private static final double PERCENT = 100.0D;

    private final PathfinderMob mob;
    private long nextAttempt;
    private int ticksLeft;

    public MobGuardGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        long now = this.mob.level().getGameTime();
        if (now < this.nextAttempt || target == null || !target.isAlive() || this.mob.isUsingItem()) return false;
        if (!WeaponData.rules().parryActive() || MobCombat.isGuardBroken(this.mob) || MobCombat.guardHand(this.mob) == null) return false;
        if (this.mob.distanceToSqr(target) > RANGE_SQUARED) return false;
        this.nextAttempt = now + ATTEMPT_INTERVAL_TICKS;
        return this.mob.getRandom().nextDouble() * PERCENT < WeaponData.gear().mobs().guardChance();
    }

    @Override
    public void start() {
        InteractionHand hand = MobCombat.guardHand(this.mob);
        if (hand == null) return;
        this.ticksLeft = MIN_GUARD_TICKS + this.mob.getRandom().nextInt(EXTRA_GUARD_TICKS + 1);
        this.mob.getNavigation().stop();
        this.mob.startUsingItem(hand);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return this.ticksLeft > 0 && this.mob.isUsingItem() && target != null && target.isAlive() && !MobCombat.isGuardBroken(this.mob);
    }

    @Override
    public void tick() {
        this.ticksLeft--;
        LivingEntity target = this.mob.getTarget();
        if (target != null) this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    @Override
    public void stop() {
        this.mob.stopUsingItem();
        this.nextAttempt = this.mob.level().getGameTime() + REST_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
