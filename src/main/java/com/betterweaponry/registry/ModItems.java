package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Enregistre une arme pour chaque combinaison type × matériau actif (ex : "netherite_katana").
 * L'ordre d'enregistrement (type puis matériau) est aussi l'ordre dans l'onglet créatif.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BetterWeaponry.MODID);

    private static final Map<WeaponType, Map<WeaponMaterial, DeferredItem<WeaponItem>>> WEAPONS = new EnumMap<>(WeaponType.class);

    static {
        for (WeaponType type : WeaponType.values()) {
            Map<WeaponMaterial, DeferredItem<WeaponItem>> byMaterial = new IdentityHashMap<>();
            for (WeaponMaterial material : WeaponMaterials.active()) {
                byMaterial.put(material, ITEMS.registerItem(
                        material.itemName(type),
                        properties -> new WeaponItem(type, material, properties.stacksTo(1))));
            }
            WEAPONS.put(type, byMaterial);
        }
    }

    private ModItems() {
    }

    /**
     * Récupère une arme précise, ex : {@code ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON)}.
     * Lève une exception si le matériau n'est pas actif (mod requis absent).
     */
    public static DeferredItem<WeaponItem> get(WeaponType type, WeaponMaterial material) {
        DeferredItem<WeaponItem> item = WEAPONS.get(type).get(material);
        if (item == null) {
            throw new IllegalStateException("Aucune arme " + material.itemName(type) + " : matériau inactif");
        }
        return item;
    }
}
