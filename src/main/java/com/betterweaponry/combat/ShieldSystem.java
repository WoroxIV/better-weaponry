package com.betterweaponry.combat;

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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Parade parfaite au bouclier (celui du mod, le vanilla et ceux d'autres mods) : un coup reçu juste après la levée
 * du bouclier ne l'use pas, repousse l'attaquant au corps à corps, et renvoie les flèches vers leur tireur.
 * Le blocage lui-même reste celui du jeu.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class ShieldSystem {
    /** En vanilla, une cible ignore les coups tant que son compteur d'invulnérabilité dépasse 10 ticks. */
    private static final int VANILLA_INVULNERABILITY_THRESHOLD = 10;

    private ShieldSystem() {
    }

    public static boolean isShield(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    /** Le joueur lève un bouclier : la fenêtre commence quand il devient efficace (5 ticks en vanilla). */
    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && isShield(event.getItem())) openPerfectWindow(player);
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

    /** Coup arrêté par le bouclier (côté serveur). */
    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof Player player)) return;
        DamageSource source = event.getDamageSource();
        if (player.invulnerableTime > VANILLA_INVULNERABILITY_THRESHOLD && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) return;

        ItemStack shield = player.getUseItem();
        float damage = event.getOriginalBlockedDamage();
        if (!WeaponData.rules().perfectParryActive()) {
            // Sans parade parfaite (coupée, ou mod de combat installé), Reflective marche sur tout coup bloqué.
            ModEnchantments.reflect(player, shield, source, damage);
            return;
        }
        if (!ParrySystem.isInPerfectWindow(player)) return;

        GearDefinition gear = WeaponData.gear();
        event.setShieldDamage(0.0F);
        ParrySystem.onPerfectParry(player, shield, source, damage, gear.shieldPerfectDamageBonus(), gear.shieldPerfectBonusSeconds());
        LivingEntity attacker = ModEnchantments.meleeAttacker(source);
        if (attacker != null && gear.shieldPerfectKnockback() > 0.0D) ModEnchantments.pushBack(attacker, player, gear.shieldPerfectKnockback());
    }

    /** Une flèche qui va toucher un joueur en parade parfaite repart vers son tireur, au lieu d'être arrêtée. */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow && !arrow.level().isClientSide
                && event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof Player player
                && tryReflectArrow(arrow, player)) {
            event.setCanceled(true);
        }
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
