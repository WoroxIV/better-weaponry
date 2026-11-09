package com.betterweaponry.forge;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModAttributes;
import com.betterweaponry.registry.ModStats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

/** Point d'entrée Forge (chargé côté client et serveur). */
@Mod(BetterWeaponry.MODID)
public final class BetterWeaponryForge {

    public BetterWeaponryForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Platform.set(new ForgePlatform(modEventBus));
        BetterWeaponry.init();
        ForgeNetwork.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addToCreativeTab);
        modEventBus.addListener(this::addAttributes);
        MinecraftForge.EVENT_BUS.register(ForgeGameEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) BetterWeaponryForgeClient.init(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModStats::registerFormats);
        BetterWeaponry.prepareConfigFolder();
    }

    /** Le bouclier, l'arc et l'arbalète vanilla sont remplacés par ceux du mod, qui ont leur propre onglet. */
    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) return;
        for (Item replaced : List.of(Items.SHIELD, Items.BOW, Items.CROSSBOW)) {
            event.getEntries().remove(new ItemStack(replaced));
        }
    }

    /** Toutes les créatures reçoivent la pénétration d'armure (valeur 0) : joueurs, mais aussi mobs qui tiendraient une arme. */
    private void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (!event.has(type, ModAttributes.ARMOR_PENETRATION.get())) event.add(type, ModAttributes.ARMOR_PENETRATION.get());
        }
        event.add(EntityType.PLAYER, ModAttributes.SWEEPING_DAMAGE_RATIO.get());
    }
}
