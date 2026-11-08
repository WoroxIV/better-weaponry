package com.betterweaponry.combat;

import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponShieldItem;
import com.betterweaponry.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Recharge d'une capacité (dash, garde brisée), affichée par l'icône vanilla dans la barre d'objets. */
public final class AbilityCooldowns {
    private static final double TICKS_PER_SECOND = 20.0D;

    private AbilityCooldowns() {
    }

    /** Durée en ticks (au moins 1). */
    public static int ticks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    /** Temps restant en secondes, d'après la part de recharge restante donnée par le jeu. */
    public static double remainingSeconds(Player player, Item item, float remainingPart) {
        return remainingPart * ((ICooldownDurations) player.getCooldowns()).betterweaponry$duration(item) / TICKS_PER_SECOND;
    }

    /** La recharge touche tous les matériaux du type : changer d'arme du même type ne permet pas d'y échapper. */
    public static void start(Player player, ItemStack stack, double seconds) {
        int ticks = ticks(seconds);
        if (stack.getItem() instanceof WeaponItem weapon) {
            ModItems.allOfType(weapon.getType()).forEach(item -> player.getCooldowns().addCooldown(item.get(), ticks));
        } else if (stack.getItem() instanceof WeaponShieldItem) {
            ModItems.allShields().forEach(item -> player.getCooldowns().addCooldown(item.get(), ticks));
        } else {
            player.getCooldowns().addCooldown(stack.getItem(), ticks);
        }
    }
}
