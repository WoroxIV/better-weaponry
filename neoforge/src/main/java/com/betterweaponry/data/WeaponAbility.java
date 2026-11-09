package com.betterweaponry.data;

import net.minecraft.util.StringRepresentable;

/**
 * Capacité déclenchée par un clic droit avec l'arme en main principale (champ "ability" d'un fichier d'arme).
 * Une arme n'a qu'une capacité. Ajouter une capacité : une valeur ici, ses réglages dans un objet à part
 * (comme {@link ParryDefinition}), et son nom dans la clé de langue {@code better_weaponry.ability.<nom>}.
 */
public enum WeaponAbility implements StringRepresentable {
    /** Le clic droit garde son action normale. */
    NONE("none"),
    /** Parade comme en 1.8 : réglages dans {@link ParryDefinition}. */
    PARRY("parry"),
    /** Bond rapide : réglages dans {@link DashDefinition}. */
    DASH("dash");

    private final String serializedName;

    WeaponAbility(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
