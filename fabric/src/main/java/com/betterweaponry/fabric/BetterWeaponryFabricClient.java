package com.betterweaponry.fabric;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.RangedWeaponClient;
import com.betterweaponry.client.ShieldItemRenderer;
import com.betterweaponry.client.ShurikenRenderer;
import com.betterweaponry.client.WeaponPaletteSource;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.network.WeaponDataSyncPayload;
import com.betterweaponry.registry.ModEntities;
import com.betterweaponry.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Point d'entrée client Fabric (jamais chargé sur un serveur dédié). */
public final class BetterWeaponryFabricClient implements ClientModInitializer {
    private ShieldItemRenderer shieldRenderer;

    @Override
    public void onInitializeClient() {
        // Le README et les schémas du dossier de config sont écrits dans la langue du jeu.
        BetterWeaponry.setLanguageSupplier(() -> Minecraft.getInstance().options.languageCode);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> BetterWeaponry.prepareConfigFolder());

        registerNetwork();
        registerRendering();
        registerEvents();
    }

    private static void registerNetwork() {
        ClientConfigurationNetworking.registerGlobalReceiver(WeaponDataSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> WeaponDataSyncPayload.handle(payload)));
        ClientPlayNetworking.registerGlobalReceiver(WeaponDataSyncPayload.TYPE, (payload, context) -> WeaponDataSyncPayload.handle(payload));
    }

    private void registerRendering() {
        SpriteSources.TYPES.put(WeaponPaletteSource.ID, WeaponPaletteSource.TYPE);
        EntityRendererRegistry.register(ModEntities.SHURIKEN.get(), ShurikenRenderer::new);

        ModItems.allShields().forEach(shield -> {
            BuiltinItemRendererRegistry.INSTANCE.register(shield.get(), (stack, mode, pose, buffers, light, overlay) -> {
                // Créé au premier affichage : le jeu n'est pas encore prêt au démarrage.
                if (shieldRenderer == null) shieldRenderer = new ShieldItemRenderer();
                shieldRenderer.renderByItem(stack, mode, pose, buffers, light, overlay);
            });
            // La pose "bouclier levé" est un modèle différent, choisi par cette propriété (comme pour le bouclier vanilla).
            ItemProperties.register(shield.get(), ResourceLocation.withDefaultNamespace("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
        RangedWeaponClient.registerItemProperties();

    }

    private static void registerEvents() {
        // En quittant un serveur, on oublie ses stats : on revient aux valeurs par défaut du mod.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WeaponData.resetToBuiltin());
        // Une connexion qui échoue pendant le chargement ne passe pas par DISCONNECT, alors que les stats du serveur sont déjà reçues.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen && client.level == null) WeaponData.resetToBuiltin();
        });
    }

}
