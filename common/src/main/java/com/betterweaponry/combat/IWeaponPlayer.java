package com.betterweaponry.combat;

/** Ajouté à chaque joueur par {@link com.betterweaponry.mixin.PlayerWeaponMixin}. */
public interface IWeaponPlayer {
    WeaponPlayerState betterweaponry$weaponState();
}
