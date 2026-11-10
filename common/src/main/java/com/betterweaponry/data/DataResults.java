package com.betterweaponry.data;

import com.mojang.serialization.DataResult;

import java.util.Optional;

/** Raccourcis pour les résultats de décodage : l'API diffère d'une version du jeu à l'autre. */
public final class DataResults {
    private DataResults() {
    }

    /** La valeur, ou une exception avec le message d'erreur. */
    public static <T> T orThrow(DataResult<T> result) {
        return result.getOrThrow(false, message -> {
        });
    }

    /** Le même que {@link #orThrow}, sous le nom utilisé par le code partagé avec la 1.21.1. */
    public static <T> T getOrThrow(DataResult<T> result) {
        return orThrow(result);
    }

    /** Le message d'erreur du décodage, s'il a échoué. */
    public static <T> Optional<String> error(DataResult<T> result) {
        return result.error().map(DataResult.PartialResult::message);
    }
}
