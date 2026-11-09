package com.betterweaponry.mixin;

import com.betterweaponry.combat.ICombatMob;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.HandSwap;
import com.betterweaponry.combat.MobCombat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;

/** Combat des mobs : portée de leur arme et coups alternés en dual wield. */
@Mixin(Mob.class)
public abstract class MobMixin implements ICombatMob {
    @Unique private boolean betterweaponry$offhandTurn;
    @Unique private long betterweaponry$guardBrokenUntil;

    @Override
    public boolean betterweaponry$offhandTurn() {
        return betterweaponry$offhandTurn;
    }

    @Override
    public void betterweaponry$setOffhandTurn(boolean offhand) {
        betterweaponry$offhandTurn = offhand;
    }

    @Override
    public long betterweaponry$guardBrokenUntil() {
        return betterweaponry$guardBrokenUntil;
    }

    @Override
    public void betterweaponry$setGuardBrokenUntil(long gameTime) {
        betterweaponry$guardBrokenUntil = gameTime;
    }

    @ModifyReturnValue(method = "getAttackBoundingBox", at = @At("RETURN"))
    private AABB betterweaponry$weaponReach(AABB zone) {
        double extra = MobCombat.extraReach((Mob) (Object) this);
        return extra == 0.0D ? zone : zone.inflate(extra, 0.0D, extra);
    }

    /**
     * Une fois sur deux, c'est l'arme secondaire qui frappe : mains échangées le temps du coup, bonus compris.
     * Une arme du mod frappe comme une épée du même matériau ({@link MobCombat#hitLikeSword}).
     */
    @WrapMethod(method = "doHurtTarget")
    private boolean betterweaponry$alternateHands(Entity target, Operation<Boolean> original) {
        Mob mob = (Mob) (Object) this;
        BooleanSupplier attack = () -> MobCombat.hitLikeSword(mob, () -> original.call(target));
        if (!DualWieldSystem.isDualWielding(mob)) return attack.getAsBoolean();
        boolean offhand = MobCombat.isOffhandTurn(mob);
        MobCombat.setOffhandTurn(mob, !offhand);
        if (!offhand) return attack.getAsBoolean();
        HandSwap swap = HandSwap.begin(mob);
        try {
            return attack.getAsBoolean();
        } finally {
            swap.restore();
        }
    }
}
