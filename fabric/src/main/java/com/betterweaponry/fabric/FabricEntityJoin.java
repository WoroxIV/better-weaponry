package com.betterweaponry.fabric;

/** Distingue une entité qui vient d'apparaître d'une entité relue sur le disque : Fabric signale les deux de la même façon. */
public final class FabricEntityJoin {
    private static int depth;

    private FabricEntityJoin() {
    }

    public static void enterFreshEntity() {
        depth++;
    }

    public static void exitFreshEntity() {
        depth--;
    }

    static boolean isFreshEntityBeingAdded() {
        return depth > 0;
    }
}
