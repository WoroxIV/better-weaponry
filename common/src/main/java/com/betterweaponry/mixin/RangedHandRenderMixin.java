package com.betterweaponry.mixin;

import com.betterweaponry.item.WeaponBowItem;
import com.betterweaponry.item.WeaponCrossbowItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vue première personne : les arcs et arbalètes du mod s'animent comme les vanilla, que le jeu teste en dur. */
@Mixin(ItemInHandRenderer.class)
public abstract class RangedHandRenderMixin {

    /** Quelle main dessiner pendant un tir, et arbalète chargée tenue à deux mains. */
    @WrapOperation(method = {"evaluateWhichHandsToRender", "selectionUsingItemWhileHoldingBowLike", "isChargedCrossbow"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean betterweaponry$modBows(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item)
                || (item == Items.BOW && stack.getItem() instanceof WeaponBowItem)
                || (item == Items.CROSSBOW && stack.getItem() instanceof WeaponCrossbowItem);
    }

    /** Un arc plus rapide (or) doit aussi paraître se bander plus vite : on avance le temps vu par l'animation. */
    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getUseItemRemainingTicks()I"))
    private int betterweaponry$fastDrawAnimation(LocalPlayer player, Operation<Integer> original) {
        int remaining = original.call(player);
        if (!(player.getUseItem().getItem() instanceof WeaponBowItem bow) || bow.drawSpeed() == 1.0D) return remaining;
        int duration = player.getUseItem().getUseDuration();
        return duration - (int) Math.round((duration - remaining) * bow.drawSpeed());
    }
}
