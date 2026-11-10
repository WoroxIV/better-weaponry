package com.betterweaponry.forge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.RangedWeaponClient;
import com.betterweaponry.client.ShurikenRenderer;
import com.betterweaponry.client.WeaponPaletteSource;
import com.betterweaponry.registry.ModEntities;
import com.betterweaponry.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Partie cliente du point d'entrée Forge (jamais chargée sur un serveur dédié). */
final class BetterWeaponryForgeClient {
    private BetterWeaponryForgeClient() {
    }

    static void init(IEventBus modEventBus) {
        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        BetterWeaponry.setLanguageSupplier(BetterWeaponryForgeClient::gameLanguage);
        // Le type de source de textures doit exister avant le premier chargement des atlas.
        BetterWeaponry.LOGGER.debug("Texture source {}", WeaponPaletteSource.TYPE);

        modEventBus.addListener(BetterWeaponryForgeClient::registerRenderers);
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

    /** Langue choisie dans les options du jeu (ex : "fr_fr"), ou l'anglais si les options ne sont pas encore chargées. */
    private static String gameLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.options != null ? minecraft.options.languageCode : "en_us";
    }
}
