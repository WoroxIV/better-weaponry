package com.betterweaponry.combat;

import com.betterweaponry.item.WeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** Un enchantement du mod : tout ce qui varie est donné à la construction (voir {@link ModEnchantments#init()}). */
public final class ModEnchantment extends Enchantment {
    /**
     * @param minBase     coût minimal au niveau 1
     * @param minPerLevel ajout par niveau
     * @param maxBase     coût maximal au niveau 1
     * @param maxPerLevel ajout par niveau
     * @param maxLevel    niveau le plus haut
     * @param treasure    jamais proposé par la table d'enchantement
     */
    public record Costs(int minBase, int minPerLevel, int maxBase, int maxPerLevel, int maxLevel, boolean treasure) {
    }

    private final Costs costs;
    private final Predicate<ItemStack> accepted;
    private final Set<ResourceLocation> exclusiveWith;

    ModEnchantment(Rarity rarity, EquipmentSlot[] slots, Costs costs, Predicate<ItemStack> accepted, Set<ResourceLocation> exclusiveWith) {
        super(rarity, EnchantmentCategory.WEAPON, slots);
        this.costs = costs;
        this.accepted = accepted;
        this.exclusiveWith = exclusiveWith;
    }

    @Override
    public int getMaxLevel() {
        return costs.maxLevel();
    }

    @Override
    public int getMinCost(int level) {
        return costs.minBase() + (level - 1) * costs.minPerLevel();
    }

    @Override
    public int getMaxCost(int level) {
        return costs.maxBase() + (level - 1) * costs.maxPerLevel();
    }

    @Override
    public boolean isTreasureOnly() {
        return costs.treasure();
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && !exclusiveWith.contains(BuiltInRegistries.ENCHANTMENT.getKey(other));
    }

    /** Une arme du mod suit sa capacité configurée ; le reste dépend du type d'objet. */
    @Override
    public boolean canEnchant(ItemStack stack) {
        if (stack.getItem() instanceof WeaponItem weapon) {
            Optional<Boolean> byAbility = ModEnchantments.supportedByAbility(weapon.definition(), this);
            if (byAbility.isPresent()) return byAbility.get();
        }
        return accepted.test(stack);
    }

    /** Méthode de Forge : la table d'enchantement suit les mêmes règles que l'enclume. */
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canEnchant(stack);
    }
}
