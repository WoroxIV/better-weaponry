package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Optional;

/**
 * Parade comme en 1.8 : clic droit maintenu avec une arme dont la capacité est "parry".
 * <ul>
 *   <li>le joueur avance lentement (ralentissement vanilla d'un objet en cours d'utilisation) ;</li>
 *   <li>les coups reçus de face sont réduits, selon les mêmes règles qu'un bouclier ;</li>
 *   <li>chaque coup paré use l'arme et fait un bruit métallique.</li>
 * </ul>
 * Le démarrage est dans {@link WeaponItem#use}, la pose de l'arme dans {@code client.WeaponClientExtensions}.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class ParrySystem {
    /** Durée d'utilisation "sans fin", comme le bouclier : la parade dure tant que le clic droit est maintenu. */
    public static final int USE_DURATION = 72000;
    private static final double PERCENT = 100.0D;
    /** En vanilla, une cible ignore les coups tant que son compteur d'invulnérabilité dépasse 10 ticks. */
    private static final int VANILLA_INVULNERABILITY_THRESHOLD = 10;

    private ParrySystem() {
    }

    /** Réglages de parade de cet objet, s'il s'agit d'une arme qui pare et que la parade est active (rules.json, autres mods). */
    public static Optional<ParryDefinition> parryOf(ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem weapon) || !WeaponData.rules().parryActive()) return Optional.empty();
        WeaponDefinition definition = weapon.definition();
        return definition.ability() == WeaponAbility.PARRY ? Optional.of(definition.parry()) : Optional.empty();
    }

    /**
     * Vrai si un clic droit avec cet objet, dans cette main, lance une parade. Il faut :
     * <ul>
     *   <li>l'arme en main principale (seule la capacité de l'arme principale compte) ;</li>
     *   <li>la main secondaire vide, une arme à deux mains (la main secondaire est alors bloquée),
     *       ou une arme en main secondaire si l'option while_dual_wielding est active.</li>
     * </ul>
     * Un bouclier en main secondaire se lève donc à la place, et une torche ou de la nourriture gardent leur action.
     */
    public static boolean canParry(Player player, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        Optional<ParryDefinition> parry = parryOf(stack);
        if (parry.isEmpty()) return false;
        if (WeaponItem.isTwoHanded(stack)) return true;

        ItemStack offhand = player.getOffhandItem();
        return offhand.isEmpty() || (parry.get().whileDualWielding() && DualWieldSystem.isWeapon(offhand));
    }

    /** Vrai si le joueur pare en ce moment. Les conditions sont revérifiées (main secondaire changée entre-temps...). */
    public static boolean isParrying(Player player) {
        return player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && canParry(player, InteractionHand.MAIN_HAND, player.getMainHandItem());
    }

    /**
     * Vrai si le joueur pare avec une arme dans chaque main (option while_dual_wielding, ex : deux saïs) :
     * les deux armes prennent alors la pose de parade (première et troisième personne).
     */
    public static boolean isParryingWithBothHands(Player player) {
        ItemStack offhand = player.getOffhandItem();
        return isParrying(player)
                && !offhand.isEmpty()
                && DualWieldSystem.isWeapon(offhand)
                && !WeaponItem.isTwoHanded(player.getMainHandItem());
    }

    /**
     * Mêmes règles qu'un bouclier : le coup doit venir de devant (moitié avant, d'après la tête du joueur),
     * ne pas être une flèche perçante, ni un dégât qui ignore les boucliers. Les dégâts sans origine
     * (chute, feu, noyade, poison...) ne sont jamais parés.
     */
    public static boolean comesFromFront(LivingEntity target, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return false;
        Vec3 origin = source.getSourcePosition();
        if (origin == null) return false;

        Vec3 facing = Vec3.directionFromRotation(0.0F, target.getYHeadRot());
        Vec3 towardTarget = origin.vectorTo(target.position());
        towardTarget = new Vec3(towardTarget.x, 0.0D, towardTarget.z).normalize();
        return towardTarget.dot(facing) < 0.0D;
    }

    /** Durabilité perdue pour un coup paré : au moins 1, sauf si durability_per_damage vaut 0. */
    public static int durabilityCost(float blockedDamage, ParryDefinition parry) {
        if (parry.durabilityPerDamage() <= 0.0D || blockedDamage <= 0.0F) return 0;
        return Math.max(1, Math.round((float) (blockedDamage * parry.durabilityPerDamage())));
    }

    /** Coup reçu (côté serveur) : réduit avant l'armure, comme la parade 1.8. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isParrying(player)) return;
        float damage = event.getAmount();
        if (damage <= 0.0F || !comesFromFront(player, event.getSource())) return;

        ItemStack weapon = player.getMainHandItem();
        ParryDefinition parry = parryOf(weapon).orElseThrow();
        float blocked = (float) (damage * parry.damageReduction() / PERCENT);
        event.setAmount(damage - blocked);

        // Juste après un coup, le jeu ignore presque tout nouveau coup : pas d'usure ni de son pour un coup ignoré.
        if (player.invulnerableTime > VANILLA_INVULNERABILITY_THRESHOLD
                && !event.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN)) return;

        int cost = durabilityCost(blocked, parry);
        if (cost > 0) weapon.hurtAndBreak(cost, player, EquipmentSlot.MAINHAND);
        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.PARRY.get(), SoundSource.PLAYERS, 1.0F, pitch);
    }
}
