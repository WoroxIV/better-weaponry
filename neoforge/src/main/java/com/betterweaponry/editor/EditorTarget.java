package com.betterweaponry.editor;

/** Où l'éditeur enregistre les réglages. */
public enum EditorTarget {
    /** config/better_weaponry/ : tous les mondes (sur un serveur : le dossier config du serveur). */
    ALL_WORLDS,
    /** Datapack {@code <monde>/datapacks/better_weaponry_editor} : ce monde seulement, prioritaire sur le reste. */
    THIS_WORLD;

    public static EditorTarget byIndex(int index) {
        EditorTarget[] values = values();
        return index >= 0 && index < values.length ? values[index] : ALL_WORLDS;
    }
}
