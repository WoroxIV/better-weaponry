package com.betterweaponry.forge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.CombatHud;
import com.betterweaponry.client.ModKeyMappings;
import com.betterweaponry.client.RangedWeaponClient;
import com.betterweaponry.client.ShurikenRenderer;
import com.betterweaponry.client.WeaponPaletteSource;
import com.betterweaponry.client.editor.ClientEditorHooks;
import com.betterweaponry.client.editor.WeaponEditorScreen;
import com.betterweaponry.editor.EditorHooks;
import com.betterweaponry.registry.ModEntities;
import com.betterweaponry.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Partie cliente du point d'entrée Forge (jamais chargée sur un serveur dédié). */
final class BetterWeaponryForgeClient {
    private BetterWeaponryForgeClient() {
    }

    static void init(IEventBus modEventBus) {
        // Bouton "Config" dans la liste des mods : l'éditeur des armes (avec une page pour les préférences d'affichage).
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> WeaponEditorScreen.fromModList(parent)));
        // Paquets du serveur pour l'éditeur (ouverture par /betterweaponry, résultat d'un enregistrement).
        EditorHooks.setClient(new ClientEditorHooks());
        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        BetterWeaponry.setLanguageSupplier(BetterWeaponryForgeClient::gameLanguage);
        // Le type de source de textures doit exister avant le premier chargement des atlas.
        BetterWeaponry.LOGGER.debug("Texture source {}", WeaponPaletteSource.TYPE);

        modEventBus.addListener(BetterWeaponryForgeClient::registerGuiOverlays);
        modEventBus.addListener(BetterWeaponryForgeClient::registerRenderers);
        modEventBus.addListener(BetterWeaponryForgeClient::registerKeyMappings);
        modEventBus.addListener(BetterWeaponryForgeClient::clientSetup);
        modEventBus.register(ForgeClientModEvents.class);
        MinecraftForge.EVENT_BUS.register(ForgeClientEvents.class);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHURIKEN.get(), ShurikenRenderer::new);
    }

    /** La pose "bouclier levé" est un modèle différent, choisi par cette propriété (comme pour le bouclier vanilla). */
    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModItems.allShields().forEach(shield -> ItemProperties.register(shield.get(),
                    new ResourceLocation("blocking"),
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
    private static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "combat_hud", (gui, graphics, partialTick, width, height) -> CombatHud.render(graphics));
    }

    /** Langue choisie dans les options du jeu (ex : "fr_fr"), ou l'anglais si les options ne sont pas encore chargées. */
    private static String gameLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.options != null ? minecraft.options.languageCode : "en_us";
    }
}
