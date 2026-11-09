package com.betterweaponry.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Usure de l'équipement et effets d'enchantement d'un objet précis. */
public final class GearWear {
    /** Durée d'Aura de feu par niveau, comme un coup vanilla. */
    private static final int FIRE_SECONDS_PER_LEVEL = 4;

    private GearWear() {
    }

    /** Use l'objet ; s'il casse, le son et les particules sont ceux de cet emplacement. */
    public static void hurt(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        stack.hurtAndBreak(amount, entity, broken -> broken.broadcastBreakEvent(slot));
    }

    /** Effets d'enchantement de cet objet-là, pour un coup que le jeu de base ne gère pas (arme secondaire, zone, coup renvoyé). */
    public static void applyPostAttackEnchantments(LivingEntity attacker, LivingEntity victim, ItemStack weapon) {
        EnchantmentHelper.getEnchantments(weapon).forEach((enchantment, level) -> enchantment.doPostAttack(attacker, victim, level));
        int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon);
        if (fire > 0) victim.setSecondsOnFire(fire * FIRE_SECONDS_PER_LEVEL);
    }
}
