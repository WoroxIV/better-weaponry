package com.betterweaponry.event;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.ModKeyMappings;
import com.betterweaponry.client.TooltipLayout;
import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.DashAbility;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.WeaponProfiles;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.config.TooltipCategory;
import com.betterweaponry.config.TooltipPlacement;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.ComboStep;
import com.betterweaponry.data.DashDefinition;
import com.betterweaponry.data.DashDirection;
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.ParryDefinition;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tooltip des armes : rassemble les lignes (stats, deux mains, effets, capacité, combo) par catégorie, puis
 * {@link TooltipLayout} les range selon les préférences du joueur (page Tooltips de l'éditeur).
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class TooltipEventHandler {
    private static final double PLAYER_BASE_REACH = 3.0D;
    private static final double PERCENT = 100.0D;

    private static final String REACH_KEY = "attribute.betterweaponry.attack_reach";
    private static final String TWO_HANDED_KEY = "attribute.betterweaponry.two_handed";
    private static final String DUAL_WIELDED_KEY = "attribute.betterweaponry.dual_wielded";
    private static final String DISABLES_SHIELD_KEY = "attribute.betterweaponry.disables_shield";
    private static final String HOLD_FOR_DETAILS_KEY = BetterWeaponry.MODID + ".tooltip.hold_for_details";
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

    private TooltipEventHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        Optional<WeaponDefinition> profile = WeaponProfiles.definition(stack);
        if (profile.isEmpty()) {
            addReachToOtherItem(stack, tooltip);
            return;
        }

        RulesDefinition rules = WeaponData.rules();
        WeaponDefinition definition = profile.get();
        Map<TooltipCategory, List<Component>> lines = new EnumMap<>(TooltipCategory.class);
        // Les stats vanilla sont sorties du tooltip pour être replacées avec les autres.
        lines.put(TooltipCategory.DAMAGE, TooltipLayout.takeLines(tooltip, "attack_damage"));
        lines.put(TooltipCategory.SPEED, TooltipLayout.takeLines(tooltip, "attack_speed"));
        lines.put(TooltipCategory.ARMOR_PENETRATION, TooltipLayout.takeLines(tooltip, "armor_penetration"));
        // Sans joueur (aperçu au menu principal), le jeu affiche l'écart avec la vitesse de base au lieu de la vitesse réelle.
        if (event.getEntity() == null && stack.getItem() instanceof WeaponItem) {
            lines.put(TooltipCategory.SPEED, List.of(Component.literal(" ").append(Component.translatable(VANILLA_STAT_FORMAT_KEY,
                    number(definition.attackSpeed()), Component.translatable(Attributes.ATTACK_SPEED.value().getDescriptionId())))));
        }
        TooltipLayout.takeLines(tooltip, VANILLA_REACH_KEY_PART);
        TooltipLayout.takeLines(tooltip, VANILLA_SWEEP_KEY_PART);

        if (WeaponItem.isTwoHanded(stack)) {
            add(lines, TooltipCategory.HANDS, Component.translatable(TWO_HANDED_KEY));
        } else if (rules.dualWieldActive() && DualWieldSystem.canDualWield(stack)) {
            add(lines, TooltipCategory.HANDS, Component.translatable(DUAL_WIELDED_KEY));
        }
        if (rules.reachActive()) add(lines, TooltipCategory.REACH, reachLine(totalReach(stack)));
        if (rules.sweepActive() && definition.sweep().enabled() && definition.sweep().damageRatio() > 0.0D) {
            add(lines, TooltipCategory.SWEEP, sweepLine(definition));
        }
        if (rules.shieldDisablingActive() && definition.disablesShield()) {
            add(lines, TooltipCategory.SHIELD_DISABLING, Component.translatable(DISABLES_SHIELD_KEY));
        }
        if (rules.onHitEffectsActive()) {
            definition.onHitEffects().forEach(effect -> addEffectLine(effect, lines));
        }
        ParrySystem.parryOf(stack).ifPresent(parry -> addParryLines(parry, lines));
        DashAbility.dashOf(stack).ifPresent(dash -> addDashLines(dash, lines));
        ComboSystem.comboOf(stack).ifPresent(combo -> addComboLines(combo, lines));

        Component hint = Component.translatable(HOLD_FOR_DETAILS_KEY, ModKeyMappings.weaponDetailsKeyName()).withStyle(ChatFormatting.DARK_GRAY);
        TooltipLayout.arrange(tooltip, lines, new TooltipLayout.Settings(
                category -> ClientConfig.get(ClientConfig.placement(category)),
                category -> ClientConfig.get(ClientConfig.color(category))), ModKeyMappings.weaponDetailsHeld(), hint);
    }

    /** Une ligne indentée d'un espace, comme les stats vanilla. */
    private static void add(Map<TooltipCategory, List<Component>> lines, TooltipCategory category, Component content) {
        lines.computeIfAbsent(category, ignored -> new ArrayList<>()).add(Component.literal(" ").append(content));
    }

    /** Objets sans profil d'arme : la ligne verte de portée, pour les armes de mêlée et les objets qui modifient la portée. */
    private static void addReachToOtherItem(ItemStack stack, List<Component> tooltip) {
        if (!DualWieldSystem.isMeleeWeapon(stack) && !hasReachModifier(stack)) return;
        TooltipPlacement placement = ClientConfig.get(ClientConfig.placement(TooltipCategory.REACH));
        if (placement == TooltipPlacement.HIDDEN) return;
        int header = TooltipLayout.headerIndex(tooltip);
        if (header < 0) return;
        TooltipLayout.takeLines(tooltip, VANILLA_REACH_KEY_PART);
        Component line = Component.literal(" ").append(reachLine(totalReach(stack)));
        tooltip.add(header + 1, TooltipLayout.colored(line, ClientConfig.get(ClientConfig.color(TooltipCategory.REACH))));
    }

    private static boolean hasReachModifier(ItemStack stack) {
        return stack.getAttributeModifiers().modifiers().stream().anyMatch(TooltipEventHandler::isReachBonus);
    }

    private static double totalReach(ItemStack stack) {
        double total = PLAYER_BASE_REACH;
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (isReachBonus(entry)) total += entry.modifier().amount();
        }
        return total;
    }

    private static boolean isReachBonus(ItemAttributeModifiers.Entry entry) {
        return entry.attribute().equals(Attributes.ENTITY_INTERACTION_RANGE) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE;
    }

    /** Format vanilla "#.##" : affiche "3.5" ou "4" et évite les "3.6000000000000005" des calculs en double. */
    private static String number(double value) {
        return ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value);
    }

    private static Component reachLine(double reach) {
        return Component.translatable(VANILLA_STAT_FORMAT_KEY, number(reach), Component.translatable(REACH_KEY));
    }

    /** "50% Sweeping Damage Ratio", avec le nom vanilla de la stat dans chaque langue. */
    private static Component sweepLine(WeaponDefinition definition) {
        Component name = Component.translatable(Attributes.SWEEPING_DAMAGE_RATIO.value().getDescriptionId());
        return Component.translatable(VANILLA_PERCENT_STAT_FORMAT_KEY, number(definition.sweep().damageRatio()), name);
    }

    /** Ex : "Au coup : Lenteur II (1.5 s)", avec le nom de l'effet traduit par le jeu. */
    private static void addEffectLine(OnHitEffect effect, Map<TooltipCategory, List<Component>> lines) {
        effect.holder().ifPresent(holder -> {
            MutableComponent name = Component.translatable(holder.value().getDescriptionId());
            if (effect.level() > 1) {
                // Même rendu que les potions vanilla : "Lenteur II"
                name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + (effect.level() - 1)));
            }
            String seconds = number(effect.durationSeconds());
            add(lines, TooltipCategory.EFFECTS, effect.chance() >= PERCENT
                    ? Component.translatable(ON_HIT_KEY, name, seconds)
                    : Component.translatable(ON_HIT_CHANCE_KEY, name, seconds, number(effect.chance())));
        });
    }

    /**
     * Ex : "Clic droit : Parade (-50 % de dégâts de face)". La touche affichée est celle des contrôles du joueur
     * ("Utiliser l'objet/placer un bloc"), même s'il l'a changée.
     */
    private static void addParryLines(ParryDefinition parry, Map<TooltipCategory, List<Component>> lines) {
        Component useKey = Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage();
        add(lines, TooltipCategory.ABILITY, Component.translatable(PARRY_KEY, useKey, number(parry.damageReduction())));
        if (WeaponData.rules().perfectParryActive() && parry.perfectWindowSeconds() > 0.0D) {
            add(lines, TooltipCategory.ABILITY, Component.translatable(PARRY_KEY + ".perfect", number(parry.perfectWindowSeconds())));
            if (parry.perfectDamageBonus() > 0.0D) {
                add(lines, TooltipCategory.ABILITY, Component.translatable(PARRY_KEY + ".perfect_bonus", number(parry.perfectDamageBonus())));
            }
        }
        if (parry.whileDualWielding()) add(lines, TooltipCategory.ABILITY, Component.translatable(PARRY_DUAL_WIELD_KEY));
    }

    /** Ex : "Clic droit : Dash (~3.5 blocs, recharge 1.5 s)", puis la direction et le dash en l'air si besoin. */
    private static void addDashLines(DashDefinition dash, Map<TooltipCategory, List<Component>> lines) {
        Component useKey = Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage();
        add(lines, TooltipCategory.ABILITY, Component.translatable(DASH_KEY, useKey, number(dash.distance()), number(dash.cooldownSeconds())));
        if (dash.direction() == DashDirection.INPUT) add(lines, TooltipCategory.ABILITY, Component.translatable(DASH_INPUT_KEY));
        if (dash.allowAirborne()) add(lines, TooltipCategory.ABILITY, Component.translatable(DASH_AIRBORNE_KEY));
    }

    /**
     * Une ligne par coup spécial du combo, ex : "Combo, coup 3 : zone 360°, 3.5 blocs, recharge ×1.6",
     * puis "Combo avec la même arme dans chaque main" si besoin.
     */
    private static void addComboLines(ComboDefinition combo, Map<TooltipCategory, List<Component>> lines) {
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
            add(lines, TooltipCategory.COMBO, Component.translatable(COMBO_STEP_KEY, i + 1, description));
        }
        if (combo.requiresDualWield()) add(lines, TooltipCategory.COMBO, Component.translatable(COMBO_REQUIRES_DUAL_WIELD_KEY));
    }
}
