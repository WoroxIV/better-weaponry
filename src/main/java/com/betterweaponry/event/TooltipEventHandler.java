package com.betterweaponry.event;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.item.WeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Ajoute "Attack Reach" et "Two Handed" sous les stats vertes (dégâts, vitesse) des armes.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class TooltipEventHandler {
    private static final double PLAYER_BASE_REACH = 3.0D;

    private static final String REACH_KEY = "attribute.betterweaponry.attack_reach";
    private static final String TWO_HANDED_KEY = "attribute.betterweaponry.two_handed";
    // Morceau de la clé vanilla "attribute.name.player.entity_interaction_range"
    private static final String VANILLA_REACH_KEY_PART = "entity_interaction_range";

    private TooltipEventHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (ClientConfig.get(ClientConfig.SHOW_ATTACK_REACH_TOOLTIP)) {
            addReachLine(stack, tooltip);
        }

        if (ClientConfig.get(ClientConfig.SHOW_TWO_HANDED_TOOLTIP) && WeaponItem.isTwoHanded(stack)) {
            // Inséré après la portée, qui compte comme une ligne de stat (voir findStatsEnd)
            insertAfterStats(tooltip, statLine(Component.translatable(TWO_HANDED_KEY)));
        }
    }

    /** Remplace la ligne bleue vanilla de portée par une ligne verte alignée sur les autres stats. */
    private static void addReachLine(ItemStack stack, List<Component> tooltip) {
        double totalReach = PLAYER_BASE_REACH;
        boolean hasReachModifier = false;

        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.attribute().equals(Attributes.ENTITY_INTERACTION_RANGE)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                totalReach += entry.modifier().amount();
                hasReachModifier = true;
            }
        }

        // Toutes les armes affichent leur portée (3 par défaut), les autres objets seulement s'ils la modifient.
        if (!DualWieldSystem.isWeapon(stack) && !hasReachModifier) return;

        tooltip.removeIf(line -> containsTranslationKey(line, VANILLA_REACH_KEY_PART));

        // Format vanilla "#.##" : affiche "3.5" ou "4" et évite les "3.6000000000000005" des calculs en double.
        String reachText = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(totalReach);
        insertAfterStats(tooltip, statLine(Component.literal(reachText + " ").append(Component.translatable(REACH_KEY))));
    }

    /** Met en forme une ligne comme les stats vanilla : indentée d'un espace, en vert foncé. */
    private static Component statLine(Component content) {
        return Component.literal(" ").append(content).withStyle(ChatFormatting.DARK_GREEN);
    }

    private static void insertAfterStats(List<Component> tooltip, Component line) {
        int index = findStatsEnd(tooltip);
        if (index != -1) {
            tooltip.add(index, line);
        }
    }

    /**
     * Index juste après la dernière ligne de stat (dégâts, vitesse ou notre portée), ou -1 si aucune
     * (ex : stats masquées par un autre mod). On parcourt à l'envers pour trouver la dernière.
     */
    private static int findStatsEnd(List<Component> tooltip) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            Component line = tooltip.get(i);
            if (containsTranslationKey(line, "attack_speed")
                    || containsTranslationKey(line, "attack_damage")
                    || containsTranslationKey(line, REACH_KEY)) {
                return i + 1;
            }
        }
        return -1;
    }

    /** Cherche une clé de traduction dans le composant, ses arguments et ses enfants (récursif). */
    private static boolean containsTranslationKey(Component component, String keyPart) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (translatable.getKey().contains(keyPart)) return true;
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component child && containsTranslationKey(child, keyPart)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (containsTranslationKey(sibling, keyPart)) return true;
        }
        return false;
    }
}
