package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Version NeoForge de ce qui diffère d'un chargeur à l'autre. */
final class NeoForgePlatform implements Platform {
    private final IEventBus modBus;
    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();

    NeoForgePlatform(IEventBus modBus) {
        this.modBus = modBus;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isDataGen() {
        return DatagenModLoader.isRunningDataGen();
    }

    @Override
    public boolean isFakePlayer(Entity entity) {
        return entity instanceof FakePlayer;
    }

    @Override
    public boolean isShield(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    @Override
    public ItemAttributeModifiers attributeModifiers(ItemStack stack) {
        return stack.getAttributeModifiers();
    }

    @Override
    public boolean isBossPart(Entity entity) {
        return entity instanceof PartEntity<?>;
    }

    @Override
    public Optional<String> readModFile(String... path) {
        IModFileInfo info = ModList.get().getModFileById(BetterWeaponry.MODID);
        if (info == null) return Optional.empty();
        Path file = info.getFile().findResource(path);
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            BetterWeaponry.LOGGER.error("Could not read {} from the mod jar", String.join("/", path), e);
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Registered<R, T> register(ResourceKey<? extends Registry<R>> registry, String name, Supplier<T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>) registers.computeIfAbsent(registry, key -> {
            DeferredRegister<R> created = DeferredRegister.create(registry, BetterWeaponry.MODID);
            created.register(modBus);
            return created;
        });
        DeferredHolder<R, T> holder = register.register(name, factory);
        return new Registered<>() {
            @Override
            public T get() {
                return holder.get();
            }

            @Override
            public Holder<R> holder() {
                return holder;
            }
        };
    }

    @Override
    public Attribute createArmorPenetrationAttribute(String descriptionId) {
        return new ArmorPenetrationAttribute(descriptionId);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTracking(Entity entity, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void migrateLegacyConfig() {
        LegacyConfigMigration.migrateClientConfig();
        LegacyConfigMigration.migrateIfPresent();
    }

    private static final class ArmorPenetrationAttribute extends PercentageAttribute {
        ArmorPenetrationAttribute(String descriptionId) {
            super(descriptionId, 0.0D, 0.0D, 1.0D);
        }

        @Override
        public ResourceLocation getBaseId() {
            return ModAttributes.BASE_ARMOR_PENETRATION_ID;
        }

        /** Ligne verte en pourcentage, au format vanilla de chaque langue ("30% Armor Penetration", "Penetrazione dell'armatura: 30%"). */
        @Override
        public MutableComponent toBaseComponent(double value, double entityBase, boolean merged, TooltipFlag flag) {
            return Component.translatable("attribute.modifier.equals.1", FORMAT.format(value * scaleFactor),
                    Component.translatable(getDescriptionId()));
        }
    }
}
