package com.betterweaponry.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Bouclier d'un matériau. Il se comporte comme le bouclier vanilla (blocage, bannières, enchantements à l'enclume) ;
 * seuls sa durabilité, sa réparation et son apparence dépendent du matériau.
 */
public class WeaponShieldItem extends ShieldItem {
    /** Le bois et l'or auraient sinon la durabilité très faible de leurs outils (59 et 32). */
    private static final int WOODEN_DURABILITY = 118;
    private static final int GOLDEN_DURABILITY = 64;

    private final WeaponMaterial material;

    public WeaponShieldItem(WeaponMaterial material, Item.Properties properties) {
        super(configure(material, properties));
        this.material = material;
    }

    private static Item.Properties configure(WeaponMaterial material, Item.Properties properties) {
        properties.durability(durabilityOf(material));
        if (material.isFireResistant()) properties.fireResistant();
        return properties;
    }

    /** La durabilité des outils du matériau, sauf pour les boucliers rudimentaires en bois et en or. */
    public static int durabilityOf(WeaponMaterial material) {
        if (material.gearDurability(GearType.SHIELD).isPresent()) return material.gearDurability(GearType.SHIELD).getAsInt();
        if (material == WeaponMaterials.WOODEN) return WOODEN_DURABILITY;
        if (material == WeaponMaterials.GOLDEN) return GOLDEN_DURABILITY;
        return material.tier().getUses();
    }

    public WeaponMaterial getMaterial() {
        return material;
    }

    /** Se répare à l'enclume avec le matériau du bouclier, et non avec des planches. */
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return material.tier().getRepairIngredient().test(repair);
    }

    /** Le vanilla a un nom par couleur de bannière ("Bouclier rouge") : ici le matériau suffit à nommer l'objet. */
    @Override
    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }
}
