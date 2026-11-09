package com.betterweaponry.combat;

/** État de combat ajouté à chaque mob par MobMixin. Rien n'est sauvegardé : ces valeurs ne durent que quelques secondes. */
public interface ICombatMob {

    boolean betterweaponry$offhandTurn();

    void betterweaponry$setOffhandTurn(boolean offhand);

    long betterweaponry$guardBrokenUntil();

    void betterweaponry$setGuardBrokenUntil(long gameTime);
}
