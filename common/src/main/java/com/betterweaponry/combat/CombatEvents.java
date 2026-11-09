package com.betterweaponry.combat;

import com.betterweaponry.platform.IncomingDamage;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.CombatCompat;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.network.ComboSyncPayload;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Règles de combat ajoutées par les stats des armes : pénétration d'armure, coup balayé,
 * temps d'invulnérabilité en dual wield. (Les effets à l'impact sont dans {@code WeaponItem.hurtEnemy},
 * les combos dans {@link ComboSystem}.)
 */
public final class CombatEvents {
    /** En vanilla, une cible ignore les coups tant que son compteur d'invulnérabilité dépasse 10 ticks. */
    private static final int VANILLA_INVULNERABILITY_THRESHOLD = 10;
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double PERCENT = 100.0D;

    private CombatEvents() {
    }

    public static void onChangedDimension(ServerPlayer player) {
        Platform.get().sendToPlayer(player, ComboSyncPayload.withHand(player, PlayerCombatState.of(player)));
    }

    /** Un coup arrive sur une créature, avant l'armure. Le bonus de parade parfaite passe avant ({@link ParrySystem#onHitWithPerfectBonus}). */
    public static void onIncomingDamage(IncomingDamage event) {
        ParrySystem.onIncomingDamage(event);
        if (event.isCanceled()) return;
        MobCombat.onMobHit(event);
        applyWeaponRules(event);
        EnchantmentEvents.onIncomingDamage(event);
    }

    private static void applyWeaponRules(IncomingDamage event) {
        // Coups directs au corps à corps uniquement (pas les flèches, ni les dégâts d'épines, d'explosion...).
        LivingEntity attacker = ModEnchantments.meleeAttacker(event.source());
        if (attacker == null) return;

        applyArmorPenetration(event, attacker);

        if (attacker instanceof Player player && DualWieldSystem.isDualWielding(player)) {
            int protectionTicks = (int) Math.round(WeaponData.rules().dualWieldInvulnerabilitySeconds() * TICKS_PER_SECOND);
            event.setPostAttackInvulnerabilityTicks(VANILLA_INVULNERABILITY_THRESHOLD + protectionTicks);
        }
    }

    /**
     * L'armure réduit moins les dégâts : avec 30 % de pénétration, elle ne retire que 70 % de ce qu'elle retirerait.
     * Pendant un coup de la main secondaire, l'attribut est déjà celui de l'arme secondaire ({@link HandSwap}).
     */
    private static void applyArmorPenetration(IncomingDamage event, LivingEntity attacker) {
        if (!WeaponData.rules().armorPenetrationActive() || event.source().is(DamageTypeTags.BYPASSES_ARMOR) || CombatCompat.isFakePlayer(attacker)) return;
        AttributeInstance attribute = attacker.getAttribute(ModAttributes.ARMOR_PENETRATION.get());
        double penetration = attribute == null ? 0.0D : attribute.getValue();
        if (penetration <= 0.0D) return;

        float kept = (float) (1.0D - Math.min(1.0D, penetration));
        event.scaleArmorReduction(kept);
    }

    /**
     * Le balayage vanilla est annulé pour les armes configurées sans coup balayé (estoc), et pendant un coup
     * spécial en zone d'un combo (la zone le remplace).
     */
    public static boolean preventsSweep(Player player) {
        boolean weaponProfile = WeaponProfiles.definition(player.getMainHandItem()).isPresent();
        boolean noSweep = WeaponData.rules().sweepActive()
                && WeaponProfiles.definition(player.getMainHandItem()).map(definition -> !definition.sweep().enabled()).orElse(false);
        return noSweep || ComboSystem.isAreaAttackInProgress(player) || (weaponProfile && CombatCompat.isFakePlayer(player));
    }

    /** Après l'armure : un coup au corps à corps entre joueurs ne retire jamais plus que pvp_max_hit_percent de la vie maximale. */
    public static float capPvpHit(LivingEntity victim, DamageSource source, float damage) {
        RulesDefinition rules = WeaponData.rules();
        if (!rules.pvpHitCapActive() || !(victim instanceof Player target)) return damage;
        if (!(ModEnchantments.meleeAttacker(source) instanceof Player attacker) || attacker == target || CombatCompat.isFakePlayer(attacker)) return damage;
        return Math.min(damage, (float) (target.getMaxHealth() * rules.pvpMaxHitPercent() / PERCENT));
    }
}
