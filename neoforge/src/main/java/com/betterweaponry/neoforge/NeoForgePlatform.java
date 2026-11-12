package com.betterweaponry.neoforge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.api.WeaponMaterialProvider;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Version NeoForge de ce qui diffère d'un chargeur à l'autre. */
final class NeoForgePlatform implements Platform {
    private final IEventBus modBus;
    private final Map<RegisterKey, DeferredRegister<?>> registers = new LinkedHashMap<>();

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
    public <R, T extends R> Registered<R, T> register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, Supplier<T> factory) {
        // Un registre différé par namespace : les armes des matériaux d'addons gardent le namespace de l'addon.
        DeferredRegister<R> register = (DeferredRegister<R>) registers.computeIfAbsent(new RegisterKey(registry, id.getNamespace()), key -> {
            DeferredRegister<R> created = DeferredRegister.create(registry, id.getNamespace());
            created.register(modBus);
            return created;
        });
        DeferredHolder<R, T> holder = register.register(id.getPath(), factory);
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

    /** Chaque mod peut nommer sa classe dans {@code [modproperties.<modid>]} de son neoforge.mods.toml. */
    @Override
    public List<WeaponMaterialProvider> materialProviders() {
        List<WeaponMaterialProvider> providers = new ArrayList<>();
        for (IModInfo mod : ModList.get().getMods()) {
            Object className = mod.getModProperties().get(WeaponMaterialProvider.KEY);
            if (!(className instanceof String name)) continue;
            try {
                Class<?> type = Class.forName(name, true, NeoForgePlatform.class.getClassLoader());
                providers.add((WeaponMaterialProvider) type.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
                BetterWeaponry.LOGGER.error("[Better Weaponry] Could not create the material provider {} of {}", name, mod.getModId(), e);
            }
        }
        return providers;
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
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.supportsEnchantment(enchantment);
    }

    @Override
    public ServerPlayer createFakePlayer(ServerLevel level) {
        return FakePlayerFactory.getMinecraft(level);
    }

    @Override
    public void migrateLegacyConfig() {
        LegacyConfigMigration.migrateIfPresent();
    }

    private record RegisterKey(ResourceKey<?> registry, String namespace) {
    }
}
