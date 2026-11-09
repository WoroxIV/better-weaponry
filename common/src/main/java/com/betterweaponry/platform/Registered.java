package com.betterweaponry.platform;

import net.minecraft.core.Holder;

import java.util.function.Supplier;

/**
 * Un objet enregistré dans le jeu.
 *
 * @param <R> type du registre (Item, SoundEvent...)
 * @param <T> type exact de l'objet
 */
public interface Registered<R, T extends R> extends Supplier<T> {

    /** L'objet : uniquement une fois les registres remplis. */
    @Override
    T get();

    Holder<R> holder();
}
