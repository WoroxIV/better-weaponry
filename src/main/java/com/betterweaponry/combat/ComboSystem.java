package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.ComboStep;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.network.ComboSyncPayload;
import com.betterweaponry.network.DualSwingPayload;
import com.betterweaponry.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Combos : chaque coup qui touche fait avancer le combo de l'arme principale ; certains coups sont spéciaux
 * (zone, deux armes à la fois, dégâts, recul, recharge). Appelé par PlayerCombatMixin autour de Player.attack(),
 * côté client (prédiction : animation, indicateur, son) et côté serveur (dégâts), avec la même logique.
 * <ul>
 *   <li>un coup dans le vide n'appelle pas attack() : il ne fait pas avancer le combo ;</li>
 *   <li>un coup chargé sous min_charge remet le combo à zéro, comme reset_after_seconds sans toucher
 *       ou un changement d'arme ;</li>
 *   <li>un coup spécial est toujours porté par l'arme principale, et le combo suivant repart de cette main.</li>
 * </ul>
 */
public final class ComboSystem {
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double PERCENT = 100.0D;
    /** Une zone de combo touche aussi les ennemis un peu plus haut ou plus bas que le joueur. */
    private static final double AREA_HEIGHT = 1.0D;
    /** Comme le vanilla : 1 niveau de Recul = 0.5 de force de recul. */
    private static final double KNOCKBACK_PER_LEVEL = 0.5D;
    /** Particules de balayage : une tous les 30° de la zone. */
    private static final double DEGREES_PER_PARTICLE = 30.0D;

    private static final ResourceLocation DAMAGE_BONUS_ID = id("combo_damage_bonus");
    private static final ResourceLocation DAMAGE_MULTIPLIER_ID = id("combo_damage_multiplier");
    private static final ResourceLocation KNOCKBACK_ID = id("combo_knockback");

    private ComboSystem() {
    }

    /**
     * Ce qui va se passer pour un coup : l'étape du combo, si elle compte (coup assez chargé) et si les deux
     * armes frappent. {@code combo == null} : pas de combo pour ce coup.
     */
    public record Hit(@Nullable ComboDefinition combo, @Nullable WeaponType type, @Nullable ComboStep step, int index,
                      boolean counts, boolean bothHands, float strength) {
        static Hit none(float strength) {
            return new Hit(null, null, null, 0, false, false, strength);
        }

        /** Coup spécial du combo (zone, deux armes, multiplicateurs). */
        public boolean special() {
            return counts && step != null && step.isSpecial();
        }
    }

    /** Avancement du combo, pour l'indicateur à l'écran. */
    public record Progress(int steps, int done, boolean nextIsSpecial) {
    }

    // --- Quel combo ---

    /** Combo de cet objet s'il est activé (fichier de l'arme et rules.json). Coupé avec Better Combat et Epic Fight, qui ont les leurs. */
    public static Optional<ComboDefinition> comboOf(ItemStack stack) {
        if (!WeaponData.rules().combosActive()) return Optional.empty();
        return WeaponProfiles.definition(stack).map(WeaponDefinition::combo).filter(combo -> combo.enabled() && !combo.steps().isEmpty());
    }

    /** Combo en vigueur pour ce joueur : celui de l'arme principale, si l'autre main tient la même arme quand il le faut. */
    public static Optional<ComboDefinition> activeCombo(Player player) {
        return comboOf(player.getMainHandItem()).filter(combo -> !combo.requiresDualWield() || holdsPair(player));
    }

    /** Le joueur tient le même type d'arme dans chaque main, en dual wield (pas d'arme à deux mains). */
    public static boolean holdsPair(Player player) {
        Optional<WeaponType> main = WeaponProfiles.type(player.getMainHandItem());
        return main.isPresent() && main.equals(WeaponProfiles.type(player.getOffhandItem())) && DualWieldSystem.isDualWielding(player);
    }

    // --- Déroulement d'un coup (PlayerCombatMixin) ---

    /** Avant le coup : remet le combo à zéro si besoin et décide de l'étape. La force du coup est lue ici. */
    public static Hit beginHit(Player player, PlayerCombatState state) {
        float strength = player.getAttackStrengthScale(0.5F);
        Optional<ComboDefinition> active = activeCombo(player);
        if (active.isEmpty()) {
            state.resetCombo();
            return Hit.none(strength);
        }
        ComboDefinition combo = active.get();
        WeaponType type = WeaponProfiles.type(player.getMainHandItem()).orElseThrow();
        if (state.comboType != type || player.level().getGameTime() > state.comboExpiresAt) state.comboStep = 0;
        state.comboType = type;

        int index = Math.floorMod(state.comboStep, combo.steps().size());
        ComboStep step = combo.steps().get(index);
        boolean counts = strength * PERCENT >= combo.minCharge();
        boolean bothHands = counts && step.bothHands() && holdsPair(player);
        return new Hit(combo, type, step, index, counts, bothHands, strength);
    }

    /**
     * Coup spécial : le coup vanilla sur la cible, avec les dégâts et le recul du combo (bonus temporaires
     * sur les attributs du joueur), puis l'arme secondaire et la zone (côté serveur).
     *
     * @param vanillaAttack le Player.attack() d'origine
     */
    public static void performSpecialHit(Player player, Entity target, Hit hit, Runnable vanillaAttack) {
        PlayerCombatState state = PlayerCombatState.of(player);
        ComboStep step = hit.step();
        List<Runnable> undo = new ArrayList<>();
        double mainDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        // Deux armes à la fois : dégâts = moyenne des deux armes.
        double damage = hit.bothHands()
                ? (mainDamage + HandSwap.attributeWithHandsSwapped(player, Attributes.ATTACK_DAMAGE)) / 2.0D
                : mainDamage;
        addTemporaryModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_BONUS_ID,
                damage - mainDamage, AttributeModifier.Operation.ADD_VALUE, undo);
        addTemporaryModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MULTIPLIER_ID,
                step.damageMultiplier() - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, undo);
        addTemporaryModifier(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK_ID,
                step.knockback(), AttributeModifier.Operation.ADD_VALUE, undo);

        state.activeStep = step;
        try {
            vanillaAttack.run();
            if (player.level() instanceof ServerLevel level) {
                DamageSource source = player.damageSources().playerAttack(player);
                if (hit.bothHands() && target instanceof LivingEntity living) applyOffhandWeapon(level, player, living, source);
                if (step.hasArea()) hitArea(level, player, target, hit, source);
                if (hit.bothHands()) player.getOffhandItem().hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
            }
        } finally {
            state.activeStep = null;
            undo.forEach(Runnable::run);
        }
    }

    /** Après le coup : fait avancer (ou remet à zéro) le combo, règle la recharge suivante, prévient le client. */
    public static void endHit(Player player, PlayerCombatState state, Hit hit) {
        state.nextAttackDelay = 0.0F;
        if (hit.combo() == null) return;

        Level level = player.level();
        if (hit.counts()) {
            state.comboStep = (hit.index() + 1) % hit.combo().steps().size();
            if (hit.special() && (hit.bothHands() || hit.step().cooldownMultiplier() != 1.0D)) {
                state.nextAttackDelay = nextAttackDelay(player, hit);
            }
            if (hit.bothHands()) {
                state.dualSwingPending = true;
                if (!level.isClientSide) PacketDistributor.sendToPlayersTrackingEntity(player, new DualSwingPayload(player.getId()));
            }
        } else {
            state.comboStep = 0;
        }
        state.comboExpiresAt = level.getGameTime() + Math.round(hit.combo().resetAfterSeconds() * TICKS_PER_SECOND);

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            PacketDistributor.sendToPlayer(serverPlayer, ComboSyncPayload.of(player, state));
        } else if (level.isClientSide && hit.counts() && hit.combo().steps().get(state.comboStep).isSpecial()) {
            comboReadyFeedback(player);
        }
    }

    /** Recharge imposée par le dernier coup spécial, si le joueur tient toujours cette arme. */
    @Nullable
    public static Float attackDelayOverride(Player player, PlayerCombatState state) {
        if (state.nextAttackDelay <= 0.0F || WeaponProfiles.type(player.getMainHandItem()).orElse(null) != state.comboType) return null;
        return state.nextAttackDelay;
    }

    /** Vrai pendant un coup spécial en zone : le coup balayé vanilla est alors remplacé par la zone. */
    public static boolean isAreaAttackInProgress(Player player) {
        ComboStep step = PlayerCombatState.of(player).activeStep;
        return step != null && step.hasArea();
    }

    /** Le coup de bras en cours anime les deux bras (coup à deux armes). */
    public static boolean isDualSwinging(Player player) {
        return player.swinging && PlayerCombatState.of(player).dualSwingActive;
    }

    /** Avancement du combo du joueur, ou null s'il n'a pas commencé (ou a expiré). */
    @Nullable
    public static Progress progress(Player player) {
        PlayerCombatState state = PlayerCombatState.of(player);
        Optional<ComboDefinition> combo = activeCombo(player);
        if (combo.isEmpty() || state.comboStep <= 0 || player.level().getGameTime() > state.comboExpiresAt) return null;
        if (WeaponProfiles.type(player.getMainHandItem()).orElse(null) != state.comboType) return null;
        List<ComboStep> steps = combo.get().steps();
        int done = Math.floorMod(state.comboStep, steps.size());
        return new Progress(steps.size(), done, steps.get(done).isSpecial());
    }

    // --- Coup en zone ---

    /**
     * Touche les ennemis de la zone (sauf la cible, déjà frappée par le coup vanilla), avec les mêmes règles
     * que le coup balayé vanilla : dégâts selon la charge et les enchantements, effets d'enchantement,
     * effets à l'impact de l'arme. Le recul pousse vers l'extérieur de la zone.
     */
    private static void hitArea(ServerLevel level, Player player, Entity primary, Hit hit, DamageSource source) {
        ComboStep step = hit.step();
        float strength = hit.strength();
        // Les bonus temporaires du combo (moyenne des deux armes, multiplicateur) sont encore appliqués.
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float charge = 0.2F + strength * strength * 0.8F;
        ItemStack weapon = player.getMainHandItem();

        for (LivingEntity victim : areaTargets(player, primary, step)) {
            float enchanted = EnchantmentHelper.modifyDamage(level, weapon, victim, source, base);
            float amount = base * charge + (enchanted - base) * strength;
            if (step.knockback() > 0.0D) {
                victim.knockback(step.knockback() * KNOCKBACK_PER_LEVEL, player.getX() - victim.getX(), player.getZ() - victim.getZ());
            }
            if (victim.hurt(source, amount)) {
                EnchantmentHelper.doPostAttackEffects(level, victim, source);
                WeaponItem.applyOnHitEffects(weapon, victim, player);
                if (hit.bothHands()) applyOffhandWeapon(level, player, victim, source);
            }
        }
        spawnAreaParticles(level, player, step);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 0.8F);
    }

    /** Créatures dans la zone : à moins de radius blocs, dans l'angle devant le joueur, et qu'on a le droit de frapper. */
    private static List<LivingEntity> areaTargets(Player player, @Nullable Entity primary, ComboStep step) {
        double radius = step.radius();
        Vec3 facing = horizontalFacing(player);
        double minDot = Math.cos(Math.toRadians(step.angle() / 2.0D));
        AABB zone = player.getBoundingBox().inflate(radius, AREA_HEIGHT, radius);

        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, zone)) {
            if (candidate == player || candidate == primary || !candidate.isAlive() || !canHit(player, candidate)) continue;
            Vec3 offset = candidate.position().subtract(player.position());
            Vec3 flat = new Vec3(offset.x, 0.0D, offset.z);
            if (flat.length() - candidate.getBbWidth() / 2.0D > radius) continue;
            if (step.angle() < 360.0D && flat.lengthSqr() > 1.0E-6D && flat.normalize().dot(facing) < minDot) continue;
            targets.add(candidate);
        }
        return targets;
    }

    /** Pas ses alliés, ses animaux apprivoisés, les porte-armures marqueurs, sa monture, ni les joueurs si le PvP est coupé. */
    private static boolean canHit(Player player, LivingEntity candidate) {
        if (candidate.isSpectator() || player.isAlliedTo(candidate)) return false;
        if (candidate instanceof ArmorStand stand && stand.isMarker()) return false;
        if (candidate instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())) return false;
        if (candidate instanceof Player other && !player.canHarmPlayer(other)) return false;
        return candidate != player.getVehicle() && !player.isPassengerOfSameVehicle(candidate);
    }

    /** Particules de balayage réparties sur l'arc de la zone. */
    private static void spawnAreaParticles(ServerLevel level, Player player, ComboStep step) {
        double angle = step.angle();
        int count = Math.max(3, (int) Math.round(angle / DEGREES_PER_PARTICLE));
        boolean fullCircle = angle >= 360.0D;
        double facingYaw = Math.toRadians(player.getYRot());
        double distance = step.radius() * 0.6D;
        for (int i = 0; i < count; i++) {
            double fraction = fullCircle ? (double) i / count : (double) i / (count - 1);
            double yaw = facingYaw + Math.toRadians(-angle / 2.0D + angle * fraction);
            double x = player.getX() - Math.sin(yaw) * distance;
            double z = player.getZ() + Math.cos(yaw) * distance;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, player.getY(0.5D), z, 0, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    // --- Outils ---

    /** Effets à l'impact et effets d'enchantement (Aura de feu...) de l'arme secondaire, pour un coup à deux armes. */
    private static void applyOffhandWeapon(ServerLevel level, Player player, LivingEntity victim, DamageSource source) {
        ItemStack offhand = player.getOffhandItem();
        WeaponItem.applyOnHitEffects(offhand, victim, player);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(level, victim, source, offhand);
    }

    /**
     * Recharge du coup suivant un coup spécial : celle de l'arme principale (ou la moyenne des deux armes
     * pour un coup à deux armes) × cooldown_multiplier.
     */
    private static float nextAttackDelay(Player player, Hit hit) {
        double mainSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        float base;
        if (DualWieldSystem.isDualWielding(player)) {
            float mainDelay = DualWieldSystem.delayForSpeed(mainSpeed);
            base = hit.bothHands()
                    ? (mainDelay + DualWieldSystem.delayForSpeed(HandSwap.attackSpeedWithHandsSwapped(player))) / 2.0F
                    : mainDelay;
        } else {
            base = (float) (TICKS_PER_SECOND / mainSpeed);
        }
        return (float) (base * hit.step().cooldownMultiplier());
    }

    /** Côté client : le prochain coup est le coup spécial. Petit son et étincelles devant le joueur. */
    private static void comboReadyFeedback(Player player) {
        Level level = player.level();
        level.playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.COMBO_READY.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(0.8D)).add(0.0D, -0.4D, 0.0D);
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.ENCHANTED_HIT,
                    front.x + (player.getRandom().nextDouble() - 0.5D) * 0.5D,
                    front.y + (player.getRandom().nextDouble() - 0.5D) * 0.5D,
                    front.z + (player.getRandom().nextDouble() - 0.5D) * 0.5D,
                    0.0D, 0.05D, 0.0D);
        }
    }

    private static Vec3 horizontalFacing(Player player) {
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    /** Ajoute un bonus d'attribut pour la durée du coup, et note comment le retirer. Rien si la valeur est nulle. */
    private static void addTemporaryModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                                             AttributeModifier.Operation operation, List<Runnable> undo) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || amount == 0.0D) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        undo.add(() -> instance.removeModifier(id));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, path);
    }
}
