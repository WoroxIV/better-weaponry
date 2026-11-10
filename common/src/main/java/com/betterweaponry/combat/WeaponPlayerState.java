package com.betterweaponry.combat;

import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/** Ce que le mod retient d'un joueur : un objet par joueur, ajouté par {@link com.betterweaponry.mixin.PlayerWeaponMixin}. */
public final class WeaponPlayerState {
    /** Force de la dernière attaque (1 = chargée à fond), pour le Saignement. */
    public float lastAttackStrength;
    /** Avant ce moment, Vampirisme ne soigne pas à nouveau. */
    public long vampirismReadyAt;
    /** Shurikens dont le dernier point de durabilité a été lancé : les ramasser recrée l'objet. Sauvegardé avec le joueur. */
    public List<UUID> brokenShurikens = List.of();

    public static WeaponPlayerState of(Player player) {
        return ((IWeaponPlayer) player).betterweaponry$weaponState();
    }
}
