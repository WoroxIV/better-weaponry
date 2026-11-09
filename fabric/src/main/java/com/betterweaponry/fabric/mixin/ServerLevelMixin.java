package com.betterweaponry.fabric.mixin;

import com.betterweaponry.event.VanillaGearEvents;
import com.betterweaponry.fabric.FabricEntityJoin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Une entité qui apparaît pour la première fois : elle reçoit son équipement avant d'entrer dans le monde. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void betterweaponry$freshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        VanillaGearEvents.onEntityJoin(entity, false);
        FabricEntityJoin.enterFreshEntity();
    }

    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void betterweaponry$freshEntityAdded(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        FabricEntityJoin.exitFreshEntity();
    }
}
