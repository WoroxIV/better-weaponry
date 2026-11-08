package com.betterweaponry;

import com.betterweaponry.client.CombatHud;
import com.betterweaponry.client.ModKeyMappings;
import com.betterweaponry.client.WeaponClientExtensions;
import com.betterweaponry.client.editor.ClientEditorHooks;
import com.betterweaponry.client.editor.WeaponEditorScreen;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.editor.EditorHooks;
import com.betterweaponry.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Point d'entrée client uniquement (jamais chargé sur un serveur dédié). */
@Mod(value = BetterWeaponry.MODID, dist = Dist.CLIENT)
public final class BetterWeaponryClient {

    public BetterWeaponryClient(IEventBus modEventBus, ModContainer container) {
        // Préférences d'affichage du joueur : config/better_weaponry-client.toml
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // Bouton "Config" dans la liste des mods : l'éditeur des armes (avec une page pour les préférences d'affichage).
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> WeaponEditorScreen.fromModList(parent));
        // Paquets du serveur pour l'éditeur (ouverture par /betterweaponry, résultat d'un enregistrement).
        EditorHooks.setClient(new ClientEditorHooks());

        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        BetterWeaponry.setLanguageSupplier(BetterWeaponryClient::gameLanguage);

        modEventBus.addListener(BetterWeaponryClient::registerClientExtensions);
        modEventBus.addListener(BetterWeaponryClient::registerGuiLayers);
        modEventBus.addListener(BetterWeaponryClient::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(BetterWeaponryClient::onLoggingOut);
    }

    /** Rendu spécial des armes en main (pose de parade en première personne). */
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Item[] weapons = ModItems.ITEMS.getEntries().stream().map(holder -> holder.get()).toArray(Item[]::new);
        event.registerItem(new WeaponClientExtensions(), weapons);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.WEAPON_DETAILS);
    }

    /** Éléments de combat à l'écran, dessinés juste au-dessus du viseur vanilla. */
    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "combat_hud"), CombatHud::render);
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
