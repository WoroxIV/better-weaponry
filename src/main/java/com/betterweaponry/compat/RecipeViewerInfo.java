package com.betterweaponry.compat;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ModEnchantments;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Pages d'information de JEI et d'EMI : ce que font nos enchantements et les capacités de chaque objet.
 * Cette classe ne dépend d'aucun des deux mods : leurs plugins ne font qu'afficher ce qu'elle renvoie.
 */
public final class RecipeViewerInfo {
    private static final String KEY = BetterWeaponry.MODID + ".info.";

    private RecipeViewerInfo() {
    }

    /** Une page : les objets concernés et ses lignes. */
    public record Entry(String id, List<ItemStack> stacks, List<Component> lines) {
    }

    public static List<Entry> entries(RegistryAccess registries) {
        List<Entry> entries = new ArrayList<>();
        for (ResourceKey<Enchantment> key : ModEnchantments.ALL) {
            registries.registryOrThrow(Registries.ENCHANTMENT).getHolder(key).ifPresent(holder -> entries.add(enchantmentEntry(key, holder)));
        }
        RulesDefinition rules = WeaponData.rules();
        for (WeaponType type : WeaponType.values()) {
            List<Component> lines = weaponLines(WeaponData.weapon(type), rules);
            if (!lines.isEmpty()) entries.add(new Entry(type.id(), stacks(ModItems.allOfType(type)), lines));
        }
        entries.add(new Entry("shield", stacks(ModItems.allShields()), List.of(Component.translatable(KEY + (rules.perfectParryActive() ? "shield" : "shield.simple")))));
        entries.add(new Entry("bow", stacks(ModItems.allBows()), List.of(Component.translatable(KEY + "ranged"))));
        entries.add(new Entry("crossbow", stacks(ModItems.allCrossbows()), List.of(Component.translatable(KEY + "ranged"))));
        entries.add(new Entry("shuriken", stacks(ModItems.allShurikens()), List.of(Component.translatable(KEY + "shuriken"))));
        return entries;
    }

    /** Un livre par niveau : JEI et EMI les listent séparément. */
    private static Entry enchantmentEntry(ResourceKey<Enchantment> key, Holder<Enchantment> enchantment) {
        List<ItemStack> books = new ArrayList<>();
        for (int level = enchantment.value().getMinLevel(); level <= enchantment.value().getMaxLevel(); level++) {
            books.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
        }
        return new Entry(key.location().getPath(), books, List.of(Component.translatable("enchantment." + BetterWeaponry.MODID + "." + key.location().getPath() + ".desc")));
    }

    /** Seulement ce qui est actif : une capacité coupée dans les règles (ou par un autre mod de combat) n'est pas décrite. */
    private static List<Component> weaponLines(WeaponDefinition weapon, RulesDefinition rules) {
        List<Component> lines = new ArrayList<>();
        if (rules.twoHandedActive() && weapon.twoHanded()) lines.add(Component.translatable(KEY + "two_handed"));
        if (rules.dualWieldActive() && weapon.dualWielded()) lines.add(Component.translatable(KEY + "dual_wielded"));
        if (rules.parryActive() && weapon.ability() == WeaponAbility.PARRY) lines.add(Component.translatable(KEY + "parry"));
        if (rules.dashActive() && weapon.ability() == WeaponAbility.DASH) lines.add(Component.translatable(KEY + "dash"));
        if (rules.combosActive() && weapon.combo().enabled()) lines.add(Component.translatable(KEY + "combo"));
        return lines;
    }

    private static List<ItemStack> stacks(Collection<? extends DeferredItem<?>> items) {
        return items.stream().map(item -> new ItemStack(item.get())).toList();
    }
}
