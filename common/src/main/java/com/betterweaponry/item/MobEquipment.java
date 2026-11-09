package com.betterweaponry.item;

import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.data.MobGearDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Équipe les mobs à leur apparition, après que le jeu leur a donné leur équipement vanilla. Le nombre de mobs armés ne
 * change donc pas : seule la nature de leur arme peut changer.
 */
public final class MobEquipment {
    private static final double PERCENT = 100.0D;
    private static final Map<Tier, WeaponMaterial> MATERIAL_OF_TIER = Map.of(Tiers.WOOD, WeaponMaterials.WOODEN, Tiers.STONE, WeaponMaterials.STONE,
            Tiers.IRON, WeaponMaterials.IRON, Tiers.GOLD, WeaponMaterials.GOLDEN, Tiers.DIAMOND, WeaponMaterials.DIAMOND, Tiers.NETHERITE, WeaponMaterials.NETHERITE);

    private MobEquipment() {
    }

    public static void onFreshSpawn(Mob mob, GearDefinition gear) {
        MobGearDefinition settings = gear.mobs();
        RandomSource random = mob.getRandom();
        boolean behaviors = WeaponData.rules().mobBehaviorsActive();
        ItemStack main = mob.getMainHandItem();

        if (behaviors && mob instanceof AbstractSkeleton && main.is(Items.BOW) && roll(random, settings.ninjaSkeletonChance())) {
            WeaponMaterial material = VanillaGear.rollMobMaterial(mob, gear, WeaponMaterials.STONE);
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.shuriken(material).get()));
        } else if (mob instanceof Enemy && isVanillaMeleeGear(main) && roll(random, settings.moddedWeaponChance())) {
            replaceWithModWeapon(mob, main, gear, settings);
        }

        if (behaviors && settings.captainShield() && mob instanceof PatrollingMonster patrol && patrol.isPatrolLeader() && mob.getOffhandItem().isEmpty()) {
            WeaponMaterial material = VanillaGear.rollMobMaterial(mob, gear, WeaponMaterials.STONE);
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.shield(material).get()));
            mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }
    }

    /** Une épée ou un outil vanilla d'un matériau connu : ce que les mobs reçoivent normalement en main. */
    private static boolean isVanillaMeleeGear(ItemStack stack) {
        return stack.getItem() instanceof TieredItem tiered && MATERIAL_OF_TIER.containsKey(tiered.getTier())
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
    }

    /** Garde les enchantements, et part du matériau de l'arme d'origine. La main secondaire ne se lâche jamais à la mort. */
    private static void replaceWithModWeapon(Mob mob, ItemStack original, GearDefinition gear, MobGearDefinition settings) {
        RandomSource random = mob.getRandom();
        WeaponMaterial material = VanillaGear.rollMobMaterial(mob, gear, MATERIAL_OF_TIER.get(((TieredItem) original.getItem()).getTier()));
        List<WeaponType> dualWielded = Arrays.stream(WeaponType.values()).filter(type -> WeaponData.weapon(type).dualWielded()).toList();
        boolean dualWield = WeaponData.rules().dualWieldActive() && !dualWielded.isEmpty() && roll(random, settings.dualWieldChance());
        WeaponType type = dualWield ? dualWielded.get(random.nextInt(dualWielded.size())) : WeaponType.values()[random.nextInt(WeaponType.values().length)];
        Item weapon = ModItems.get(type, material).get();
        mob.setItemSlot(EquipmentSlot.MAINHAND, VanillaGear.transmute(original, weapon, 1));
        if (dualWield) {
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(weapon));
            mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }
    }

    private static boolean roll(RandomSource random, double percent) {
        return random.nextDouble() * PERCENT < percent;
    }
}
