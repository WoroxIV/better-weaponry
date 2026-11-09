package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.CombatHud;
import com.betterweaponry.client.ModKeyMappings;
import com.betterweaponry.client.RangedWeaponClient;
import com.betterweaponry.client.ShieldClientExtensions;
import com.betterweaponry.client.ShurikenRenderer;
import com.betterweaponry.client.WeaponClientExtensions;
import com.betterweaponry.client.WeaponPaletteSource;
import com.betterweaponry.client.editor.ClientEditorHooks;
import com.betterweaponry.client.editor.WeaponEditorScreen;
import com.betterweaponry.editor.EditorHooks;
import com.betterweaponry.registry.ModEntities;
import com.betterweaponry.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Point d'entrée client NeoForge (jamais chargé sur un serveur dédié). */
@Mod(value = BetterWeaponry.MODID, dist = Dist.CLIENT)
public final class BetterWeaponryNeoForgeClient {

    public BetterWeaponryNeoForgeClient(IEventBus modEventBus, ModContainer container) {
        // Bouton "Config" dans la liste des mods : l'éditeur des armes (avec une page pour les préférences d'affichage).
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> WeaponEditorScreen.fromModList(parent));
        // Paquets du serveur pour l'éditeur (ouverture par /betterweaponry, résultat d'un enregistrement).
        EditorHooks.setClient(new ClientEditorHooks());

        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        BetterWeaponry.setLanguageSupplier(BetterWeaponryNeoForgeClient::gameLanguage);

        modEventBus.addListener(BetterWeaponryNeoForgeClient::registerClientExtensions);
        modEventBus.addListener(BetterWeaponryNeoForgeClient::registerGuiLayers);
        modEventBus.addListener(BetterWeaponryNeoForgeClient::registerRenderers);
        modEventBus.addListener(BetterWeaponryNeoForgeClient::registerKeyMappings);
        modEventBus.addListener(BetterWeaponryNeoForgeClient::registerSpriteSources);
        modEventBus.addListener(BetterWeaponryNeoForgeClient::clientSetup);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHURIKEN.get(), ShurikenRenderer::new);
    }

    /** Rendu spécial des armes en main (pose de parade en première personne). */
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new WeaponClientExtensions(), ModItems.allWeapons().toArray(Item[]::new));
        Item[] shields = ModItems.allShields().stream().<Item>map(holder -> holder.get()).toArray(Item[]::new);
        event.registerItem(new ShieldClientExtensions(), shields);
    }

    private static void registerSpriteSources(RegisterSpriteSourceTypesEvent event) {
        event.register(WeaponPaletteSource.ID, WeaponPaletteSource.TYPE);
    }

    /** La pose "bouclier levé" est un modèle différent, choisi par cette propriété (comme pour le bouclier vanilla). */
    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModItems.allShields().forEach(shield -> ItemProperties.register(shield.get(),
                    ResourceLocation.withDefaultNamespace("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F));
            RangedWeaponClient.registerItemProperties();
        });
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // "Interface" : pas de conflit avec Maj = s'accroupir, qui ne compte qu'en jeu.
        ModKeyMappings.WEAPON_DETAILS.setKeyConflictContext(KeyConflictContext.GUI);
        ModKeyMappings.OPEN_TUTORIAL.setKeyConflictContext(KeyConflictContext.IN_GAME);
        event.register(ModKeyMappings.WEAPON_DETAILS);
        event.register(ModKeyMappings.OPEN_TUTORIAL);
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
}
