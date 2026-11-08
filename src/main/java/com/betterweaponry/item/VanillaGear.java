package com.betterweaponry.item;

import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Remplace l'équipement vanilla par les versions du mod. Un objet vanilla ne peut pas être retiré du jeu sans abîmer
 * les mondes existants : on coupe donc ses sources (butin, échanges, recette, onglet créatif) et on convertit ceux
 * que les joueurs possèdent déjà.
 */
public final class VanillaGear {
    private static final double PERCENT = 100.0D;
    /** Du moins bon au meilleur : chaque marche est tirée au sort dans le butin. */
    private static final List<WeaponMaterial> UPGRADE_LADDER = List.of(WeaponMaterials.WOODEN, WeaponMaterials.GOLDEN,
            WeaponMaterials.STONE, WeaponMaterials.IRON, WeaponMaterials.DIAMOND, WeaponMaterials.NETHERITE);

    private VanillaGear() {
    }

    /** Cet objet vanilla a une version du mod. */
    public static boolean isReplaced(ItemStack stack) {
        return stack.is(Items.SHIELD);
    }

    /** Version du mod dans ce matériau : enchantements et bannière conservés, usure proportionnelle à la durabilité. */
    public static ItemStack convert(ItemStack stack, WeaponMaterial material) {
        ItemStack result = stack.transmuteCopy(ModItems.shield(material).get(), stack.getCount());
        if (stack.isDamaged()) {
            double worn = (double) stack.getDamageValue() / stack.getMaxDamage();
            result.setDamageValue(Math.min(result.getMaxDamage() - 1, (int) Math.round(worn * result.getMaxDamage())));
        }
        return result;
    }

    /** Matériau tiré au sort : on part du bois, et chaque marche a {@code upgradeChance} % de chance de monter. */
    public static WeaponMaterial rollMaterial(RandomSource random, double upgradeChance) {
        int step = 0;
        while (step < UPGRADE_LADDER.size() - 1 && random.nextDouble() * PERCENT < upgradeChance) step++;
        return UPGRADE_LADDER.get(step);
    }

    /** Remplace l'équipement vanilla d'un butin généré. */
    public static void replaceInLoot(List<ItemStack> loot, RandomSource random, GearDefinition gear) {
        if (!gear.replaceVanillaGear()) return;
        loot.replaceAll(stack -> isReplaced(stack) ? convert(stack, rollMaterial(random, gear.lootUpgradeChance())) : stack);
    }

    /** Convertit ce que le joueur possède (inventaire et coffre de l'Ender) en version en bois. */
    public static void convertInventory(Player player) {
        convertContainer(player.getInventory());
        convertContainer(player.getEnderChestInventory());
    }

    private static void convertContainer(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isReplaced(stack)) container.setItem(slot, convert(stack, WeaponMaterials.WOODEN));
        }
    }
}
