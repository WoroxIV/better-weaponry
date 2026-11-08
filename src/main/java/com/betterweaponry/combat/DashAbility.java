package com.betterweaponry.combat;

import com.betterweaponry.data.DashDefinition;
import com.betterweaponry.data.DashDirection;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModItems;
import com.betterweaponry.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Dash : un clic droit avec une arme dont la capacité est "dash" projette le joueur.
 * <ul>
 *   <li>Le mouvement est fait côté client (en vanilla, le client déplace lui-même son joueur, et lui seul connaît
 *       les touches de déplacement) ;</li>
 *   <li>le serveur décide du reste : recharge (icône vanilla dans la barre d'objets), faim, son et particules.</li>
 * </ul>
 * Comme pour la perle de l'Ender vanilla, la recharge est posée des deux côtés pour un retour immédiat.
 */
public final class DashAbility {
    /** Petit saut au départ d'un dash au sol : le joueur glisse un instant au lieu d'être freiné net par le sol. */
    static final double HOP = 0.2D;
    // Physique vanilla d'une entité qui bouge (LivingEntity.travel), sur un bloc normal.
    private static final double GROUND_DRAG = 0.6D * 0.91D;
    private static final double AIR_DRAG = 0.91D;
    private static final double GRAVITY = 0.08D;
    private static final double VERTICAL_DRAG = 0.98D;
    /** Distance parcourue sur sol plat pour une vitesse de départ de 1 bloc par tick (calculée une fois). */
    private static final double DISTANCE_PER_SPEED = simulateDistance(1.0D);
    private static final double TICKS_PER_SECOND = 20.0D;
    /** Comme le sprint : impossible à 3 pilons de faim ou moins (6 points). */
    private static final int MIN_FOOD_LEVEL = 6;
    /** Unité vanilla : 4 d'épuisement = 1 point de nourriture. */
    private static final float EXHAUSTION_PER_FOOD_POINT = 4.0F;

    private DashAbility() {
    }

    /** Réglages de dash de cet objet, s'il s'agit d'une arme qui fait un dash et que le dash est actif (rules.json, autres mods). */
    public static Optional<DashDefinition> dashOf(ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem weapon) || !WeaponData.rules().dashActive()) return Optional.empty();
        WeaponDefinition definition = weapon.definition();
        return definition.ability() == WeaponAbility.DASH ? Optional.of(definition.dash()) : Optional.empty();
    }

    /**
     * Vrai si un clic droit avec cet objet, dans cette main, lance un dash. Il faut :
     * <ul>
     *   <li>l'arme en main principale, sans recharge en cours ;</li>
     *   <li>la main secondaire vide ou tenant une arme (un bouclier se lève, une torche se pose...),
     *       sauf avec une arme à deux mains qui bloque la main secondaire ;</li>
     *   <li>ni à cheval, ni en vol en élytres, ni dans l'eau ou la lave ; au sol, sauf si allow_airborne ;</li>
     *   <li>assez de nourriture pour sprinter, si le dash coûte de la faim.</li>
     * </ul>
     */
    public static boolean canDash(Player player, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        Optional<DashDefinition> settings = dashOf(stack);
        if (settings.isEmpty() || player.getCooldowns().isOnCooldown(stack.getItem())) return false;
        DashDefinition dash = settings.get();

        ItemStack offhand = player.getOffhandItem();
        boolean offhandFree = WeaponItem.isTwoHanded(stack) || offhand.isEmpty() || DualWieldSystem.isWeapon(offhand);
        if (!offhandFree) return false;
        if (player.isPassenger() || player.isFallFlying() || player.isInWater() || player.isInLava()) return false;
        if (!player.onGround() && !dash.allowAirborne()) return false;
        return dash.hungerCost() <= 0.0D || hasEnoughFood(player);
    }

    /** Lance le dash (appelé des deux côtés par {@link WeaponItem#use}, après {@link #canDash}). */
    public static void dash(Level level, Player player, ItemStack stack, DashDefinition dash) {
        startCooldown(player, ((WeaponItem) stack.getItem()).getType(), dash);
        if (level.isClientSide) {
            launch(player, dash);
            return;
        }
        player.causeFoodExhaustion((float) dash.hungerCost() * EXHAUSTION_PER_FOOD_POINT);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DASH.get(), SoundSource.PLAYERS, 1.0F, pitch);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1D, player.getZ(), 6, 0.25D, 0.05D, 0.25D, 0.02D);
        }
    }

    /** Donne au joueur la vitesse du dash. Au sol, un petit saut évite que le frottement ne l'arrête aussitôt. */
    public static void launch(Player player, DashDefinition dash) {
        Vec3 direction = direction(player, dash.direction());
        double speed = initialSpeed(dash.distance());
        double vertical = player.onGround() ? HOP : Math.max(player.getDeltaMovement().y, 0.0D);
        player.setDeltaMovement(direction.x * speed, vertical, direction.z * speed);
        player.hasImpulse = true;
    }

    /**
     * Direction horizontale (longueur 1) du dash. En mode "input", on prend les touches de déplacement
     * (xxa = gauche/droite, zza = avant/arrière, remplis par le client) ; sans touche, vers l'avant.
     */
    public static Vec3 direction(Player player, DashDirection mode) {
        double strafe = mode == DashDirection.INPUT ? player.xxa : 0.0D;
        double forward = mode == DashDirection.INPUT ? player.zza : 0.0D;
        if (strafe * strafe + forward * forward < 1.0E-4D) {
            strafe = 0.0D;
            forward = 1.0D;
        }
        // Même rotation que Entity.getInputVector : touches relatives au regard → monde.
        double yaw = Math.toRadians(player.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new Vec3(strafe * cos - forward * sin, 0.0D, forward * cos + strafe * sin).normalize();
    }

    /** Vitesse de départ (blocs par tick) pour parcourir à peu près {@code distance} blocs sur sol plat. */
    public static double initialSpeed(double distance) {
        return distance / DISTANCE_PER_SPEED;
    }

    /** Recharge en ticks (au moins 1). */
    public static int cooldownTicks(DashDefinition dash) {
        return Math.max(1, (int) Math.round(dash.cooldownSeconds() * TICKS_PER_SECOND));
    }

    /** La recharge touche tous les matériaux du type : changer de dague ne permet pas de refaire un dash. */
    private static void startCooldown(Player player, WeaponType type, DashDefinition dash) {
        int ticks = cooldownTicks(dash);
        ModItems.allOfType(type).forEach(item -> player.getCooldowns().addCooldown(item.get(), ticks));
    }

    private static boolean hasEnoughFood(Player player) {
        return player.getFoodData().getFoodLevel() > MIN_FOOD_LEVEL || player.getAbilities().mayfly;
    }

    /**
     * Reproduit la physique vanilla d'un dash au sol : petit saut, freinage de l'air (×0.91 par tick),
     * puis du sol à l'atterrissage (×0.546). Le résultat est proportionnel à la vitesse de départ.
     */
    static double simulateDistance(double speed) {
        double distance = 0.0D;
        double horizontal = speed;
        double height = 0.0D;
        double vertical = HOP;
        for (int tick = 0; tick < 200 && horizontal > 1.0E-4D; tick++) {
            distance += horizontal;
            height += vertical;
            boolean onGround = height <= 0.0D;
            if (onGround) {
                height = 0.0D;
                vertical = 0.0D;
            }
            horizontal *= onGround ? GROUND_DRAG : AIR_DRAG;
            vertical = (vertical - GRAVITY) * VERTICAL_DRAG;
        }
        return distance;
    }
}
