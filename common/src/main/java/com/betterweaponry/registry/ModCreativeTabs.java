package com.betterweaponry.registry;

import java.util.function.Supplier;
import com.betterweaponry.platform.Registered;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.RangedWeapon;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponShieldItem;
import com.betterweaponry.item.WeaponType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

    /** Objets de l'onglet, dans l'ordre. La portée vient des données : un changement dans l'éditeur déplace l'arme. */
    public static List<ItemStack> displayOrder() {
        List<ItemStack> stacks = new ArrayList<>();
        addAll(stacks, ModItems.allShields());
        Arrays.stream(WeaponType.values())
                .sorted(Comparator.comparingDouble(type -> WeaponData.weapon(type).attackReach()))
                .forEach(type -> addAll(stacks, ModItems.allOfType(type)));
        addAll(stacks, ModItems.allBows());
        addAll(stacks, ModItems.allCrossbows());
        addAll(stacks, ModItems.allShurikens());
        return stacks;
    }

    private static void addAll(List<ItemStack> stacks, Collection<? extends Supplier<? extends Item>> items) {
        items.stream().filter(item -> canBeObtained(item.get())).forEach(item -> stacks.add(new ItemStack(item.get())));
    }

    /** Un matériau de mod sans ingrédient (objet renommé, retiré par un script) a ses recettes coupées : autant ne pas le montrer. */
    private static boolean canBeObtained(Item item) {
        if (item instanceof WeaponItem weapon) return weapon.getMaterial().hasIngredients();
        if (item instanceof WeaponShieldItem shield) return shield.getMaterial().hasIngredients();
        if (item instanceof RangedWeapon ranged) return ranged.getMaterial().hasIngredients();
        return true;
    }
}
