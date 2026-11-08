package com.betterweaponry.item;

/**
 * Équipements déclinés dans chaque matériau, en plus des armes de {@link WeaponType}.
 * Leurs noms suivent le même système de traduction : une entrée dans {@code names.types} de chaque langue.
 */
public enum GearType {
    SHIELD("shield"),
    BOW("bow"),
    CROSSBOW("crossbow");

    private final String id;

    GearType(String id) {
        this.id = id;
    }

    /** Suffixe des noms d'items, ex : "shield" pour "iron_shield". */
    public String id() {
        return id;
    }
}
