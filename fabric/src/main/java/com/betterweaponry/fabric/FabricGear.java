package com.betterweaponry.fabric;

import com.betterweaponry.item.WeaponBowItem;
import com.betterweaponry.item.WeaponCrossbowItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Le jeu de base teste souvent "c'est l'arc vanilla" : les versions du mod doivent aussi répondre oui. */
public final class FabricGear {

    private FabricGear() {
    }

    public static boolean isModVersionOf(ItemStack stack, Item vanilla) {
        if (vanilla == Items.BOW) return stack.getItem() instanceof WeaponBowItem;
        return vanilla == Items.CROSSBOW && stack.getItem() instanceof WeaponCrossbowItem;
    }

    public static boolean holdsModVersionOf(LivingEntity entity, Item vanilla) {
        return isModVersionOf(entity.getMainHandItem(), vanilla) || isModVersionOf(entity.getOffhandItem(), vanilla);
    }
}
