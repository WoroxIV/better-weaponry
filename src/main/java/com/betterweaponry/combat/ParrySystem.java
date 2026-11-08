package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Optional;

/**
 * Parade comme en 1.8 : clic droit maintenu avec une arme dont la capacité est "parry".
 * <ul>
 *   <li>le joueur avance lentement (ralentissement vanilla d'un objet en cours d'utilisation) ;</li>
 *   <li>les coups reçus de face sont réduits, selon les mêmes règles qu'un bouclier ;</li>
 *   <li>chaque coup paré use l'arme et fait un bruit métallique ;</li>
 *   <li>au tout début de la parade, un coup est bloqué en entier (parade parfaite) ;</li>
 *   <li>une arme qui désactive les boucliers brise la garde : parade impossible pendant un moment.</li>
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
    private static final int PERFECT_PARTICLES = 10;

    private ParrySystem() {
    }

    /** Réglages de parade de cet objet, s'il s'agit d'une arme qui pare et que la parade est active (rules.json, autres mods). */
    public static Optional<ParryDefinition> parryOf(ItemStack stack) {
        if (!WeaponData.rules().parryActive()) return Optional.empty();
        return WeaponProfiles.definition(stack).filter(definition -> definition.ability() == WeaponAbility.PARRY).map(WeaponDefinition::parry);
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
        // La recharge vient d'une garde brisée.
        if (parry.isEmpty() || player.getCooldowns().isOnCooldown(stack.getItem())) return false;
        if (WeaponItem.isTwoHanded(stack)) return true;

        return player.getOffhandItem().isEmpty() || (parry.get().whileDualWielding() && DualWieldSystem.isDualWielding(player));
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
        return isParrying(player) && DualWieldSystem.isDualWielding(player);
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

    /**
     * Début d'une parade : ouvre la fenêtre de parade parfaite, sauf si la précédente est trop récente
     * (relever la garde en boucle ne donne pas une parade parfaite permanente).
     */
    public static void beginParry(Player player, ItemStack stack) {
        ParryDefinition parry = parryOf(stack).orElseThrow();
        PlayerCombatState state = PlayerCombatState.of(player);
        long now = player.level().getGameTime();
        boolean available = WeaponData.rules().perfectParryActive() && parry.perfectWindowSeconds() > 0.0D && now >= state.perfectParryReadyAt;
        if (!available) {
            state.perfectParryUntil = 0L;
            return;
        }
        state.perfectParryUntil = now + AbilityCooldowns.ticks(parry.perfectWindowSeconds());
        state.perfectParryReadyAt = state.perfectParryUntil + AbilityCooldowns.ticks(parry.perfectCooldownSeconds());
    }

    /** La parade en cours vient de commencer : un coup reçu maintenant est bloqué en entier. */
    public static boolean isInPerfectWindow(Player player) {
        return player.level().getGameTime() < PlayerCombatState.of(player).perfectParryUntil;
    }

    /** Coup reçu (côté serveur) : réduit avant l'armure, comme la parade 1.8. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isParrying(player)) return;
        float damage = event.getAmount();
        DamageSource source = event.getSource();
        if (damage <= 0.0F || source.is(DamageTypes.THORNS) || !comesFromFront(player, source)) return;

        ItemStack weapon = player.getMainHandItem();
        ParryDefinition parry = parryOf(weapon).orElseThrow();
        // Juste après un coup, le jeu ignore presque tout nouveau coup : pas d'usure ni de son pour un coup ignoré.
        boolean ignoredAnyway = player.invulnerableTime > VANILLA_INVULNERABILITY_THRESHOLD && !source.is(DamageTypeTags.BYPASSES_COOLDOWN);

        if (isInPerfectWindow(player)) {
            // Rien ne passe : ni dégâts, ni recul, ni usure. La garde ne peut pas être brisée.
            event.setCanceled(true);
            if (!ignoredAnyway) onPerfectParry(player, weapon, source, damage, parry.perfectDamageBonus(), parry.perfectBonusSeconds());
            // Réflexion a mis l'arme en recharge : elle ne pare plus, la pose ne doit pas rester.
            if (player.getCooldowns().isOnCooldown(weapon.getItem())) player.stopUsingItem();
            return;
        }

        float blocked = (float) (damage * parry.damageReduction() / PERCENT);
        event.setAmount(damage - blocked);
        if (ignoredAnyway) return;

        int cost = durabilityCost(blocked, parry);
        if (cost > 0) weapon.hurtAndBreak(cost, player, EquipmentSlot.MAINHAND);
        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.PARRY.get(), SoundSource.PLAYERS, 1.0F, pitch);

        if (breaksGuard(source, parry)) breakGuard(player, weapon, parry);
    }

    /**
     * Effets communs d'une parade parfaite, à l'arme ou au bouclier : son et particules, bonus sur le prochain coup
     * (Riposte s'y ajoute), renvoi des dégâts (Reflective).
     *
     * @param guard objet qui a paré
     */
    public static void onPerfectParry(Player player, ItemStack guard, DamageSource source, float damage, double bonusPercent, double bonusSeconds) {
        playPerfectParry(player, guard);
        int riposte = ModEnchantments.level(player.level(), guard, ModEnchantments.RIPOSTE);
        grantPerfectBonus(player, bonusPercent + riposte * ModEnchantments.RIPOSTE_PERCENT_PER_LEVEL, bonusSeconds);
        ModEnchantments.reflect(player, guard, source, damage);
    }

    /** Après une parade parfaite, le prochain coup porté dans le temps imparti fait plus de dégâts. */
    public static void grantPerfectBonus(Player player, double percent, double seconds) {
        if (percent <= 0.0D) return;
        PlayerCombatState state = PlayerCombatState.of(player);
        state.perfectBonus = percent / PERCENT;
        state.perfectBonusUntil = player.level().getGameTime() + AbilityCooldowns.ticks(seconds);
    }

    /**
     * Coup porté au corps à corps (côté serveur) : applique et consomme le bonus de parade parfaite.
     * Avant les autres calculs, pour qu'une parade adverse réduise le coup déjà augmenté.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHitWithPerfectBonus(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        // Pas les dégâts renvoyés (épines, Reflective) : le bonus est pour le prochain vrai coup.
        if (!(ModEnchantments.meleeAttacker(source) instanceof Player attacker)) return;
        PlayerCombatState state = PlayerCombatState.of(attacker);
        if (state.perfectBonus <= 0.0D) return;
        if (attacker.level().getGameTime() >= state.perfectBonusUntil) {
            state.perfectBonus = 0.0D;
            return;
        }
        LivingEntity target = event.getEntity();
        // Un coup que le jeu va ignorer (cible encore invulnérable) ne gâche pas le bonus.
        if (target.invulnerableTime > VANILLA_INVULNERABILITY_THRESHOLD && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) return;
        event.setAmount((float) (event.getAmount() * (1.0D + state.perfectBonus)));
        state.perfectBonus = 0.0D;
        if (attacker.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.5D), target.getZ(), PERFECT_PARTICLES, 0.3D, 0.4D, 0.3D, 0.2D);
        }
    }

    /** Coup direct d'une arme qui désactive les boucliers (hache vanilla, ou option disables_shield). */
    public static boolean breaksGuard(DamageSource source, ParryDefinition parry) {
        return WeaponData.rules().guardBreakActive() && parry.guardBreakSeconds() > 0.0D
                && source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
                && attacker.canDisableShield();
    }

    /** La parade s'arrête et l'arme ne peut plus parer pendant guard_break_seconds (icône de recharge vanilla). */
    private static void breakGuard(Player player, ItemStack weapon, ParryDefinition parry) {
        AbilityCooldowns.start(player, weapon, parry.guardBreakSeconds());
        player.stopUsingItem();
        float pitch = 0.8F + player.getRandom().nextFloat() * 0.4F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, pitch);
        if (player.level() instanceof ServerLevel level) {
            Vec3 point = pointInFront(player);
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 16, 0.3D, 0.3D, 0.3D, 0.2D);
        }
    }

    /** Son et particules de la parade parfaite, selon le matériau de l'arme. */
    public static void playPerfectParry(Player player, ItemStack weapon) {
        ParryMaterial material = ParryMaterial.of(weapon);
        float pitch = 0.95F + player.getRandom().nextFloat() * 0.1F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.perfectParry(material), SoundSource.PLAYERS, 1.0F, pitch);
        if (player.level() instanceof ServerLevel level) {
            Vec3 point = pointInFront(player);
            for (ParticleOptions particle : material.particles()) {
                level.sendParticles(particle, point.x, point.y, point.z, PERFECT_PARTICLES, 0.25D, 0.25D, 0.25D, 0.1D);
            }
        }
    }

    /** Devant le torse du joueur, là où l'arme bloque le coup. */
    private static Vec3 pointInFront(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.8D)).add(0.0D, -0.3D, 0.0D);
    }
}
