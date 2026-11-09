package com.betterweaponry.forge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponShieldItem;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.network.Payload;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.data.loading.DatagenModLoader;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Version Forge de ce qui diffère d'un chargeur à l'autre. */
final class ForgePlatform implements Platform {
    private static final String COMMON_NAMESPACE = "c:";

    private final IEventBus modBus;
    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();

    ForgePlatform(IEventBus modBus) {
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
        return stack.canPerformAction(ToolActions.SHIELD_BLOCK);
    }

    @Override
    public Attribute entityReachAttribute() {
        return ForgeMod.ENTITY_REACH.get();
    }

    @Override
    public double entityReach(Player player) {
        return player.getEntityReach();
    }

    @Override
    public String commonTag(String id) {
        return id.startsWith(COMMON_NAMESPACE) ? "forge:" + id.substring(COMMON_NAMESPACE.length()) : id;
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
        RegistryObject<T> object = register.register(name, factory);
        return object::get;
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Payload payload) {
        ForgeNetwork.sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersTracking(Entity entity, Payload payload) {
        ForgeNetwork.sendToPlayersTracking(entity, payload);
    }

    @Override
    public void sendToServer(Payload payload) {
        ForgeNetwork.sendToServer(payload);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(stack);
    }

    @Override
    public ServerPlayer createFakePlayer(ServerLevel level) {
        return FakePlayerFactory.getMinecraft(level);
    }

    @Override
    public void migrateLegacyConfig() {
        LegacyConfigMigration.migrateClientConfig();
        LegacyConfigMigration.migrateIfPresent();
    }

    @Override
    public WeaponItem createWeapon(WeaponType type, WeaponMaterial material, Item.Properties properties) {
        return new ForgeWeaponItem(type, material, properties);
    }

    @Override
    public WeaponShieldItem createShield(WeaponMaterial material, Item.Properties properties) {
        return new ForgeShieldItem(material, properties);
    }
}
