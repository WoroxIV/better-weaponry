package com.betterweaponry.network;

import net.minecraft.network.FriendlyByteBuf;

/** Un paquet du mod. Chaque paquet a aussi une méthode statique {@code read} : ils sont déclarés par le code de chaque chargeur. */
public interface Payload {
    void write(FriendlyByteBuf buffer);
}
