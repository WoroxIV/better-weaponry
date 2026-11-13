package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Portée des armes dans le tooltip : une stat de base vert foncé ("5 Attack Reach"), comme les dégâts et la vitesse,
 * à la place du bonus bleu vanilla ("+2 Entity Interaction Range").
 * Aucune dépendance au client : ce rangement est vérifié par les tests automatiques.
 */
public final class ReachTooltip {
    public static final String REACH_KEY = "attribute." + BetterWeaponry.MODID + ".attack_reach";
    private static final double PLAYER_BASE_REACH = 3.0D;
    /** Début des clés vanilla des titres de stats, ex : "item.modifiers.mainhand". */
    private static final String HEADER_KEY = "item.modifiers.";
    private static final String SPEED_KEY_PART = "attack_speed";
    // Format vanilla d'une stat de base ("%s %s" en anglais, "%2$s : %1$s" dans d'autres langues) : l'ordre suit la langue.
    private static final String VANILLA_STAT_FORMAT_KEY = "attribute.modifier.equals.0";

    private ReachTooltip() {
    }

    /** Ces mods réécrivent aussi les lignes d'attributs : Apothic Attributes (1.21) et Attributes Lib, son nom en 1.20. */
    private static final List<String> TOOLTIP_MODS = List.of("apothic_attributes", "attributeslib");

    /** Combat Ascended, Better Combat, Epic Fight et Apothic Attributes affichent la portée à leur façon : on leur laisse la ligne. */
    public static boolean handledByAnotherMod() {
        return BwCompat.combatReforged() || BwCompat.betterCombat() || BwCompat.epicFight()
                || TOOLTIP_MODS.stream().anyMatch(Platform.get()::isModLoaded);
    }

    public static void onTooltip(ItemStack stack, List<Component> tooltip) {
        if (handledByAnotherMod() || !(stack.getItem() instanceof WeaponItem) || !BwCompat.reachActive()) return;
        // Stats cachées (composant attribute_modifiers sans tooltip, ou un autre mod) : rien à remplacer.
        int header = indexOf(tooltip, HEADER_KEY);
        if (header < 0) return;

        String reachKey = Attributes.ENTITY_INTERACTION_RANGE.value().getDescriptionId();
        tooltip.removeIf(line -> containsTranslationKey(line, reachKey));

        // Sous la vitesse d'attaque, pour garder les trois stats ensemble ; sinon juste sous le titre.
        header = indexOf(tooltip, HEADER_KEY);
        tooltip.add(Math.max(header, lastIndexOf(tooltip, SPEED_KEY_PART)) + 1, line(totalReach(stack)));
    }

    /** " 5 Attack Reach", indentée et vert foncé comme les stats de base vanilla. */
    public static Component line(double reach) {
        return Component.literal(" ").append(Component.translatable(VANILLA_STAT_FORMAT_KEY,
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(reach), Component.translatable(REACH_KEY))).withStyle(ChatFormatting.DARK_GREEN);
    }

    /** Portée du joueur avec cet objet : base 3, plus les bonus de l'objet (ceux de l'arme et ceux d'un datapack ou d'un autre mod). */
    public static double totalReach(ItemStack stack) {
        double total = PLAYER_BASE_REACH;
        for (ItemAttributeModifiers.Entry entry : Platform.get().attributeModifiers(stack).modifiers()) {
            if (entry.attribute().equals(Attributes.ENTITY_INTERACTION_RANGE) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                total += entry.modifier().amount();
            }
        }
        return total;
    }

    private static int indexOf(List<Component> tooltip, String keyPart) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (containsTranslationKey(tooltip.get(i), keyPart)) return i;
        }
        return -1;
    }

    private static int lastIndexOf(List<Component> tooltip, String keyPart) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            if (containsTranslationKey(tooltip.get(i), keyPart)) return i;
        }
        return -1;
    }

    /** Cherche une clé de traduction dans le composant, ses arguments et ses enfants (récursif). */
    public static boolean containsTranslationKey(Component component, String keyPart) {
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
