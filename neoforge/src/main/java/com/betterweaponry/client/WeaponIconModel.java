package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

import java.util.Map;

/** Applique {@link WeaponIconLayout} dans les interfaces. Le reste (en main, au sol...) ne change pas. */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class WeaponIconModel extends BakedModelWrapper<BakedModel> {
    private final WeaponIconLayout layout;

    private WeaponIconModel(BakedModel original, WeaponIconLayout layout) {
        super(original);
        this.layout = layout;
    }

    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (WeaponType type : WeaponType.values()) {
            for (WeaponMaterial material : WeaponMaterials.all()) {
                ResourceLocation id = material.itemId(type);
                models.computeIfPresent(ModelResourceLocation.inventory(id),
                        (key, model) -> WeaponIconLayout.of(model).<BakedModel>map(layout -> new WeaponIconModel(model, layout)).orElse(model));
            }
        }
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        if (context != ItemDisplayContext.GUI) return super.applyTransform(context, pose, leftHand);
        layout.apply(pose);
        return this;
    }
}
