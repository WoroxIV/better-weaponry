package com.betterweaponry.item;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Arbalète d'un matériau : l'arbalète vanilla, avec la durabilité du matériau.
 * La vitesse de carreau et le temps de chargement de l'or se règlent dans CrossbowItemMixin (méthodes statiques du jeu).
 */
public class WeaponCrossbowItem extends CrossbowItem implements RangedWeapon {
    private static final int VANILLA_DURABILITY = 465;

    private final WeaponMaterial material;

    public WeaponCrossbowItem(WeaponMaterial material, Item.Properties properties) {
        super(configure(material, properties));
        this.material = material;
    }

    private static Item.Properties configure(WeaponMaterial material, Item.Properties properties) {
        properties.durability(RangedWeapon.durabilityOf(material, GearType.CROSSBOW, VANILLA_DURABILITY));
        if (material.isFireResistant()) properties.fireResistant();
        return properties;
    }

    @Override
    public WeaponMaterial getMaterial() {
        return material;
    }

    @Override
    public int getEnchantmentValue() {
        return material.tier().getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return material.tier().getRepairIngredient().test(repair);
    }
}
