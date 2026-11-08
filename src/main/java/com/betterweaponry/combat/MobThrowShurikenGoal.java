package com.betterweaponry.combat;

import com.betterweaponry.item.ShurikenItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Squelette ninja : s'approche à portée de sa cible, s'arrête et lance des shurikens tant qu'il la voit. */
public class MobThrowShurikenGoal extends Goal {
    private static final double RANGE_SQUARED = 225.0D;
    private static final int THROW_INTERVAL_TICKS = 30;
    private static final double APPROACH_SPEED = 1.0D;

    private final PathfinderMob mob;
    private int cooldown;

    public MobThrowShurikenGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean holdsShuriken() {
        return this.mob.getMainHandItem().getItem() instanceof ShurikenItem;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && holdsShuriken();
    }

    @Override
    public void start() {
        this.mob.setAggressive(true);
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        boolean inSight = this.mob.getSensing().hasLineOfSight(target);
        if (this.mob.distanceToSqr(target) > RANGE_SQUARED || !inSight) {
            this.mob.getNavigation().moveTo(target, APPROACH_SPEED);
            return;
        }
        this.mob.getNavigation().stop();
        if (--this.cooldown > 0) return;
        this.cooldown = THROW_INTERVAL_TICKS;
        if (this.mob.level() instanceof ServerLevel level && this.mob.getMainHandItem().getItem() instanceof ShurikenItem shuriken) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            shuriken.throwFromMob(level, this.mob, target, this.mob.getMainHandItem());
        }
    }
}
