package com.betterweaponry.forge;

import com.betterweaponry.client.ShieldClientExtensions;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponShieldItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Bouclier du mod sur Forge : le modèle 3D passe par l'objet. */
final class ForgeShieldItem extends WeaponShieldItem {
    ForgeShieldItem(WeaponMaterial material, Properties properties) {
        super(material, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ShieldClientExtensions());
    }
}
