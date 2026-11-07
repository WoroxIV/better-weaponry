package com.betterweaponry.combat;

import com.betterweaponry.compat.BetterCombatCompat;
import com.betterweaponry.config.ServerConfig;
import com.betterweaponry.item.WeaponItem;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * Règles du dual wield : une arme dans chaque main, aucune des deux à deux mains.
 * <ul>
 *   <li>les coups alternent entre les deux mains (LivingEntityMixin) ;</li>
 *   <li>un coup de la main secondaire utilise réellement l'arme secondaire ({@link HandSwap}) ;</li>
 *   <li>la recharge suit l'arme qui va frapper, accélérée par le multiplicateur de la config.</li>
 * </ul>
 */
public final class DualWieldSystem {
    private static final double TICKS_PER_SECOND = 20.0D;

    private DualWieldSystem() {
    }

    /** Ce que le mod considère comme une arme (vanilla ou d'autres mods inclus). */
    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof TridentItem;
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
     * Durée de recharge (en ticks) de la main qui va frapper, multiplicateur de la config inclus.
     * Pendant un échange de mains, les attributs reflètent déjà l'arme secondaire.
     */
    public static float attackStrengthDelay(Player player, IDualWieldPlayer state) {
        double speed = state.betterweaponry$isOffhandTurn() && state.betterweaponry$getActiveSwap() == null
                ? HandSwap.attackSpeedWithHandsSwapped(player)
                : player.getAttributeValue(Attributes.ATTACK_SPEED);
        double multiplier = ServerConfig.get(ServerConfig.DUAL_WIELD_SPEED_MULTIPLIER);
        return (float) (TICKS_PER_SECOND / (speed * multiplier));
    }
}
