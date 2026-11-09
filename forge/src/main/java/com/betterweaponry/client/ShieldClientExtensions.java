package com.betterweaponry.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Relie les boucliers du mod à leur rendu. Créé à la première utilisation : le jeu n'est pas encore prêt au démarrage. */
public final class ShieldClientExtensions implements IClientItemExtensions {
    private ShieldItemRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) renderer = new ShieldItemRenderer();
        return renderer;
    }
}
