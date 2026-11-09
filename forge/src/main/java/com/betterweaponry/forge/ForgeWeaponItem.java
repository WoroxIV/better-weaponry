package com.betterweaponry.forge;

import com.betterweaponry.client.WeaponClientExtensions;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponType;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Arme du mod sur Forge : le rendu en main passe par l'objet. */
final class ForgeWeaponItem extends WeaponItem {
    ForgeWeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
        super(type, material, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new WeaponClientExtensions());
    }
}
