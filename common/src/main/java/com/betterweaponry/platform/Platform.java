package com.betterweaponry.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Ce qui diffère d'un chargeur de mods à l'autre (NeoForge, Fabric...). Le code commun ne connaît que cette interface ;
 * chaque chargeur en fournit une version et l'installe au tout début de son point d'entrée.
 */
public interface Platform {

    /** La version du chargeur en cours d'utilisation. */
    static Platform get() {
        return PlatformHolder.instance();
    }

    /** À appeler en premier dans le point d'entrée du chargeur. */
    static void set(Platform platform) {
        PlatformHolder.install(platform);
    }

    boolean isModLoaded(String modId);

    Path configDir();

    Path gameDir();

    /** Vrai pendant la génération des fichiers de données. */
    boolean isDataGen();

    /** Joueur simulé par un mod (déployeur de Create...). */
    boolean isFakePlayer(Entity entity);

    /** Cet objet bloque comme un bouclier (celui du jeu, du mod, ou d'un autre mod). */
    boolean isShield(ItemStack stack);

    /** Tous les bonus d'attributs de cet objet, y compris ceux ajoutés par les mods. */
    ItemAttributeModifiers attributeModifiers(ItemStack stack);

    /** Partie d'une créature à plusieurs corps (dragon, boss d'un mod). */
    boolean isBossPart(Entity entity);

    /** Contenu texte d'un fichier du jar du mod, ex : {@code readModFile("data", "better_weaponry", "x.json")}. */
    Optional<String> readModFile(String... path);

    /** Enregistre un objet du jeu (item, son, effet...). Il n'est utilisable qu'une fois les registres remplis. */
    <R, T extends R> Registered<R, T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory);

    /** Début de la construction d'un onglet créatif, placé après l'onglet Combat. */
    CreativeModeTab.Builder creativeTabBuilder();

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Envoie aux joueurs qui voient cette entité, sans elle-même. */
    void sendToPlayersTracking(Entity entity, CustomPacketPayload payload);

    /** Côté client uniquement. */
    void sendToServer(CustomPacketPayload payload);

    /** Cet objet accepte cet enchantement sur une table ou une enclume. */
    boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment);

    /** Joueur simulé, pour les tests automatiques. */
    ServerPlayer createFakePlayer(ServerLevel level);

    /** Reprend les anciennes configs du mod, s'il y en a pour ce chargeur. */
    default void migrateLegacyConfig() {
    }
}
