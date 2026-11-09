package com.betterweaponry.mixin;

import com.betterweaponry.item.ShurikenItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** À l'enclume, un shuriken accepte les enchantements des arcs et des arbalètes. */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$shurikens(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof ShurikenItem shuriken) cir.setReturnValue(shuriken.supportsEnchantment(stack, (Enchantment) (Object) this));
    }
}
