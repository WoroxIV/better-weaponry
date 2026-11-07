package com.betterweaponry.combat;

import com.betterweaponry.compat.BetterCombatCompat;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Règles du dual wield : une arme dans chaque main, aucune des deux à deux mains.
 * <ul>
 *   <li>les coups alternent entre les deux mains (LivingEntityMixin) ;</li>
 *   <li>un coup de la main secondaire utilise réellement l'arme secondaire ({@link HandSwap}) ;</li>
 *   <li>la recharge suit l'arme qui va frapper, accélérée par le multiplicateur des règles (rules.json).</li>
 * </ul>
 */
public final class DualWieldSystem {
    private static final double TICKS_PER_SECOND = 20.0D;

    private DualWieldSystem() {
    }

    /**
     * Ce que le mod considère comme une arme : le tag {@code better_weaponry:dual_wieldable}
     * (épées, haches, trident et armes de mêlée des autres mods), modifiable par datapack.
     */
    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ModTags.DUAL_WIELDABLE);
    }

    /** Toujours faux si Better Combat est installé : il a son propre système de dual wield. */
    public static boolean isDualWielding(Player player) {
        if (BetterCombatCompat.isLoaded()) return false;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return isWeapon(main) && isWeapon(off)
                && !WeaponItem.isTwoHanded(main)
                && !WeaponItem.isTwoHanded(off);
    }

    /**
     * Durée de recharge (en ticks) de la main qui va frapper, multiplicateur des règles (rules.json) inclus.
     * Pendant un échange de mains, les attributs reflètent déjà l'arme secondaire.
     */
    public static float attackStrengthDelay(Player player, PlayerCombatState state) {
        double speed = state.offhandTurn && state.activeSwap == null
                ? HandSwap.attackSpeedWithHandsSwapped(player)
                : player.getAttributeValue(Attributes.ATTACK_SPEED);
        return delayForSpeed(speed);
    }

    /** Recharge en ticks d'une arme de cette vitesse tenue en dual wield. */
    public static float delayForSpeed(double attackSpeed) {
        double multiplier = WeaponData.rules().dualWieldSpeedMultiplier();
        return (float) (TICKS_PER_SECOND / (attackSpeed * multiplier));
    }
}
