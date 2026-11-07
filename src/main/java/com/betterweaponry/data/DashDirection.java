package com.betterweaponry.data;

import net.minecraft.util.StringRepresentable;

/** Direction d'un dash (champ "dash.direction"). */
public enum DashDirection implements StringRepresentable {
    /** Là où le joueur regarde, à l'horizontale (fente). */
    LOOK("look"),
    /** Là où le joueur se déplace avec les touches de mouvement (esquive sur le côté) ; vers l'avant sans touche. */
    INPUT("input");

    private final String serializedName;

    DashDirection(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
