package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;

/**
 * Règles de combat ajoutées par les stats des armes : pénétration d'armure, coup balayé,
 * temps d'invulnérabilité en dual wield. (Les effets à l'impact sont dans {@link WeaponItem#hurtEnemy}.)
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class CombatEvents {
    /** En vanilla, une cible ignore les coups tant que son compteur d'invulnérabilité dépasse 10 ticks. */
    private static final int VANILLA_INVULNERABILITY_THRESHOLD = 10;
    private static final double TICKS_PER_SECOND = 20.0D;

    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        // Coups directs au corps à corps uniquement (pas les flèches, ni les dégâts d'épines, d'explosion...).
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) return;

        applyArmorPenetration(event, attacker);

        if (attacker instanceof Player player && DualWieldSystem.isDualWielding(player)) {
            int protectionTicks = (int) Math.round(WeaponData.rules().dualWieldInvulnerabilitySeconds() * TICKS_PER_SECOND);
            event.getContainer().setPostAttackInvulnerabilityTicks(VANILLA_INVULNERABILITY_THRESHOLD + protectionTicks);
        }
    }

    /**
     * L'armure réduit moins les dégâts : avec 30 % de pénétration, elle ne retire que 70 % de ce qu'elle retirerait.
     * Pendant un coup de la main secondaire, l'attribut est déjà celui de l'arme secondaire ({@link HandSwap}).
     */
    private static void applyArmorPenetration(LivingIncomingDamageEvent event, LivingEntity attacker) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        AttributeInstance attribute = attacker.getAttribute(ModAttributes.ARMOR_PENETRATION);
        double penetration = attribute == null ? 0.0D : attribute.getValue();
        if (penetration <= 0.0D) return;

        float kept = (float) (1.0D - Math.min(1.0D, penetration));
        event.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * kept);
    }

    /** Armes configurées sans coup balayé (estoc) : le balayage vanilla est annulé. */
    @SubscribeEvent
    public static void onSweepAttack(SweepAttackEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof WeaponItem weapon && !weapon.definition().sweep().enabled()) {
            event.setSweeping(false);
        }
    }
}
