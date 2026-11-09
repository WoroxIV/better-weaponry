package com.betterweaponry.network;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponData;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

/**
 * Étape de la connexion d'un joueur : envoie les données des armes avant qu'il entre dans le monde.
 * Ainsi, les stats sont déjà bonnes quand JEI/EMI et l'onglet créatif construisent leurs index.
 * (Même fonctionnement que la synchronisation des configs de NeoForge.)
 */
public record SyncWeaponDataTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final ConfigurationTask.Type TYPE =
            new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "sync_weapon_data"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(new WeaponDataSyncPayload(WeaponData.current()));
        listener.finishCurrentTask(TYPE);
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }
}
