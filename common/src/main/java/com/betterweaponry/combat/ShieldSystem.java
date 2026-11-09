package com.betterweaponry.combat;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.projectile.Projectile;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.entity.ShurikenEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Parade parfaite au bouclier (celui du mod, le vanilla et ceux d'autres mods) : un coup reçu juste après la levée
 * du bouclier ne l'use pas, repousse l'attaquant au corps à corps, et renvoie les flèches vers leur tireur.
 * Le blocage lui-même reste celui du jeu.
 */
public final class ShieldSystem {
    /** En vanilla, une cible ignore les coups tant que son compteur d'invulnérabilité dépasse 10 ticks. */
    private static final int VANILLA_INVULNERABILITY_THRESHOLD = 10;

    private ShieldSystem() {
    }

    public static boolean isShield(ItemStack stack) {
        return Platform.get().isShield(stack);
    }

    /** Le joueur lève un bouclier : la fenêtre commence quand il devient efficace (5 ticks en vanilla). */
    public static void onStartUsingItem(LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player && isShield(stack)) openPerfectWindow(player);
    }

    private static void openPerfectWindow(Player player) {
        GearDefinition gear = WeaponData.gear();
        PlayerCombatState state = PlayerCombatState.of(player);
        long now = player.level().getGameTime();
        if (!WeaponData.rules().perfectParryActive() || gear.shieldPerfectWindow() <= 0.0D || now < state.perfectParryReadyAt) {
            state.perfectParryUntil = 0L;
            return;
        }
        state.perfectParryUntil = now + ShieldItem.EFFECTIVE_BLOCK_DELAY + AbilityCooldowns.ticks(gear.shieldPerfectWindow());
        state.perfectParryReadyAt = state.perfectParryUntil + AbilityCooldowns.ticks(gear.shieldPerfectCooldown());
    }

    /**
     * Coup arrêté par le bouclier (côté serveur).
     *
     * @return vrai si le bouclier ne doit pas s'user
     */
    public static boolean onShieldBlock(LivingEntity entity, DamageSource source, float damage) {
        if (!(entity instanceof Player player)) return false;
        if (player.invulnerableTime > VANILLA_INVULNERABILITY_THRESHOLD && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) return false;

        ItemStack shield = player.getUseItem();
        if (!WeaponData.rules().perfectParryActive()) {
            // Sans parade parfaite (coupée, ou mod de combat installé), Reflective marche sur tout coup bloqué.
            ModEnchantments.reflect(player, shield, source, damage);
            return false;
        }
        if (!ParrySystem.isInPerfectWindow(player)) return false;

        GearDefinition gear = WeaponData.gear();
        ParrySystem.onPerfectParry(player, shield, source, damage, gear.shieldPerfectDamageBonus(), gear.shieldPerfectBonusSeconds());
        LivingEntity attacker = ModEnchantments.meleeAttacker(source);
        if (attacker != null && gear.shieldPerfectKnockback() > 0.0D) ModEnchantments.pushBack(attacker, player, gear.shieldPerfectKnockback());
        return true;
    }

    /** Une flèche qui va toucher un joueur en parade parfaite repart vers son tireur. @return vrai si l'impact est annulé */
    public static boolean onProjectileImpact(Projectile projectile, HitResult result) {
        return projectile instanceof AbstractArrow arrow && !arrow.level().isClientSide
                && result instanceof EntityHitResult hit && hit.getEntity() instanceof Player player
                && tryReflectArrow(arrow, player);
    }

    /** @return vrai si la flèche a été renvoyée */
    public static boolean tryReflectArrow(AbstractArrow arrow, Player player) {
        GearDefinition gear = WeaponData.gear();
        // Changer leur propriétaire volerait un trident Loyauté, ou empêcherait le lanceur de récupérer son shuriken.
        if (arrow instanceof ThrownTrident || (arrow instanceof ShurikenEntity && arrow.pickup == AbstractArrow.Pickup.ALLOWED)) return false;
        if (!gear.reflectArrows() || !player.isBlocking() || !ParrySystem.isInPerfectWindow(player)
                || !player.isDamageSourceBlocked(player.damageSources().arrow(arrow, arrow.getOwner()))) return false;

        @Nullable Entity shooter = arrow.getOwner();
        double speed = arrow.getDeltaMovement().length();
        Vec3 direction = shooter != null && shooter.isAlive() ? shooter.getEyePosition().subtract(arrow.position()) : arrow.getDeltaMovement().reverse();
        arrow.setOwner(player);
        arrow.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        arrow.hasImpulse = true;
        ParrySystem.playPerfectParry(player, player.getUseItem());
        ParrySystem.grantPerfectBonus(player, gear.shieldPerfectDamageBonus(), gear.shieldPerfectBonusSeconds());
        return true;
    }
}
