package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponBowItem;
import com.betterweaponry.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

/** Apparence des arcs et arbalètes du mod : mêmes états de modèle et même zoom que les vanilla. */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class RangedWeaponClient {
    private static final float VANILLA_DRAW_TICKS = 20.0F;
    private static final float MAX_ZOOM = 0.15F;

    private RangedWeaponClient() {
    }

    private static ResourceLocation vanilla(String name) {
        return ResourceLocation.withDefaultNamespace(name);
    }

    /** Propriétés lues par les fichiers de modèle (arc bandé, arbalète chargée...). */
    public static void registerItemProperties() {
        ModItems.allBows().forEach(holder -> {
            WeaponBowItem bow = holder.get();
            ItemProperties.register(bow, vanilla("pull"), (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) return 0.0F;
                return (float) ((stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) * bow.drawSpeed() / VANILLA_DRAW_TICKS);
            });
            ItemProperties.register(bow, vanilla("pulling"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
        ModItems.allCrossbows().forEach(holder -> registerCrossbow(holder.get()));
    }

    private static void registerCrossbow(Item crossbow) {
        ItemProperties.register(crossbow, vanilla("pull"), (stack, level, entity, seed) -> {
            if (entity == null || CrossbowItem.isCharged(stack)) return 0.0F;
            return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / CrossbowItem.getChargeDuration(stack, entity);
        });
        ItemProperties.register(crossbow, vanilla("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemProperties.register(crossbow, vanilla("charged"), (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemProperties.register(crossbow, vanilla("firework"), (stack, level, entity, seed) -> {
            ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
            return projectiles != null && projectiles.contains(Items.FIREWORK_ROCKET) ? 1.0F : 0.0F;
        });
    }

    /** Le zoom pendant le bandage : le jeu ne le fait que pour l'arc vanilla. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof WeaponBowItem bow)) return;
        float drawn = (float) (player.getTicksUsingItem() * bow.drawSpeed() / VANILLA_DRAW_TICKS);
        float zoom = drawn > 1.0F ? 1.0F : drawn * drawn;
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F - zoom * MAX_ZOOM));
    }
}
