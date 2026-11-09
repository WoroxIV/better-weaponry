package com.betterweaponry.fabric;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/** Version Fabric de ce qui diffère d'un chargeur à l'autre. */
final class FabricPlatform implements Platform {

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
    public ItemAttributeModifiers attributeModifiers(ItemStack stack) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (EquipmentSlotGroup group : EquipmentSlotGroup.values()) {
            stack.forEachModifier(group, (attribute, modifier) -> builder.add(attribute, modifier, group));
        }
        return builder.build();
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
        Holder.Reference<R> holder = Registry.registerForHolder(target, ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, name), factory.get());
        return new Registered<>() {
            @Override
            public T get() {
                return (T) holder.value();
            }

            @Override
            public Holder<R> holder() {
                return holder;
            }
        };
    }

    @Override
    public Attribute createArmorPenetrationAttribute(String descriptionId) {
        return new RangedAttribute(descriptionId, 0.0D, 0.0D, 1.0D);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToPlayersTracking(Entity entity, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack.getItem() instanceof WeaponItem weapon) return weapon.supportsEnchantment(stack, enchantment);
        if (stack.getItem() instanceof ShurikenItem shuriken) return shuriken.supportsEnchantment(stack, enchantment);
        return enchantment.value().isSupportedItem(stack);
    }

    @Override
    public ServerPlayer createFakePlayer(ServerLevel level) {
        return FakePlayer.get(level);
    }
}
