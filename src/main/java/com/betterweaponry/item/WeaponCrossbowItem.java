package com.betterweaponry.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Arbalète d'un matériau : l'arbalète vanilla, avec la vitesse de carreau et la durabilité du matériau.
 * Le temps de chargement de l'or est réglé dans CrossbowItemMixin (méthode statique du jeu).
 */
public class WeaponCrossbowItem extends CrossbowItem implements RangedWeapon {
    private static final int VANILLA_DURABILITY = 465;

    private final WeaponMaterial material;

    public WeaponCrossbowItem(WeaponMaterial material, Item.Properties properties) {
        super(configure(material, properties));
        this.material = material;
    }

    private static Item.Properties configure(WeaponMaterial material, Item.Properties properties) {
        properties.durability(RangedWeapon.durabilityOf(material, VANILLA_DURABILITY)).component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        if (material.isFireResistant()) properties.fireResistant();
        return properties;
    }

    @Override
    public WeaponMaterial getMaterial() {
        return material;
    }

    @Override
    protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> projectileItems,
                         float velocity, float inaccuracy, boolean isCrit, @Nullable LivingEntity target) {
        super.shoot(level, shooter, hand, weapon, projectileItems, velocityFor(shooter, velocity), inaccuracy, isCrit, target);
    }

    @Override
    public int getEnchantmentValue() {
        return material.tier().getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return material.tier().getRepairIngredient().test(repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addSpeedLine(tooltip);
    }
}
