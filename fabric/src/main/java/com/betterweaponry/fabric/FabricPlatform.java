package com.betterweaponry.fabric;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.network.Payload;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/** Version Fabric de ce qui diffère d'un chargeur à l'autre. */
final class FabricPlatform implements Platform {
    private static final String COMMON_NAMESPACE = "c:";
    private static final String TOOLS_PREFIX = "tools/";
    /** En créatif, le jeu fait porter les objets à 5 blocs au lieu de 3. Les armes du mod gardent leur portée affichée. */
    private static final double CREATIVE_REACH_BONUS = 2.0D;

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isDataGen() {
        return false;
    }

    @Override
    public boolean isFakePlayer(Entity entity) {
        return entity instanceof FakePlayer;
    }

    @Override
    public boolean isShield(ItemStack stack) {
        return stack.getItem() instanceof ShieldItem;
    }

    @Override
    public Attribute entityReachAttribute() {
        return FabricAttributes.ENTITY_REACH.get();
    }

    @Override
    public double entityReach(Player player) {
        double reach = player.getAttributeValue(FabricAttributes.ENTITY_REACH.get());
        return reach == 0.0D || !player.isCreative() || WeaponItem.hasExactReach(player) ? reach : reach + CREATIVE_REACH_BONUS;
    }

    /** Convention de Fabric en 1.20.1 : "c:ingots/steel" s'écrit "c:steel_ingots". La même table que dans fabric/build.gradle. */
    @Override
    public String commonTag(String id) {
        if (!id.startsWith(COMMON_NAMESPACE)) return id;
        String path = id.substring(COMMON_NAMESPACE.length());
        if (path.equals("gems/diamond")) return COMMON_NAMESPACE + "diamonds";
        if (path.equals("rods/wooden")) return COMMON_NAMESPACE + "wooden_rods";
        if (path.startsWith(TOOLS_PREFIX)) return COMMON_NAMESPACE + path.substring(TOOLS_PREFIX.length());
        int separator = path.indexOf('/');
        return separator < 0 ? id : COMMON_NAMESPACE + path.substring(separator + 1) + "_" + path.substring(0, separator);
    }

    @Override
    public boolean isBossPart(Entity entity) {
        return entity instanceof EnderDragonPart;
    }

    @Override
    public Optional<String> readModFile(String... path) {
        return FabricLoader.getInstance().getModContainer(BetterWeaponry.MODID).flatMap(container -> container.findPath(String.join("/", path))).flatMap(file -> {
            if (!Files.isRegularFile(file)) return Optional.empty();
            try {
                return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                BetterWeaponry.LOGGER.error("Could not read {} from the mod jar", String.join("/", path), e);
                return Optional.empty();
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Registered<R, T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory) {
        Registry<R> target = (Registry<R>) BuiltInRegistries.REGISTRY.get(registry.location());
        if (target == null) throw new IllegalArgumentException("Unknown registry " + registry.location());
        Holder.Reference<R> holder = Registry.registerForHolder(target, new ResourceLocation(BetterWeaponry.MODID, name), factory.get());
        return () -> (T) holder.value();
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Payload payload) {
        FabricNetwork.sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTracking(Entity entity, Payload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            FabricNetwork.sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendToServer(Payload payload) {
        FabricNetwork.sendToServer(payload);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Enchantment enchantment) {
        return stack.getItem() instanceof ShurikenItem shuriken ? shuriken.supportsEnchantment(stack, enchantment) : enchantment.canEnchant(stack);
    }

    @Override
    public ServerPlayer createFakePlayer(ServerLevel level) {
        return FakePlayer.get(level);
    }
}
