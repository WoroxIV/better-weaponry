package com.betterweaponry.combat;

import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Règles du dual wield : une arme ambidextre dans chaque main (option "dual_wielded" de chaque arme).
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
     * Ce que le mod considère comme une arme de mêlée : le tag {@code better_weaponry:melee_weapons}
     * (épées, haches, trident et armes de mêlée des autres mods), modifiable par datapack.
     */
    public static boolean isMeleeWeapon(ItemStack stack) {
        return stack.is(ModTags.MELEE_WEAPONS);
    }

    /**
     * Cet objet peut être manié avec une autre arme dans l'autre main : option "dual_wielded" de l'arme,
     * ou tag {@code better_weaponry:dual_wieldable} pour un objet d'un autre mod.
     */
    public static boolean canDualWield(ItemStack stack) {
        if (WeaponItem.isTwoHanded(stack)) return false;
        return WeaponProfiles.definition(stack).map(WeaponDefinition::dualWielded).orElse(false) || stack.is(ModTags.DUAL_WIELDABLE);
    }

    /** Une arme ambidextre dans chaque main. Toujours faux si le dual wield est coupé (rules.json, Better Combat, Epic Fight). */
    public static boolean isDualWielding(LivingEntity player) {
        return WeaponData.rules().dualWieldActive()
                && canDualWield(player.getMainHandItem())
                && canDualWield(player.getOffhandItem());
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
