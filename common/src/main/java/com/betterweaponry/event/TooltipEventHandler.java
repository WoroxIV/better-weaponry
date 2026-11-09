package com.betterweaponry.event;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import com.betterweaponry.platform.Platform;
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
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.RangedWeapon;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tooltip des armes : rassemble les lignes (stats, deux mains, effets, capacité, combo) par catégorie, puis
 * {@link TooltipLayout} les range selon les préférences du joueur (page Tooltips de l'éditeur).
 */
public final class TooltipEventHandler {
    private static final double PLAYER_BASE_REACH = 3.0D;
    private static final double PERCENT = 100.0D;

    private static final String REACH_KEY = "attribute.betterweaponry.attack_reach";
    private static final String TWO_HANDED_KEY = "attribute.betterweaponry.two_handed";
    private static final String DUAL_WIELDED_KEY = "attribute.betterweaponry.dual_wielded";
    private static final String DISABLES_SHIELD_KEY = "attribute.betterweaponry.disables_shield";
    private static final String HOLD_FOR_DETAILS_KEY = BetterWeaponry.MODID + ".tooltip.hold_for_details";
    private static final String PROJECTILE_SPEED_KEY = BetterWeaponry.MODID + ".tooltip.projectile_speed";
    private static final String ON_HIT_KEY = BetterWeaponry.MODID + ".tooltip.on_hit_effect";
    private static final String ON_HIT_CHANCE_KEY = ON_HIT_KEY + ".chance";
    private static final String ABILITY_KEY = BetterWeaponry.MODID + ".tooltip.ability";
    private static final String PARRY_KEY = ABILITY_KEY + ".parry";
    private static final String DASH_KEY = ABILITY_KEY + ".dash";
    private static final String DASH_AIRBORNE_KEY = DASH_KEY + ".airborne";
    private static final String COMBO_KEY = BetterWeaponry.MODID + ".tooltip.combo";
    private static final String COMBO_STEP_KEY = COMBO_KEY + ".step";
    private static final String COMBO_SEPARATOR_KEY = COMBO_KEY + ".separator";
    // Formats vanilla d'une stat de base, nombre puis pourcentage ("%s %s" en anglais, "%s en %s" en français,
    // "%2$s: %1$s" en italien...) : l'ordre des mots suit la langue.
    private static final String VANILLA_STAT_FORMAT_KEY = "attribute.modifier.equals.0";
    private static final String VANILLA_PERCENT_STAT_FORMAT_KEY = "attribute.modifier.equals.1";

    private TooltipEventHandler() {
    }

    /** @param player null dans les aperçus au menu principal */
    public static void onTooltip(ItemStack stack, List<Component> tooltip, @Nullable Player player) {
        if (stack.getItem() instanceof RangedWeapon ranged) {
            Map<TooltipCategory, List<Component>> speed = new EnumMap<>(TooltipCategory.class);
            add(speed, TooltipCategory.PROJECTILE_SPEED, Component.translatable(PROJECTILE_SPEED_KEY, number(ranged.speedMultiplier())));
            arrange(tooltip, speed);
            return;
        }
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
        lines.put(TooltipCategory.ARMOR_PENETRATION, armorPenetrationLines(tooltip, definition));
        // Sans joueur (aperçu au menu principal), le jeu n'ajoute pas les valeurs de base du joueur aux dégâts et à la vitesse.
        if (player == null && stack.getItem() instanceof WeaponItem weapon) {
            lines.put(TooltipCategory.DAMAGE, List.of(statLine(definition.damageFor(weapon.getMaterial()), Attributes.ATTACK_DAMAGE)));
            lines.put(TooltipCategory.SPEED, List.of(statLine(definition.attackSpeed(), Attributes.ATTACK_SPEED)));
        }
        TooltipLayout.takeLines(tooltip, reachKey());
        TooltipLayout.takeLines(tooltip, ModAttributes.SWEEPING_DAMAGE_RATIO.get().getDescriptionId());

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
        ParrySystem.parryOf(stack).ifPresent(parry -> add(lines, TooltipCategory.ABILITY, Component.translatable(PARRY_KEY)));
        DashAbility.dashOf(stack).ifPresent(dash -> addDashLines(dash, lines));
        // Sans dual wield, le combo d'une arme ambidextre n'existe pas : rien à annoncer.
        if (rules.dualWieldActive() || !DualWieldSystem.canDualWield(stack)) {
            ComboSystem.comboOf(stack).ifPresent(combo -> addComboLines(combo, definition, lines));
        }

        arrange(tooltip, lines);
    }

    /** Range les lignes selon les préférences du joueur. */
    private static void arrange(List<Component> tooltip, Map<TooltipCategory, List<Component>> lines) {
        Component hint = Component.translatable(HOLD_FOR_DETAILS_KEY, ModKeyMappings.weaponDetailsKeyName()).withStyle(ChatFormatting.DARK_GRAY);
        TooltipLayout.arrange(tooltip, lines, new TooltipLayout.Settings(
                category -> ClientConfig.get(ClientConfig.placement(category)),
                category -> ClientConfig.get(ClientConfig.color(category))), ModKeyMappings.weaponDetailsHeld(), hint);
    }

    /** Une stat indentée, au format vanilla de chaque langue. */
    private static Component statLine(double value, Attribute attribute) {
        return Component.literal(" ").append(Component.translatable(VANILLA_STAT_FORMAT_KEY, number(value), Component.translatable(attribute.getDescriptionId())));
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
        TooltipLayout.takeLines(tooltip, reachKey());
        Component line = Component.literal(" ").append(reachLine(totalReach(stack)));
        tooltip.add(header + 1, TooltipLayout.colored(line, ClientConfig.get(ClientConfig.color(TooltipCategory.REACH))));
    }

    private static boolean hasReachModifier(ItemStack stack) {
        return !reachBonuses(stack).isEmpty();
    }

    private static double totalReach(ItemStack stack) {
        double total = PLAYER_BASE_REACH;
        for (AttributeModifier modifier : reachBonuses(stack)) total += modifier.getAmount();
        return total;
    }

    private static List<AttributeModifier> reachBonuses(ItemStack stack) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Platform.get().entityReachAttribute()).stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION).toList();
    }

    private static String reachKey() {
        return Platform.get().entityReachAttribute().getDescriptionId();
    }

    /** Le jeu affiche "+0.3" : remplacée par une ligne verte en pourcentage, comme les autres stats de l'arme. */
    private static List<Component> armorPenetrationLines(List<Component> tooltip, WeaponDefinition definition) {
        Attribute attribute = ModAttributes.armorPenetration();
        if (TooltipLayout.takeLines(tooltip, attribute.getDescriptionId()).isEmpty()) return List.of();
        return List.of(Component.literal(" ").append(Component.translatable(VANILLA_PERCENT_STAT_FORMAT_KEY, number(definition.armorPenetration()),
                Component.translatable(attribute.getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN));
    }

    /** Format vanilla "#.##" : affiche "3.5" ou "4" et évite les "3.6000000000000005" des calculs en double. */
    private static String number(double value) {
        return ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value);
    }

    private static Component reachLine(double reach) {
        return Component.translatable(VANILLA_STAT_FORMAT_KEY, number(reach), Component.translatable(REACH_KEY));
    }

    /** "50% Sweeping Damage Ratio", avec le nom vanilla de la stat dans chaque langue. */
    private static Component sweepLine(WeaponDefinition definition) {
        Component name = Component.translatable(ModAttributes.SWEEPING_DAMAGE_RATIO.get().getDescriptionId());
        return Component.translatable(VANILLA_PERCENT_STAT_FORMAT_KEY, number(definition.sweep().damageRatio()), name);
    }

    /** Ex : "Coup : Lenteur II 1.5 s", avec le nom de l'effet traduit par le jeu. */
    private static void addEffectLine(OnHitEffect effect, Map<TooltipCategory, List<Component>> lines) {
        effect.mobEffect().ifPresent(mobEffect -> {
            MutableComponent name = Component.translatable(mobEffect.getDescriptionId());
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

    /** Ex : "Dash : ~3.5 blocs, 1.5 s", avec "(air)" à la fin s'il marche aussi en l'air. */
    private static void addDashLines(DashDefinition dash, Map<TooltipCategory, List<Component>> lines) {
        add(lines, TooltipCategory.ABILITY, Component.translatable(dash.allowAirborne() ? DASH_AIRBORNE_KEY : DASH_KEY, number(dash.distance()), number(dash.cooldownSeconds())));
    }

    /** Une ligne par coup spécial du combo, réduite à l'essentiel, ex : "Combo 3 : 360°, dégâts ×1.6". */
    private static void addComboLines(ComboDefinition combo, WeaponDefinition definition, Map<TooltipCategory, List<Component>> lines) {
        List<ComboStep> steps = combo.steps();
        for (int i = 0; i < steps.size(); i++) {
            ComboStep step = steps.get(i);
            List<Component> parts = new ArrayList<>();
            if (widensAttack(step, definition)) parts.add(Component.translatable(COMBO_KEY + ".area", number(step.angle())));
            if (step.damageMultiplier() != 1.0D) parts.add(Component.translatable(COMBO_KEY + ".damage", number(step.damageMultiplier())));
            if (parts.isEmpty()) continue;

            MutableComponent description = Component.empty();
            for (int part = 0; part < parts.size(); part++) {
                if (part > 0) description.append(Component.translatable(COMBO_SEPARATOR_KEY));
                description.append(parts.get(part));
            }
            add(lines, TooltipCategory.COMBO, Component.translatable(COMBO_STEP_KEY, i + 1, description));
        }
    }

    /** Le coup touche une zone plus grande que le coup normal de l'arme (son coup balayé, s'il en a un). */
    private static boolean widensAttack(ComboStep step, WeaponDefinition definition) {
        boolean sweeps = WeaponData.rules().sweepActive() && definition.sweep().enabled();
        return step.hasArea() && (!sweeps || step.radius() > definition.sweep().radius());
    }
}
