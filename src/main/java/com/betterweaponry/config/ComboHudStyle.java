package com.betterweaponry.config;

/** Apparence de l'indicateur de combo. */
public enum ComboHudStyle {
    DOTS("dots"),
    SQUARES("squares"),
    BAR("bar"),
    RING("ring"),
    COUNTER("counter");

    private final String id;

    ComboHudStyle(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
