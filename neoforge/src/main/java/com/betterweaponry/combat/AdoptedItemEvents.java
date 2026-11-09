package com.betterweaponry.combat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * Ce que {@link WeaponItem} fait lui-même pour les armes du mod, version événements pour les objets adoptés
 * par un fichier d'arme (voir {@link WeaponProfiles}). Les effets à l'impact, la pose et la zone de balayage
 * sont dans ItemStackMixin et PlayerCombatMixin.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class AdoptedItemEvents {
    private static final double PERCENT = 100.0D;

    private AdoptedItemEvents() {
    }

    /** Clic droit : même logique que {@link WeaponItem#use}. Appelé côté client puis côté serveur. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!WeaponProfiles.isAdopted(stack)) return;

        Player player = event.getEntity();
        if (ParrySystem.canParry(player, event.getHand(), stack)) {
            ParrySystem.beginParry(player, stack);
            player.startUsingItem(event.getHand());
        } else if (DashAbility.canDash(player, event.getHand(), stack)) {
            DashAbility.dash(event.getLevel(), player, stack, DashAbility.dashOf(stack).orElseThrow());
        } else {
            return;
        }
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    /** Pénétration d'armure et balayage de l'arme ; les dégâts, la vitesse et la portée restent ceux de l'objet. */
    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        Optional<WeaponProfiles.Profile> profile = WeaponProfiles.of(event.getItemStack()).filter(WeaponProfiles.Profile::adopted);
        if (profile.isEmpty()) return;

        RulesDefinition rules = WeaponData.rules();
        WeaponDefinition definition = profile.get().definition();
        if (rules.armorPenetrationActive() && definition.armorPenetration() > 0.0D) {
            event.addModifier(ModAttributes.armorPenetration(), new AttributeModifier(ModAttributes.BASE_ARMOR_PENETRATION_ID,
                    definition.armorPenetration() / PERCENT, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        if (rules.sweepActive() && definition.sweep().enabled() && definition.sweep().damageRatio() > 0.0D) {
            event.addModifier(Attributes.SWEEPING_DAMAGE_RATIO, new AttributeModifier(WeaponItem.BASE_SWEEPING_RATIO_ID,
                    definition.sweep().damageRatio() / PERCENT, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
    }
}
