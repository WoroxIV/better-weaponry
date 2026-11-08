package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.registry.ModSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Règles de combat des mobs. Ils ignorent le maniement à deux mains et les combos, mais respectent la portée de leur arme,
 * peuvent parer, voir leur garde brisée, et alternent leurs coups en dual wield. Tout se coupe avec rules.json, Better Combat ou Epic Fight.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class MobCombat {
    private static final String OFFHAND_TURN = BetterWeaponry.MODID + ":offhand_turn";
    private static final String GUARD_BROKEN_UNTIL = BetterWeaponry.MODID + ":guard_broken_until";
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

    public static boolean isOffhandTurn(Mob mob) {
        return mob.getPersistentData().getBoolean(OFFHAND_TURN);
    }

    public static void setOffhandTurn(Mob mob, boolean offhand) {
        mob.getPersistentData().putBoolean(OFFHAND_TURN, offhand);
    }

    public static boolean isGuardBroken(Mob mob) {
        return mob.level().getGameTime() < mob.getPersistentData().getLong(GUARD_BROKEN_UNTIL);
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
    @SubscribeEvent
    public static void onMobHit(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !mob.isUsingItem() || !WeaponData.rules().parryActive()) return;
        ItemStack guard = mob.getUseItem();
        Optional<ParryDefinition> parry = ParrySystem.parryOf(guard);
        if (parry.isEmpty() && !ShieldSystem.isShield(guard)) return;
        DamageSource source = event.getSource();
        float damage = event.getAmount();
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
        mob.getPersistentData().putLong(GUARD_BROKEN_UNTIL, mob.level().getGameTime() + AbilityCooldowns.ticks(seconds));
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
