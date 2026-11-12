package com.betterweaponry.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Arc d'un matériau : l'arc vanilla, avec la vitesse de flèche et la durabilité du matériau. */
public class WeaponBowItem extends BowItem implements RangedWeapon {
    private static final int VANILLA_DURABILITY = 384;

    private final WeaponMaterial material;

    public WeaponBowItem(WeaponMaterial material, Item.Properties properties) {
        super(configure(material, properties));
        this.material = material;
    }

    private static Item.Properties configure(WeaponMaterial material, Item.Properties properties) {
        properties.durability(RangedWeapon.durabilityOf(material, GearType.BOW, VANILLA_DURABILITY));
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

    /** Un arc plus rapide atteint la pleine puissance plus tôt : le temps de bandage compte davantage. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int duration = getUseDuration(stack, entity);
        int drawn = duration - timeLeft;
        super.releaseUsing(stack, level, entity, duration - (int) Math.round(drawn * drawSpeed()));
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
