package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Onglet créatif du mod : boucliers, armes de mêlée de la plus courte à la plus longue, puis arcs, arbalètes et shurikens. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BetterWeaponry.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS = TABS.register("weapons", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.better_weaponry.weapons"))
            .icon(() -> new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get()))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .displayItems((parameters, output) -> displayOrder().forEach(output::accept))
            .build());

    private ModCreativeTabs() {
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

    private static void addAll(List<ItemStack> stacks, Collection<? extends DeferredItem<?>> items) {
        items.forEach(item -> stacks.add(new ItemStack(item.get())));
    }
}
