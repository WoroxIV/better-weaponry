package com.betterweaponry;

import com.betterweaponry.client.WeaponClientExtensions;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Point d'entrée client uniquement (jamais chargé sur un serveur dédié). */
@Mod(value = BetterWeaponry.MODID, dist = Dist.CLIENT)
public final class BetterWeaponryClient {

    public BetterWeaponryClient(IEventBus modEventBus, ModContainer container) {
        // Préférences d'affichage du joueur : config/better_weaponry-client.toml
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // Bouton "Config" dans la liste des mods. Les libellés viennent des clés
        // "better_weaponry.configuration.*" des fichiers de langue.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        BetterWeaponry.setLanguageSupplier(BetterWeaponryClient::gameLanguage);

        modEventBus.addListener(BetterWeaponryClient::registerClientExtensions);
        NeoForge.EVENT_BUS.addListener(BetterWeaponryClient::onLoggingOut);
    }

    /** Rendu spécial des armes en main (pose de parade en première personne). */
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Item[] weapons = ModItems.ITEMS.getEntries().stream().map(holder -> holder.get()).toArray(Item[]::new);
        event.registerItem(new WeaponClientExtensions(), weapons);
    }

    /** Langue choisie dans les options du jeu (ex : "fr_fr"), ou l'anglais si les options ne sont pas encore chargées. */
    private static String gameLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.options != null ? minecraft.options.languageCode : "en_us";
    }

    /** En quittant un serveur, on oublie ses stats : on revient aux valeurs par défaut du mod. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WeaponData.resetToBuiltin();
    }
}
