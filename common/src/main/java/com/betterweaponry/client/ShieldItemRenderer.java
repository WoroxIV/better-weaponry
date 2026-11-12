package com.betterweaponry.client;

import com.betterweaponry.item.WeaponShieldItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.Objects;

/**
 * Dessine les boucliers du mod comme le jeu dessine le sien (modèle 3D, bannière).
 * La texture {@code entity/shield/<matériau>_shield} est fabriquée à partir de {@code textures/templates/entity/shield/shield.png}
 * et de la palette du matériau. Un fichier {@code textures/entity/shield/<matériau>_shield.png} (64×64) la remplace.
 * Si le matériau n'a ni l'un ni l'autre, le bouclier a l'apparence du vanilla.
 */
public final class ShieldItemRenderer extends BlockEntityWithoutLevelRenderer {
    private ShieldModel model;

    public ShieldItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (model == null) model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));

        BannerPatternLayers patterns = stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        DyeColor color = stack.get(DataComponents.BASE_COLOR);
        boolean decorated = !patterns.layers().isEmpty() || color != null;

        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);
        Material material = materialOf(stack, decorated);
        VertexConsumer consumer = material.sprite().wrap(
                ItemRenderer.getFoilBufferDirect(buffer, model.renderType(material.atlasLocation()), true, stack.hasFoil()));
        model.handle().render(poseStack, consumer, packedLight, packedOverlay);
        if (decorated) {
            BannerRenderer.renderPatterns(poseStack, buffer, packedLight, packedOverlay, model.plate(), material, false,
                    Objects.requireNonNullElse(color, DyeColor.WHITE), patterns, stack.hasFoil());
        } else {
            model.plate().render(poseStack, consumer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Texture propre au matériau si elle existe, sinon celle du bouclier vanilla. */
    private static Material materialOf(ItemStack stack, boolean decorated) {
        if (stack.getItem() instanceof WeaponShieldItem shield) {
            String name = "entity/shield/" + shield.getMaterial().id() + "_shield";
            Material custom = new Material(Sheets.SHIELD_SHEET, ResourceLocation.fromNamespaceAndPath(shield.getMaterial().namespace(), name));
            if (!custom.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) return custom;
        }
        return decorated ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
    }
}
