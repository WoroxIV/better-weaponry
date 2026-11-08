package com.betterweaponry.client;

import com.betterweaponry.entity.ShurikenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** Dessine le shuriken à plat, comme un disque, qui tourne sur lui-même tant qu'il vole. */
public class ShurikenRenderer extends EntityRenderer<ShurikenEntity> {
    private static final float SPIN_DEGREES_PER_TICK = 50.0F;
    private static final float SCALE = 0.9F;
    private static final int HIDDEN_TICKS = 2;
    private static final double HIDDEN_DISTANCE_SQUARED = 12.25D;

    private final ItemRenderer itemRenderer;

    public ShurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ShurikenEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        // Comme les objets lancés vanilla : juste après le lancer, il passerait devant la caméra et cacherait tout l'écran.
        if (entity.tickCount < HIDDEN_TICKS && this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < HIDDEN_DISTANCE_SQUARED) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        if (!entity.isStuck()) pose.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * SPIN_DEGREES_PER_TICK));
        pose.scale(SCALE, SCALE, SCALE);
        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShurikenEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
