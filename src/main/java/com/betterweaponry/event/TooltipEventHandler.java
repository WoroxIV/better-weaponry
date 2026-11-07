package com.betterweaponry.event;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.DashAbility;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.ComboStep;
import com.betterweaponry.data.DashDefinition;
import com.betterweaponry.data.DashDirection;
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Complète les stats vertes (dégâts, vitesse, pénétration d'armure) des armes :
 * portée d'attaque, coup balayé, "Arme à deux mains", "Désactive les boucliers", effets à l'impact
 * capacité du clic droit et coups spéciaux du combo.
 * Chaque ligne ajoutée se place après la précédente, dans cet ordre.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class TooltipEventHandler {
    private static final double PLAYER_BASE_REACH = 3.0D;
    private static final double PERCENT = 100.0D;

    private static final String REACH_KEY = "attribute.betterweaponry.attack_reach";
    private static final String TWO_HANDED_KEY = "attribute.betterweaponry.two_handed";
    private static final String DISABLES_SHIELD_KEY = "attribute.betterweaponry.disables_shield";
    private static final String ON_HIT_KEY = BetterWeaponry.MODID + ".tooltip.on_hit_effect";
    private static final String ON_HIT_CHANCE_KEY = ON_HIT_KEY + ".chance";
    private static final String ABILITY_KEY = BetterWeaponry.MODID + ".tooltip.ability";
    private static final String PARRY_KEY = ABILITY_KEY + ".parry";
    private static final String PARRY_DUAL_WIELD_KEY = PARRY_KEY + ".dual_wield";
    private static final String DASH_KEY = ABILITY_KEY + ".dash";
    private static final String DASH_INPUT_KEY = DASH_KEY + ".input";
    private static final String DASH_AIRBORNE_KEY = DASH_KEY + ".airborne";
    private static final String COMBO_KEY = BetterWeaponry.MODID + ".tooltip.combo";
    private static final String COMBO_STEP_KEY = COMBO_KEY + ".step";
    private static final String COMBO_SEPARATOR_KEY = COMBO_KEY + ".separator";
    private static final String COMBO_REQUIRES_DUAL_WIELD_KEY = COMBO_KEY + ".requires_dual_wield";
    // Morceaux des clés vanilla "attribute.name.player.entity_interaction_range" et "...sweeping_damage_ratio"
    private static final String VANILLA_REACH_KEY_PART = "entity_interaction_range";
    private static final String VANILLA_SWEEP_KEY_PART = "sweeping_damage_ratio";
    // Formats vanilla d'une stat de base, nombre puis pourcentage ("%s %s" en anglais, "%s en %s" en français,
    // "%2$s: %1$s" en italien...) : l'ordre des mots suit la langue.
    private static final String VANILLA_STAT_FORMAT_KEY = "attribute.modifier.equals.0";
    private static final String VANILLA_PERCENT_STAT_FORMAT_KEY = "attribute.modifier.equals.1";

    /** Clés qui marquent une ligne de stat : les lignes ajoutées se placent après la dernière d'entre elles. */
    private static final List<String> STAT_LINE_MARKERS = List.of("attack_damage", "attack_speed", "armor_penetration",
            REACH_KEY, VANILLA_SWEEP_KEY_PART, TWO_HANDED_KEY, DISABLES_SHIELD_KEY, ON_HIT_KEY, ABILITY_KEY, COMBO_KEY);

    private TooltipEventHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (ClientConfig.get(ClientConfig.SHOW_ATTACK_REACH_TOOLTIP)) {
            addReachLine(stack, tooltip);
        }
        if (!(stack.getItem() instanceof WeaponItem weapon)) return;

        WeaponDefinition definition = weapon.definition();
        addSweepLine(definition, tooltip);
        if (ClientConfig.get(ClientConfig.SHOW_TWO_HANDED_TOOLTIP) && definition.twoHanded()) {
            insertAfterStats(tooltip, statLine(Component.translatable(TWO_HANDED_KEY)));
        }
        if (definition.disablesShield()) {
            insertAfterStats(tooltip, statLine(Component.translatable(DISABLES_SHIELD_KEY)));
        }
        for (OnHitEffect effect : definition.onHitEffects()) {
            addEffectLine(effect, tooltip);
        }
        ParrySystem.parryOf(stack).ifPresent(parry -> addParryLines(parry, tooltip));
        DashAbility.dashOf(stack).ifPresent(dash -> addDashLines(dash, tooltip));
        ComboSystem.comboOf(stack).ifPresent(combo -> addComboLines(combo, tooltip));
    }

    /**
     * Une ligne par coup spécial du combo, ex : "Combo, coup 3 : zone 360°, 3.5 blocs, recharge ×1.6",
     * puis "Combo avec la même arme dans chaque main" si besoin.
     */
    private static void addComboLines(ComboDefinition combo, List<Component> tooltip) {
        List<ComboStep> steps = combo.steps();
        for (int i = 0; i < steps.size(); i++) {
            ComboStep step = steps.get(i);
            if (!step.isSpecial()) continue;
            List<Component> parts = new ArrayList<>();
            if (step.bothHands()) parts.add(Component.translatable(COMBO_KEY + ".both_hands"));
            if (step.hasArea()) parts.add(Component.translatable(COMBO_KEY + ".area", number(step.angle()), number(step.radius())));
            if (step.damageMultiplier() != 1.0D) parts.add(Component.translatable(COMBO_KEY + ".damage", number(step.damageMultiplier())));
            if (step.knockback() > 0.0D) parts.add(Component.translatable(COMBO_KEY + ".knockback", number(step.knockback())));
            if (step.cooldownMultiplier() != 1.0D) parts.add(Component.translatable(COMBO_KEY + ".cooldown", number(step.cooldownMultiplier())));

            MutableComponent description = Component.empty();
            for (int part = 0; part < parts.size(); part++) {
                if (part > 0) description.append(Component.translatable(COMBO_SEPARATOR_KEY));
                description.append(parts.get(part));
            }
            insertAfterStats(tooltip, comboLine(Component.translatable(COMBO_STEP_KEY, i + 1, description)));
        }
        if (combo.requiresDualWield()) {
            insertAfterStats(tooltip, comboLine(Component.translatable(COMBO_REQUIRES_DUAL_WIELD_KEY)));
        }
    }

    /** Ligne de combo : indentée comme les stats, en jaune. */
    private static Component comboLine(Component content) {
        return Component.literal(" ").append(content).withStyle(ChatFormatting.YELLOW);
    }

    private static String number(double value) {
        return ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value);
    }

    /** Ex : "Clic droit : Dash (~3.5 blocs, recharge 1.5 s)", puis la direction et le dash en l'air si besoin. */
    private static void addDashLines(DashDefinition dash, List<Component> tooltip) {
        Component useKey = Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage();
        String distance = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(dash.distance());
        String cooldown = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(dash.cooldownSeconds());
        insertAfterStats(tooltip, abilityLine(Component.translatable(DASH_KEY, useKey, distance, cooldown)));
        if (dash.direction() == DashDirection.INPUT) {
            insertAfterStats(tooltip, abilityLine(Component.translatable(DASH_INPUT_KEY)));
        }
        if (dash.allowAirborne()) {
            insertAfterStats(tooltip, abilityLine(Component.translatable(DASH_AIRBORNE_KEY)));
        }
    }

    /**
     * Ex : "Clic droit : Parade (-50 % de dégâts de face)". La touche affichée est celle des contrôles du joueur
     * ("Utiliser l'objet/placer un bloc"), même s'il l'a changée.
     */
    private static void addParryLines(ParryDefinition parry, List<Component> tooltip) {
        Component useKey = Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage();
        String percent = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(parry.damageReduction());
        insertAfterStats(tooltip, abilityLine(Component.translatable(PARRY_KEY, useKey, percent)));
        if (parry.whileDualWielding()) {
            insertAfterStats(tooltip, abilityLine(Component.translatable(PARRY_DUAL_WIELD_KEY)));
        }
    }

    /** Ligne de capacité : indentée comme les stats, en doré pour la distinguer. */
    private static Component abilityLine(Component content) {
        return Component.literal(" ").append(content).withStyle(ChatFormatting.GOLD);
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
        insertAfterStats(tooltip, statLine(Component.translatable(VANILLA_STAT_FORMAT_KEY, reachText, Component.translatable(REACH_KEY))));
    }

    /**
     * Le jeu affiche le bonus de balayage en "+0.5" bleu : on le remplace par une ligne verte en pourcentage
     * ("50% Sweeping Damage Ratio"), avec le nom vanilla de la stat dans chaque langue.
     */
    private static void addSweepLine(WeaponDefinition definition, List<Component> tooltip) {
        tooltip.removeIf(line -> containsTranslationKey(line, VANILLA_SWEEP_KEY_PART));
        if (!definition.sweep().enabled() || definition.sweep().damageRatio() <= 0.0D) return;

        String percent = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(definition.sweep().damageRatio());
        Component name = Component.translatable(Attributes.SWEEPING_DAMAGE_RATIO.value().getDescriptionId());
        insertAfterStats(tooltip, statLine(Component.translatable(VANILLA_PERCENT_STAT_FORMAT_KEY, percent, name)));
    }

    /** Ex : "Au coup : Lenteur II (1.5 s)", avec le nom de l'effet traduit par le jeu. */
    private static void addEffectLine(OnHitEffect effect, List<Component> tooltip) {
        effect.holder().ifPresent(holder -> {
            MutableComponent name = Component.translatable(holder.value().getDescriptionId());
            if (effect.level() > 1) {
                // Même rendu que les potions vanilla : "Lenteur II"
                name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + (effect.level() - 1)));
            }
            String seconds = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(effect.durationSeconds());
            Component line = effect.chance() >= PERCENT
                    ? Component.translatable(ON_HIT_KEY, name, seconds)
                    : Component.translatable(ON_HIT_CHANCE_KEY, name, seconds,
                    ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(effect.chance()));
            insertAfterStats(tooltip, Component.literal(" ").append(line).withStyle(ChatFormatting.BLUE));
        });
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
     * Index juste après la dernière ligne de stat (vanilla ou ajoutée par le mod), ou -1 si aucune
     * (ex : stats masquées par un autre mod). On parcourt à l'envers pour trouver la dernière.
     */
    private static int findStatsEnd(List<Component> tooltip) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            Component line = tooltip.get(i);
            for (String marker : STAT_LINE_MARKERS) {
                if (containsTranslationKey(line, marker)) return i + 1;
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
