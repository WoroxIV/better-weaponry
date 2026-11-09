package com.betterweaponry.registry;

import net.minecraft.core.registries.Registries;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.betterweaponry.platform.Registered;
import com.betterweaponry.platform.Platform;
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
    private static final Map<WeaponType, Map<WeaponMaterial, Registered<Item, WeaponItem>>> WEAPONS = new EnumMap<>(WeaponType.class);
    private static final Map<WeaponMaterial, Registered<Item, WeaponShieldItem>> SHIELDS = new LinkedHashMap<>();
    private static final Map<WeaponMaterial, Registered<Item, WeaponBowItem>> BOWS = new LinkedHashMap<>();
    private static final Map<WeaponMaterial, Registered<Item, WeaponCrossbowItem>> CROSSBOWS = new LinkedHashMap<>();
    private static final Map<WeaponMaterial, Registered<Item, ShurikenItem>> SHURIKENS = new LinkedHashMap<>();

    static {
        for (WeaponType type : WeaponType.values()) {
            // Garde l'ordre de déclaration des matériaux : c'est celui de l'onglet créatif.
            Map<WeaponMaterial, Registered<Item, WeaponItem>> byMaterial = new LinkedHashMap<>();
            for (WeaponMaterial material : WeaponMaterials.active()) {
                byMaterial.put(material, register(material.itemName(type), () -> new WeaponItem(type, material, properties(new Item.Properties(), material))));
            }
            WEAPONS.put(type, byMaterial);
        }
        for (WeaponMaterial material : WeaponMaterials.active()) {
            SHIELDS.put(material, register(material.itemName(GearType.SHIELD), () -> new WeaponShieldItem(material, new Item.Properties())));
            BOWS.put(material, register(material.itemName(GearType.BOW), () -> new WeaponBowItem(material, new Item.Properties())));
            CROSSBOWS.put(material, register(material.itemName(GearType.CROSSBOW), () -> new WeaponCrossbowItem(material, new Item.Properties())));
            SHURIKENS.put(material, register(material.itemName(GearType.SHURIKEN), () -> new ShurikenItem(material, new Item.Properties())));
        }
    }

    private ModItems() {
    }

    /** Force la déclaration des objets. */
    public static void init() {
    }

    private static <T extends Item> Registered<Item, T> register(String name, Supplier<T> item) {
        return Platform.get().register(Registries.ITEM, name, item);
    }

    /** Toutes les armes de mêlée, tous types et matériaux confondus. */
    public static List<Item> allWeapons() {
        List<Item> weapons = new ArrayList<>();
        for (Map<WeaponMaterial, Registered<Item, WeaponItem>> byMaterial : WEAPONS.values()) {
            byMaterial.values().forEach(weapon -> weapons.add(weapon.get()));
        }
        return weapons;
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
    public static Registered<Item, WeaponItem> get(WeaponType type, WeaponMaterial material) {
        Registered<Item, WeaponItem> item = WEAPONS.get(type).get(material);
        if (item == null) {
            throw new IllegalStateException("Aucune arme " + material.itemName(type) + " : matériau inactif");
        }
        return item;
    }

    /** Lève une exception si le matériau n'est pas actif (mod requis absent). */
    public static Registered<Item, WeaponShieldItem> shield(WeaponMaterial material) {
        Registered<Item, WeaponShieldItem> item = SHIELDS.get(material);
        if (item == null) throw new IllegalStateException("Aucun bouclier " + material.itemName(GearType.SHIELD) + " : matériau inactif");
        return item;
    }

    public static Registered<Item, WeaponBowItem> bow(WeaponMaterial material) {
        return Objects.requireNonNull(BOWS.get(material), () -> "Aucun arc " + material + " : matériau inactif");
    }

    public static Registered<Item, WeaponCrossbowItem> crossbow(WeaponMaterial material) {
        return Objects.requireNonNull(CROSSBOWS.get(material), () -> "Aucune arbalète " + material + " : matériau inactif");
    }

    public static Registered<Item, ShurikenItem> shuriken(WeaponMaterial material) {
        return Objects.requireNonNull(SHURIKENS.get(material), () -> "Aucun shuriken " + material + " : matériau inactif");
    }

    public static Collection<Registered<Item, ShurikenItem>> allShurikens() {
        return Collections.unmodifiableCollection(SHURIKENS.values());
    }

    public static Collection<Registered<Item, WeaponBowItem>> allBows() {
        return Collections.unmodifiableCollection(BOWS.values());
    }

    public static Collection<Registered<Item, WeaponCrossbowItem>> allCrossbows() {
        return Collections.unmodifiableCollection(CROSSBOWS.values());
    }

    /** Un bouclier par matériau actif. */
    public static Collection<Registered<Item, WeaponShieldItem>> allShields() {
        return Collections.unmodifiableCollection(SHIELDS.values());
    }

    /** Toutes les armes d'un type, une par matériau actif (ex : pour une recharge commune à tous les matériaux). */
    public static Collection<Registered<Item, WeaponItem>> allOfType(WeaponType type) {
        return Collections.unmodifiableCollection(WEAPONS.get(type).values());
    }
}
