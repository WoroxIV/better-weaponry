package com.betterweaponry.combat;

import com.betterweaponry.registry.ModAttributes;
import java.util.UUID;
import com.betterweaponry.platform.IncomingDamage;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.registry.ModSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Règles de combat des mobs. Ils ignorent le maniement à deux mains et les combos, mais respectent la portée de leur arme,
 * peuvent parer, voir leur garde brisée, et alternent leurs coups en dual wield. Tout se coupe avec rules.json, Better Combat ou Epic Fight.
 */
public final class MobCombat {
    private static final UUID SWORD_DAMAGE_ID = ModAttributes.uuid("mob_sword_damage");
    private static final double PLAYER_BASE_REACH = 3.0D;
    /** Une arme plus courte que le vanilla réduit la zone du mob, sans jamais l'annuler. */
    private static final double SMALLEST_REACH_CHANGE = -0.5D;
    private static final double PERCENT = 100.0D;
    private static final int GUARD_GOAL_PRIORITY = 1;
    private static final int THROW_GOAL_PRIORITY = 2;

    private MobCombat() {
    }

    /** Ce que l'arme ajoute (ou retire) à la zone d'attaque du mob, par rapport à une arme de portée normale. */
    public static double extraReach(Mob mob) {
        if (!WeaponData.rules().reachActive()) return 0.0D;
        return WeaponProfiles.definition(mob.getMainHandItem()).map(definition -> Math.max(SMALLEST_REACH_CHANGE, definition.attackReach() - PLAYER_BASE_REACH)).orElse(0.0D);
    }

    /**
     * Fait porter un coup à un mob avec les dégâts d'une épée du matériau de son arme : il frappe au rythme de son espèce,
     * quelle que soit la vitesse de l'arme, et une arme lente mais puissante serait bien plus dangereuse entre ses mains.
     * À appeler après l'éventuel échange de mains : l'arme en main principale est celle qui frappe.
     */
    public static boolean hitLikeSword(Mob mob, BooleanSupplier attack) {
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage == null || !(mob.getMainHandItem().getItem() instanceof WeaponItem weapon)) return attack.getAsBoolean();
        WeaponMaterial material = weapon.getMaterial();
        double correction = material.swordDamage() - weapon.definition().damageFor(material);
        damage.addTransientModifier(new AttributeModifier(SWORD_DAMAGE_ID, "Mob sword damage", correction, AttributeModifier.Operation.ADDITION));
        try {
            return attack.getAsBoolean();
        } finally {
            damage.removeModifier(SWORD_DAMAGE_ID);
        }
    }

    public static boolean isOffhandTurn(Mob mob) {
        return ((ICombatMob) mob).betterweaponry$offhandTurn();
    }

    public static void setOffhandTurn(Mob mob, boolean offhand) {
        ((ICombatMob) mob).betterweaponry$setOffhandTurn(offhand);
    }

    public static boolean isGuardBroken(Mob mob) {
        return mob.level().getGameTime() < ((ICombatMob) mob).betterweaponry$guardBrokenUntil();
    }

    /** Main qui peut lever une garde : l'arme qui pare, sinon un bouclier. Null s'il n'y en a pas. */
    @Nullable
    public static InteractionHand guardHand(Mob mob) {
        if (isParryWeapon(mob.getMainHandItem()) || ShieldSystem.isShield(mob.getMainHandItem())) return InteractionHand.MAIN_HAND;
        return ShieldSystem.isShield(mob.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }

    private static boolean isParryWeapon(ItemStack stack) {
        return WeaponProfiles.definition(stack).map(WeaponDefinition::ability).filter(ability -> ability == WeaponAbility.PARRY).isPresent();
    }

    /** Les buts ne sont pas sauvegardés : ils sont remis à chaque arrivée du mob dans le monde, y compris au rechargement. */
    public static void addGoals(Mob mob) {
        if (!(mob instanceof PathfinderMob pathfinder)) return;
        if (guardHand(mob) != null) pathfinder.goalSelector.addGoal(GUARD_GOAL_PRIORITY, new MobGuardGoal(pathfinder));
        if (mob instanceof AbstractSkeleton && mob.getMainHandItem().getItem() instanceof ShurikenItem) {
            pathfinder.goalSelector.addGoal(THROW_GOAL_PRIORITY, new MobThrowShurikenGoal(pathfinder));
        }
    }

    /** Coup reçu par un mob en garde : mêmes règles qu'un joueur, sans parade parfaite. Le bouclier reste géré par le jeu. */
    public static void onMobHit(IncomingDamage event) {
        if (!(event.target() instanceof Mob mob) || !mob.isUsingItem() || !WeaponData.rules().parryActive()) return;
        ItemStack guard = mob.getUseItem();
        Optional<ParryDefinition> parry = ParrySystem.parryOf(guard);
        if (parry.isEmpty() && !ShieldSystem.isShield(guard)) return;
        DamageSource source = event.source();
        float damage = event.amount();
        if (damage <= 0.0F || source.is(DamageTypes.THORNS) || !ParrySystem.comesFromFront(mob, source)) return;

        parry.ifPresent(definition -> {
            event.setAmount((float) (damage * (1.0D - definition.damageReduction() / PERCENT)));
            mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), ModSounds.PARRY.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        });
        ParryDefinition rules = parry.orElse(ParryDefinition.DEFAULT);
        if (ParrySystem.breaksGuard(source, rules)) breakGuard(mob, rules.guardBreakSeconds());
    }

    /** Le but de garde s'arrête au tick suivant : le coup en cours est encore paré, comme pour un joueur. */
    private static void breakGuard(Mob mob, double seconds) {
        ((ICombatMob) mob).betterweaponry$setGuardBrokenUntil(mob.level().getGameTime() + AbilityCooldowns.ticks(seconds));
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
