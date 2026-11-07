package com.betterweaponry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Point d'entrée client uniquement (jamais chargé sur un serveur dédié). */
@Mod(value = BetterWeaponry.MODID, dist = Dist.CLIENT)
public final class BetterWeaponryClient {

    public BetterWeaponryClient(ModContainer container) {
        // Bouton "Config" dans la liste des mods. Les libellés viennent des clés
        // "better_weaponry.configuration.*" des fichiers de langue.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
