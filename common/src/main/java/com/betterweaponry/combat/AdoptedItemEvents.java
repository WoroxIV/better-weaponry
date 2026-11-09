package com.betterweaponry.combat;

import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Ce que {@link WeaponItem} fait lui-même pour les armes du mod, version événements pour les objets adoptés
 * par un fichier d'arme (voir {@link WeaponProfiles}). Les effets à l'impact, la pose et la zone de balayage
 * sont dans ItemStackMixin et PlayerCombatMixin.
 */
public final class AdoptedItemEvents {
    private static final double PERCENT = 100.0D;

    private AdoptedItemEvents() {
    }

    /**
     * Clic droit : même logique que {@link WeaponItem#use}. Appelé côté client puis côté serveur.
     *
     * @return vrai si la capacité de l'arme a été utilisée
     */
    public static boolean onRightClickItem(Player player, InteractionHand hand, ItemStack stack, Level level) {
        if (!WeaponProfiles.isAdopted(stack)) return false;
        if (ParrySystem.canParry(player, hand, stack)) {
            ParrySystem.beginParry(player, stack);
            player.startUsingItem(hand);
        } else if (DashAbility.canDash(player, hand, stack)) {
            DashAbility.dash(level, player, stack, DashAbility.dashOf(stack).orElseThrow());
        } else {
            return false;
        }
        return true;
    }

    /** Pénétration d'armure et balayage de l'arme, dans la main principale ; les dégâts, la vitesse et la portée restent ceux de l'objet. */
    public static void collectModifiers(ItemStack stack, BiConsumer<Holder<Attribute>, AttributeModifier> output) {
        Optional<WeaponProfiles.Profile> profile = WeaponProfiles.of(stack).filter(WeaponProfiles.Profile::adopted);
        if (profile.isEmpty()) return;

        RulesDefinition rules = WeaponData.rules();
        WeaponDefinition definition = profile.get().definition();
        if (rules.armorPenetrationActive() && definition.armorPenetration() > 0.0D) {
            output.accept(ModAttributes.armorPenetration(), new AttributeModifier(ModAttributes.BASE_ARMOR_PENETRATION_ID,
                    definition.armorPenetration() / PERCENT, AttributeModifier.Operation.ADD_VALUE));
        }
        if (rules.sweepActive() && definition.sweep().enabled() && definition.sweep().damageRatio() > 0.0D) {
            output.accept(Attributes.SWEEPING_DAMAGE_RATIO, new AttributeModifier(WeaponItem.BASE_SWEEPING_RATIO_ID,
                    definition.sweep().damageRatio() / PERCENT, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
