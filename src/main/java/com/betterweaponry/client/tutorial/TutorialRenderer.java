package com.betterweaponry.client.tutorial;

import com.betterweaponry.tutorial.TutorialScene;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Dessine une scène de tutoriel dans un écran, sans monde chargé : des modèles animés à la main sur une plateforme de blocs.
 * Tout fonctionne donc aussi depuis le menu principal.
 */
final class TutorialRenderer {
    /** Pleine lumière. */
    private static final int LIGHT = 15728880;
    private static final int PLATFORM_RADIUS = 4;
    private static final float DEPTH = 400.0F;
    private static final float PLAYER_SCALE = 0.9375F;
    private static final float MODEL_OFFSET = 1.501F;
    private static final float GROUND_OFFSET = 0.02F;
    private static final float OUTLINE_WIDTH = 0.05F;
    private static final float BEAM_WIDTH = 0.06F;
    private static final double DEGREES_PER_SEGMENT = 6.0D;
    private static final float ZOMBIE_ARMS_PITCH = -Mth.PI / 2.25F;
    private static final float HOLD_PITCH = 0.31415927F;
    private static final float GUARD_PITCH = 0.9424779F;
    private static final float GUARD_YAW = 0.5235988F;
    private static final float RISE_BLOCKS = 0.9F;
    private static final float RISE_SIDE_PIXELS = 26.0F;
    private static final ResourceLocation ZOMBIE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final ResourceLocation ARMOR_OUTER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_1.png");
    private static final ResourceLocation ARMOR_INNER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_2.png");

    /** Un texte ou une étincelle à dessiner par-dessus la scène, à cet endroit de l'écran. */
    record ScreenLabel(float x, float y, Component text, int color) {
    }

    record ScreenSpark(float x, float y, float progress, int color) {
    }

    private final Minecraft minecraft = Minecraft.getInstance();
    private final PlayerModel<LivingEntity> playerModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
    private final ZombieModel<Zombie> zombieModel = new ZombieModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.ZOMBIE));
    private final HumanoidModel<LivingEntity> armorOuter = new HumanoidModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    private final HumanoidModel<LivingEntity> armorInner = new HumanoidModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));

    final List<ScreenLabel> labels = new ArrayList<>();
    final List<ScreenSpark> sparks = new ArrayList<>();

    /**
     * @param centerX centre de la scène à l'écran
     * @param scale   pixels par bloc
     * @param yaw     rotation de la caméra autour de la scène, en degrés
     * @param pitch   inclinaison de la caméra, en degrés
     */
    void render(GuiGraphics graphics, TutorialScene scene, float tick, int centerX, int centerY, float scale, float yaw, float pitch) {
        labels.clear();
        sparks.clear();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((float) centerX, (float) centerY, DEPTH);
        pose.scale(scale, scale, -scale);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        Lighting.setupForEntityInInventory();

        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        drawPlatform(pose, buffers);
        graphics.flush();

        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        for (TutorialScene.Zone zone : scene.zones()) drawZone(pose, quads, scene, zone, tick);
        for (TutorialScene.Beam beam : scene.beams()) drawBeam(pose, quads, scene, beam, tick);
        graphics.flush();

        for (TutorialScene.Actor actor : scene.actors()) drawActor(pose, buffers, actor, scene.pose(actor, tick), tick);
        graphics.flush();

        collectOverlays(pose, scene, tick);
        pose.popPose();
        Lighting.setupFor3DItems();
    }

    private void drawPlatform(PoseStack pose, MultiBufferSource buffers) {
        BlockRenderDispatcher blocks = minecraft.getBlockRenderer();
        BlockState light = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState dark = Blocks.POLISHED_ANDESITE.defaultBlockState();
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                pose.pushPose();
                pose.translate(x - 0.5F, -1.0F, z - 0.5F);
                blocks.renderSingleBlock((x + z & 1) == 0 ? light : dark, pose, buffers, LIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
    }

    private static int withAlpha(int color, float factor) {
        int alpha = (int) ((color >>> 24) * factor);
        return alpha << 24 | color & 0xFFFFFF;
    }

    private static void quad(Matrix4f matrix, VertexConsumer quads, float x1, float z1, float x2, float z2, float x3, float z3, float x4, float z4, int color) {
        quads.addVertex(matrix, x1, GROUND_OFFSET, z1).setColor(color);
        quads.addVertex(matrix, x2, GROUND_OFFSET, z2).setColor(color);
        quads.addVertex(matrix, x3, GROUND_OFFSET, z3).setColor(color);
        quads.addVertex(matrix, x4, GROUND_OFFSET, z4).setColor(color);
    }

    /** Disque ou secteur au sol, avec un bord plus visible. */
    private void drawZone(PoseStack pose, VertexConsumer quads, TutorialScene scene, TutorialScene.Zone zone, float tick) {
        float alpha = TutorialScene.fade(tick, zone.from(), zone.to());
        if (alpha <= 0.0F) return;
        float centerX = (float) zone.x();
        float centerZ = (float) zone.z();
        double facing = 0.0D;
        if (zone.actor() != null) {
            TutorialScene.ActorPose actor = scene.pose(scene.actor(zone.actor()), tick);
            centerX = (float) actor.x();
            centerZ = (float) actor.z();
            facing = actor.yaw();
        }
        Matrix4f matrix = pose.last().pose();
        int fill = withAlpha(zone.color(), alpha * 0.5F);
        int edge = withAlpha(zone.color() | 0xFF000000, alpha);
        int segments = Math.max(4, (int) Math.ceil(zone.angle() / DEGREES_PER_SEGMENT));
        double start = Math.toRadians(-facing - zone.angle() / 2.0D);
        double step = Math.toRadians(zone.angle()) / segments;
        float radius = (float) zone.radius();
        float inner = Math.max(0.0F, radius - OUTLINE_WIDTH);
        for (int i = 0; i < segments; i++) {
            float sinA = (float) Math.sin(start + i * step);
            float cosA = (float) Math.cos(start + i * step);
            float sinB = (float) Math.sin(start + (i + 1) * step);
            float cosB = (float) Math.cos(start + (i + 1) * step);
            quad(matrix, quads, centerX, centerZ, centerX + inner * sinA, centerZ + inner * cosA,
                    centerX + inner * sinB, centerZ + inner * cosB, centerX, centerZ, fill);
            quad(matrix, quads, centerX + inner * sinA, centerZ + inner * cosA, centerX + radius * sinA, centerZ + radius * cosA,
                    centerX + radius * sinB, centerZ + radius * cosB, centerX + inner * sinB, centerZ + inner * cosB, edge);
        }
    }

    private void drawBeam(PoseStack pose, VertexConsumer quads, TutorialScene scene, TutorialScene.Beam beam, float tick) {
        float alpha = TutorialScene.fade(tick, beam.from(), beam.to());
        if (alpha <= 0.0F) return;
        double fromX = beam.fromX();
        double fromZ = beam.fromZ();
        double toX = beam.toX();
        double toZ = beam.toZ();
        if (beam.fromActor() != null) {
            TutorialScene.ActorPose actor = scene.pose(scene.actor(beam.fromActor()), tick);
            fromX = actor.x();
            fromZ = actor.z();
        }
        if (beam.toActor() != null) {
            TutorialScene.ActorPose actor = scene.pose(scene.actor(beam.toActor()), tick);
            toX = actor.x();
            toZ = actor.z();
        }
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-3D) return;
        float sideX = (float) (-dz / length * BEAM_WIDTH);
        float sideZ = (float) (dx / length * BEAM_WIDTH);
        quad(pose.last().pose(), quads, (float) fromX + sideX, (float) fromZ + sideZ, (float) fromX - sideX, (float) fromZ - sideZ,
                (float) toX - sideX, (float) toZ - sideZ, (float) toX + sideX, (float) toZ + sideZ, withAlpha(beam.color(), alpha));
    }

    private void drawActor(PoseStack pose, MultiBufferSource buffers, TutorialScene.Actor actor, TutorialScene.ActorPose state, float tick) {
        boolean zombie = actor.kind() == TutorialScene.ActorKind.ZOMBIE;
        HumanoidModel<?> model = zombie ? zombieModel : playerModel;
        applyPose(model, state, tick, zombie);
        if (model == playerModel) copyOuterLayer();
        int overlay = OverlayTexture.pack(0.0F, state.hurt() > 0.0F);

        pose.pushPose();
        pose.translate((float) state.x(), 0.0F, (float) state.z());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw()));
        pose.scale(-1.0F, -1.0F, 1.0F);
        if (!zombie) pose.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        pose.translate(0.0F, -MODEL_OFFSET, 0.0F);

        ResourceLocation texture = zombie ? ZOMBIE_TEXTURE : DefaultPlayerSkin.getDefaultTexture();
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(texture)), LIGHT, overlay);
        if (actor.armored()) drawArmor(pose, buffers, model, overlay);
        drawHeldItem(pose, buffers, model.rightArm, state.mainHand(), false, model);
        drawHeldItem(pose, buffers, model.leftArm, state.offHand(), true, model);
        pose.popPose();
    }

    private void drawArmor(PoseStack pose, MultiBufferSource buffers, HumanoidModel<?> model, int overlay) {
        for (HumanoidModel<LivingEntity> armor : List.of(armorOuter, armorInner)) {
            armor.head.copyFrom(model.head);
            armor.hat.copyFrom(model.head);
            armor.body.copyFrom(model.body);
            armor.rightArm.copyFrom(model.rightArm);
            armor.leftArm.copyFrom(model.leftArm);
            armor.rightLeg.copyFrom(model.rightLeg);
            armor.leftLeg.copyFrom(model.leftLeg);
            armor.hat.visible = false;
        }
        armorOuter.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(ARMOR_OUTER_TEXTURE)), LIGHT, overlay);
        armorInner.head.visible = false;
        armorInner.rightArm.visible = false;
        armorInner.leftArm.visible = false;
        armorInner.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(ARMOR_INNER_TEXTURE)), LIGHT, overlay);
    }

    /** Même placement que le calque vanilla des objets en main. */
    private void drawHeldItem(PoseStack pose, MultiBufferSource buffers, ModelPart arm, ItemStack stack, boolean leftHand, HumanoidModel<?> model) {
        if (stack.isEmpty()) return;
        pose.pushPose();
        model.translateToHand(leftHand ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate((leftHand ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
        ItemDisplayContext context = leftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        minecraft.getItemRenderer().renderStatic(null, stack, context, leftHand, pose, buffers, null, LIGHT, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    private void copyOuterLayer() {
        playerModel.hat.copyFrom(playerModel.head);
        playerModel.jacket.copyFrom(playerModel.body);
        playerModel.leftSleeve.copyFrom(playerModel.leftArm);
        playerModel.rightSleeve.copyFrom(playerModel.rightArm);
        playerModel.leftPants.copyFrom(playerModel.leftLeg);
        playerModel.rightPants.copyFrom(playerModel.rightLeg);
    }

    /** Mêmes courbes que le jeu pour la marche, les objets tenus, la garde et les coups. */
    private static void applyPose(HumanoidModel<?> model, TutorialScene.ActorPose state, float tick, boolean zombie) {
        for (ModelPart part : List.of(model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg)) part.resetPose();
        float walk = state.walkPhase();
        float amount = state.walkAmount();
        model.rightLeg.xRot = Mth.cos(walk) * 1.4F * amount;
        model.leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.4F * amount;
        model.rightArm.xRot = Mth.cos(walk + Mth.PI) * amount;
        model.leftArm.xRot = Mth.cos(walk) * amount;

        if (zombie) {
            model.rightArm.xRot = ZOMBIE_ARMS_PITCH;
            model.leftArm.xRot = ZOMBIE_ARMS_PITCH;
        } else {
            if (!state.mainHand().isEmpty()) model.rightArm.xRot = model.rightArm.xRot * 0.5F - HOLD_PITCH;
            if (!state.offHand().isEmpty()) model.leftArm.xRot = model.leftArm.xRot * 0.5F - HOLD_PITCH;
        }
        if (state.guardMain()) guard(model.rightArm, -1.0F);
        if (state.guardOff()) guard(model.leftArm, 1.0F);

        float bob = Mth.cos(tick * 0.09F) * 0.05F + 0.05F;
        float sway = Mth.sin(tick * 0.067F) * 0.05F;
        model.rightArm.zRot += bob;
        model.leftArm.zRot -= bob;
        model.rightArm.xRot += sway;
        model.leftArm.xRot -= sway;

        if (state.mainSwing() > 0.0F) swing(model, model.rightArm, state.mainSwing(), 1.0F);
        if (state.offSwing() > 0.0F) swing(model, model.leftArm, state.offSwing(), -1.0F);
    }

    private static void guard(ModelPart arm, float side) {
        arm.xRot = arm.xRot * 0.5F - GUARD_PITCH;
        arm.yRot = side * GUARD_YAW;
    }

    private static void swing(HumanoidModel<?> model, ModelPart arm, float time, float side) {
        float twist = Mth.sin(Mth.sqrt(time) * Mth.TWO_PI) * 0.2F * side;
        model.body.yRot = twist;
        float eased = 1.0F - time;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float lift = Mth.sin(eased * Mth.PI) * 1.2F + Mth.sin(time * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;
        arm.xRot -= lift;
        arm.yRot += twist * 2.0F;
        arm.zRot += Mth.sin(time * Mth.PI) * -0.4F * side;
    }

    private void collectOverlays(PoseStack pose, TutorialScene scene, float tick) {
        Matrix4f matrix = pose.last().pose();
        for (TutorialScene.Label label : scene.labels()) {
            float alpha = TutorialScene.fade(tick, label.from(), label.to());
            if (alpha <= 0.0F) continue;
            TutorialScene.ActorPose actor = scene.pose(scene.actor(label.actor()), tick);
            float rise = label.rise() ? RISE_BLOCKS * (tick - label.from()) / (label.to() - label.from()) : 0.0F;
            Vector3f point = new Vector3f((float) (actor.x() + label.x()), (float) label.y() + rise, (float) (actor.z() + label.z())).mulPosition(matrix);
            labels.add(new ScreenLabel(point.x + (label.rise() ? RISE_SIDE_PIXELS : 0.0F), point.y, label.text(), withAlpha(label.color(), Math.max(0.1F, alpha))));
        }
        for (TutorialScene.Spark spark : scene.sparks()) {
            if (tick < spark.tick() || tick >= spark.tick() + TutorialScene.SPARK_TICKS) continue;
            TutorialScene.ActorPose actor = scene.pose(scene.actor(spark.actor()), tick);
            Vector3f point = new Vector3f((float) actor.x(), 1.35F, (float) actor.z()).mulPosition(matrix);
            sparks.add(new ScreenSpark(point.x, point.y, (tick - spark.tick()) / TutorialScene.SPARK_TICKS, spark.color()));
        }
    }
}
