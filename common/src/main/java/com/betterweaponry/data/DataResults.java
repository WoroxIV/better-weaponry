package com.betterweaponry.data;

import com.mojang.serialization.DataResult;

/** Raccourcis pour les résultats de décodage : l'API diffère d'une version du jeu à l'autre. */
public final class DataResults {
    private DataResults() {
    }

    /** La valeur, ou une exception avec le message d'erreur. */
    public static <T> T orThrow(DataResult<T> result) {
        return result.getOrThrow(false, message -> {
        });
    }
}
