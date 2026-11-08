package com.betterweaponry.mixin;

import com.betterweaponry.combat.ICooldownDurations;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/** Retient la durée totale des recharges, pour afficher le temps restant quelle que soit l'origine de la recharge. */
@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin implements ICooldownDurations {
    @Unique private final Map<Item, Integer> betterweaponry$durations = new HashMap<>();

    @Inject(method = "addCooldown", at = @At("HEAD"))
    private void betterweaponry$rememberDuration(Item item, int ticks, CallbackInfo ci) {
        betterweaponry$durations.put(item, ticks);
    }

    @Override
    public int betterweaponry$duration(Item item) {
        return betterweaponry$durations.getOrDefault(item, 0);
    }
}
