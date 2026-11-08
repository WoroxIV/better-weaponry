package com.betterweaponry.item;

import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.registry.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Remplace l'équipement vanilla par les versions du mod. Un objet vanilla ne peut pas être retiré du jeu sans abîmer
 * les mondes existants : on coupe donc ses sources (butin, échanges, recette, onglet créatif) et on convertit ceux
 * que les joueurs possèdent déjà.
 */
public final class VanillaGear {
    private static final double PERCENT = 100.0D;
    private static final double HARD_DIFFICULTY_BONUS = 1.5D;
    /** Du moins bon au meilleur : chaque marche est tirée au sort dans le butin. */
    private static final List<WeaponMaterial> UPGRADE_LADDER = List.of(WeaponMaterials.WOODEN, WeaponMaterials.GOLDEN,
            WeaponMaterials.STONE, WeaponMaterials.IRON, WeaponMaterials.DIAMOND, WeaponMaterials.NETHERITE);

    private VanillaGear() {
    }

    /** Cet objet vanilla a une version du mod. */
    public static boolean isReplaced(ItemStack stack) {
        return stack.is(Items.SHIELD) || stack.is(Items.BOW) || stack.is(Items.CROSSBOW);
    }

    /** Version du mod dans ce matériau : enchantements et bannière conservés, usure proportionnelle à la durabilité. */
    public static ItemStack convert(ItemStack stack, WeaponMaterial material) {
        ItemStack result = stack.transmuteCopy(replacement(stack, material), stack.getCount());
        if (stack.isDamaged()) {
            double worn = (double) stack.getDamageValue() / stack.getMaxDamage();
            result.setDamageValue(Math.min(result.getMaxDamage() - 1, (int) Math.round(worn * result.getMaxDamage())));
        }
        return result;
    }

    private static Item replacement(ItemStack stack, WeaponMaterial material) {
        if (stack.is(Items.BOW)) return ModItems.bow(material).get();
        if (stack.is(Items.CROSSBOW)) return ModItems.crossbow(material).get();
        return ModItems.shield(material).get();
    }

    /** Matériau tiré au sort : on part du bois, et chaque marche a {@code upgradeChance} % de chance de monter. */
    public static WeaponMaterial rollMaterial(RandomSource random, double upgradeChance) {
        return rollMaterial(random, upgradeChance, WeaponMaterials.WOODEN);
    }

    /** Même tirage, en partant de {@code lowest}. */
    public static WeaponMaterial rollMaterial(RandomSource random, double upgradeChance, WeaponMaterial lowest) {
        return rollMaterial(random, upgradeChance, lowest, WeaponMaterials.NETHERITE);
    }

    /** Même tirage, sans jamais dépasser {@code highest}. */
    public static WeaponMaterial rollMaterial(RandomSource random, double upgradeChance, WeaponMaterial lowest, WeaponMaterial highest) {
        int step = UPGRADE_LADDER.indexOf(lowest);
        int top = Math.max(step, UPGRADE_LADDER.indexOf(highest));
        while (step < top && random.nextDouble() * PERCENT < upgradeChance) step++;
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

    /**
     * L'arme vanilla d'un mob devient une arme du mod : un peu plus souvent d'un meilleur matériau en difficulté Difficile.
     * On part de la pierre, qui tire comme le vanilla : un squelette ordinaire reste aussi dangereux qu'avant.
     */
    public static void convertMobHands(Mob mob, GearDefinition gear) {
        if (!gear.replaceVanillaGear()) return;
        for (EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (isReplaced(stack)) mob.setItemSlot(slot, convert(stack, rollMobMaterial(mob, gear, WeaponMaterials.STONE)));
        }
    }

    /** Matériau de l'équipement d'un mob : un peu plus souvent meilleur en Difficile, plafonné selon la dimension et la difficulté. */
    public static WeaponMaterial rollMobMaterial(Mob mob, GearDefinition gear, WeaponMaterial lowest) {
        boolean hard = mob.level().getDifficulty() == Difficulty.HARD;
        double chance = gear.mobs().upgradeChance() * (hard ? HARD_DIFFICULTY_BONUS : 1.0D);
        return rollMaterial(mob.getRandom(), chance, lowest, materialCap(mob.level().dimension(), hard));
    }

    /** Meilleur matériau possible sur un mob : le fer dans l'Overworld, le diamant dans l'End et le Nether, un cran de plus en Difficile. */
    public static WeaponMaterial materialCap(ResourceKey<Level> dimension, boolean hard) {
        if (dimension == Level.NETHER) return hard ? WeaponMaterials.NETHERITE : WeaponMaterials.DIAMOND;
        if (dimension == Level.END) return WeaponMaterials.DIAMOND;
        return hard ? WeaponMaterials.DIAMOND : WeaponMaterials.IRON;
    }

    private static void convertContainer(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isReplaced(stack)) container.setItem(slot, convert(stack, WeaponMaterials.WOODEN));
        }
    }
}
