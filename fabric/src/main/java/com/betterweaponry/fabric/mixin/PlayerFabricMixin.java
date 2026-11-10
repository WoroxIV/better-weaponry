package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.EnchantmentEvents;
import com.betterweaponry.platform.Platform;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ce que le jeu de base réserve au bouclier vanilla, et les dégâts infligés à un joueur. */
@Mixin(Player.class)
public abstract class PlayerFabricMixin {

    /** Les dégâts finaux reçus par un joueur, une fois l'armure et l'absorption comptées. */
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void betterweaponry$damageDealt(CombatTracker tracker, DamageSource source, float damage, Operation<Void> original) {
        original.call(tracker, source, damage);
        EnchantmentEvents.onDamageDealt((Player) (Object) this, source, damage);
    }

    /** Le jeu de base met toujours en recharge le bouclier vanilla : ici, celui qui est tenu. */
    @WrapOperation(method = "disableShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"))
    private void betterweaponry$disableHeldShield(ItemCooldowns cooldowns, Item item, int ticks, Operation<Void> original) {
        ItemStack used = ((Player) (Object) this).getUseItem();
        original.call(cooldowns, Platform.get().isShield(used) ? used.getItem() : item, ticks);
    }

    /** Le jeu de base n'use que le bouclier vanilla. */
    @WrapOperation(method = "hurtCurrentlyUsedShield", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean betterweaponry$wearAnyShield(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || Platform.get().isShield(stack);
    }
}
