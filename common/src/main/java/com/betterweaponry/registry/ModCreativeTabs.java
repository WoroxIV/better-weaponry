package com.betterweaponry.registry;

import java.util.function.Supplier;
import com.betterweaponry.platform.Registered;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.RangedWeapon;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponShieldItem;
import com.betterweaponry.item.WeaponType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Onglet créatif du mod : boucliers, armes de mêlée de la plus courte à la plus longue, puis arcs, arbalètes et shurikens. */
public final class ModCreativeTabs {
    public static final Registered<CreativeModeTab, CreativeModeTab> WEAPONS = Platform.get().register(Registries.CREATIVE_MODE_TAB, "weapons", () -> Platform.get().creativeTabBuilder()
            .title(Component.translatable("itemGroup.better_weaponry.weapons"))
            .icon(() -> new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get()))
            .displayItems((parameters, output) -> displayOrder().forEach(output::accept))
            .build());

    private ModCreativeTabs() {
    }

    /** Force la déclaration de l'onglet. */
    public static void init() {
    }

    /**
     * Objets de l'onglet, dans l'ordre. La portée vient des données : un changement dans l'éditeur déplace l'arme.
     * Les matériaux des addons n'y sont pas : chaque addon les montre dans son propre onglet.
     */
    public static List<ItemStack> displayOrder() {
        return displayOrder(material -> !material.isAddon());
    }

    /** Même ordre, pour les matériaux choisis (ex : ceux d'un addon, pour son propre onglet). */
    public static List<ItemStack> displayOrder(Predicate<WeaponMaterial> materials) {
        List<ItemStack> stacks = new ArrayList<>();
        addAll(stacks, ModItems.allShields(), materials);
        Arrays.stream(WeaponType.values())
                .sorted(Comparator.comparingDouble(type -> WeaponData.weapon(type).attackReach()))
                .forEach(type -> addAll(stacks, ModItems.allOfType(type), materials));
        addAll(stacks, ModItems.allBows(), materials);
        addAll(stacks, ModItems.allCrossbows(), materials);
        addAll(stacks, ModItems.allShurikens(), materials);
        return stacks;
    }

    private static void addAll(List<ItemStack> stacks, Collection<? extends Supplier<? extends Item>> items, Predicate<WeaponMaterial> materials) {
        items.stream().map(Supplier::get).filter(item -> isShown(item, materials)).forEach(item -> stacks.add(new ItemStack(item)));
    }

    /** Un matériau de mod sans ingrédient (objet renommé, retiré par un script) a ses recettes coupées : autant ne pas le montrer. */
    private static boolean isShown(Item item, Predicate<WeaponMaterial> materials) {
        WeaponMaterial material = materialOf(item);
        return material == null || materials.test(material) && material.hasIngredients();
    }

    @Nullable
    private static WeaponMaterial materialOf(Item item) {
        if (item instanceof WeaponItem weapon) return weapon.getMaterial();
        if (item instanceof WeaponShieldItem shield) return shield.getMaterial();
        if (item instanceof RangedWeapon ranged) return ranged.getMaterial();
        return null;
    }
}
