package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.GearType;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.item.WeaponBowItem;
import com.betterweaponry.item.WeaponCrossbowItem;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponShieldItem;
import com.betterweaponry.item.WeaponType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Enregistre une arme pour chaque combinaison type × matériau actif (ex : "netherite_katana").
 * L'ordre d'enregistrement (type puis matériau) est aussi l'ordre dans l'onglet créatif.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BetterWeaponry.MODID);

    private static final Map<WeaponType, Map<WeaponMaterial, DeferredItem<WeaponItem>>> WEAPONS = new EnumMap<>(WeaponType.class);
    private static final Map<WeaponMaterial, DeferredItem<WeaponShieldItem>> SHIELDS = new LinkedHashMap<>();
    private static final Map<WeaponMaterial, DeferredItem<WeaponBowItem>> BOWS = new LinkedHashMap<>();
    private static final Map<WeaponMaterial, DeferredItem<WeaponCrossbowItem>> CROSSBOWS = new LinkedHashMap<>();
    private static final Map<WeaponMaterial, DeferredItem<ShurikenItem>> SHURIKENS = new LinkedHashMap<>();

    static {
        for (WeaponType type : WeaponType.values()) {
            // Garde l'ordre de déclaration des matériaux : c'est celui de l'onglet créatif.
            Map<WeaponMaterial, DeferredItem<WeaponItem>> byMaterial = new LinkedHashMap<>();
            for (WeaponMaterial material : WeaponMaterials.active()) {
                byMaterial.put(material, ITEMS.registerItem(
                        material.itemName(type),
                        properties -> new WeaponItem(type, material, properties(properties, material))));
            }
            WEAPONS.put(type, byMaterial);
        }
        for (WeaponMaterial material : WeaponMaterials.active()) {
            SHIELDS.put(material, ITEMS.registerItem(material.itemName(GearType.SHIELD), properties -> new WeaponShieldItem(material, properties)));
            BOWS.put(material, ITEMS.registerItem(material.itemName(GearType.BOW), properties -> new WeaponBowItem(material, properties)));
            CROSSBOWS.put(material, ITEMS.registerItem(material.itemName(GearType.CROSSBOW), properties -> new WeaponCrossbowItem(material, properties)));
            SHURIKENS.put(material, ITEMS.registerItem(material.itemName(GearType.SHURIKEN), properties -> new ShurikenItem(material, properties)));
        }
    }

    private ModItems() {
    }

    /** Une arme ne s'empile pas ; celles en netherite (et matériaux semblables) résistent au feu et à la lave. */
    private static Item.Properties properties(Item.Properties properties, WeaponMaterial material) {
        properties.stacksTo(1);
        if (material.isFireResistant()) properties.fireResistant();
        return properties;
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

    /** Lève une exception si le matériau n'est pas actif (mod requis absent). */
    public static DeferredItem<WeaponShieldItem> shield(WeaponMaterial material) {
        DeferredItem<WeaponShieldItem> item = SHIELDS.get(material);
        if (item == null) throw new IllegalStateException("Aucun bouclier " + material.itemName(GearType.SHIELD) + " : matériau inactif");
        return item;
    }

    public static DeferredItem<WeaponBowItem> bow(WeaponMaterial material) {
        return Objects.requireNonNull(BOWS.get(material), () -> "Aucun arc " + material + " : matériau inactif");
    }

    public static DeferredItem<WeaponCrossbowItem> crossbow(WeaponMaterial material) {
        return Objects.requireNonNull(CROSSBOWS.get(material), () -> "Aucune arbalète " + material + " : matériau inactif");
    }

    public static DeferredItem<ShurikenItem> shuriken(WeaponMaterial material) {
        return Objects.requireNonNull(SHURIKENS.get(material), () -> "Aucun shuriken " + material + " : matériau inactif");
    }

    public static Collection<DeferredItem<ShurikenItem>> allShurikens() {
        return Collections.unmodifiableCollection(SHURIKENS.values());
    }

    public static Collection<DeferredItem<WeaponBowItem>> allBows() {
        return Collections.unmodifiableCollection(BOWS.values());
    }

    public static Collection<DeferredItem<WeaponCrossbowItem>> allCrossbows() {
        return Collections.unmodifiableCollection(CROSSBOWS.values());
    }

    /** Un bouclier par matériau actif. */
    public static Collection<DeferredItem<WeaponShieldItem>> allShields() {
        return Collections.unmodifiableCollection(SHIELDS.values());
    }

    /** Toutes les armes d'un type, une par matériau actif (ex : pour une recharge commune à tous les matériaux). */
    public static Collection<DeferredItem<WeaponItem>> allOfType(WeaponType type) {
        return Collections.unmodifiableCollection(WEAPONS.get(type).values());
    }
}
