package com.betterweaponry.mixin;

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
import org.spongepowered.asm.mixin.injection.At;

/** Combat des mobs : portée de leur arme et coups alternés en dual wield. */
@Mixin(Mob.class)
public abstract class MobMixin {

    @ModifyReturnValue(method = "getAttackBoundingBox", at = @At("RETURN"))
    private AABB betterweaponry$weaponReach(AABB zone) {
        double extra = MobCombat.extraReach((Mob) (Object) this);
        return extra == 0.0D ? zone : zone.inflate(extra, 0.0D, extra);
    }

    /** Une fois sur deux, c'est l'arme secondaire qui frappe : mains échangées le temps du coup, bonus compris. */
    @WrapMethod(method = "doHurtTarget")
    private boolean betterweaponry$alternateHands(Entity target, Operation<Boolean> original) {
        Mob mob = (Mob) (Object) this;
        if (!DualWieldSystem.isDualWielding(mob)) return original.call(target);
        boolean offhand = MobCombat.isOffhandTurn(mob);
        MobCombat.setOffhandTurn(mob, !offhand);
        if (!offhand) return original.call(target);
        HandSwap swap = HandSwap.begin(mob);
        try {
            return original.call(target);
        } finally {
            swap.restore();
        }
    }
}
