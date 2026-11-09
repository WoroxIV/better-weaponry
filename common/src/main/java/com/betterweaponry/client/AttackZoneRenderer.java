package com.betterweaponry.client;

import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.PlayerCombatState;
import com.betterweaponry.combat.WeaponProfiles;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.data.ComboDefinition;
import com.betterweaponry.data.ComboStep;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Vue debug des zones d'attaque (option client) : pour régler les rayons et les angles à l'œil.
 * Cercle blanc : portée. Arcs cyan : zones de combo (doré pour le prochain coup). Cadre vert : balayage autour de la cible.
 */
public final class AttackZoneRenderer {
    private static final double GROUND_OFFSET = 0.05D;
    private static final double DEGREES_PER_SEGMENT = 6.0D;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int CYAN = 0xFF55FFFF;
    private static final int GOLD = 0xFFFFAA00;
    private static final float[] GREEN = {0.33F, 1.0F, 0.33F, 1.0F};
    private static final double FULL_CIRCLE = 360.0D;
    /** Mêmes dimensions verticales que le balayage du jeu. */
    private static final double VANILLA_SWEEP_RADIUS = 1.0D;
    private static final double VANILLA_SWEEP_HEIGHT = 0.25D;

    private AttackZoneRenderer() {
    }

    public static void render(PoseStack pose, Vec3 camera, float partialTick) {
        if (!ClientConfig.get(ClientConfig.SHOW_ATTACK_ZONES)) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        Optional<WeaponProfiles.Profile> profile = WeaponProfiles.of(player.getMainHandItem());
        if (profile.isEmpty()) return;

        Vec3 feet = player.getPosition(partialTick);
        double yaw = Math.toRadians(player.getViewYRot(partialTick));
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        pose.pushPose();
        pose.translate(feet.x - camera.x, feet.y - camera.y + GROUND_OFFSET, feet.z - camera.z);
        drawArc(pose, lines, player.entityInteractionRange(), FULL_CIRCLE, yaw, WHITE);
        Optional<ComboDefinition> combo = ComboSystem.activeCombo(player);
        if (combo.isPresent()) {
            List<ComboStep> steps = combo.get().steps();
            int next = Math.floorMod(PlayerCombatState.of(player).comboStep, steps.size());
            for (int i = 0; i < steps.size(); i++) {
                ComboStep step = steps.get(i);
                if (step.hasArea()) drawArc(pose, lines, step.radius(), step.angle(), yaw, i == next ? GOLD : CYAN);
            }
        }
        pose.popPose();

        Entity target = minecraft.crosshairPickEntity;
        if (target != null) drawSweepZone(pose, lines, profile.get().definition(), target, camera);
        buffers.endBatch(RenderType.lines());
    }

    private static void drawSweepZone(PoseStack pose, VertexConsumer lines, WeaponDefinition definition, Entity target, Vec3 camera) {
        boolean custom = WeaponData.rules().sweepActive();
        if (custom && !definition.sweep().enabled()) return;
        AABB zone = custom
                ? WeaponItem.sweepZone(definition, target)
                : target.getBoundingBox().inflate(VANILLA_SWEEP_RADIUS, VANILLA_SWEEP_HEIGHT, VANILLA_SWEEP_RADIUS);
        LevelRenderer.renderLineBox(pose, lines, zone.move(camera.reverse()), GREEN[0], GREEN[1], GREEN[2], GREEN[3]);
    }

    /** Arc centré sur le regard ; moins d'un tour complet, ses deux bords rejoignent le joueur. */
    private static void drawArc(PoseStack pose, VertexConsumer lines, double radius, double angle, double yaw, int color) {
        int segments = Math.max(1, (int) Math.ceil(angle / DEGREES_PER_SEGMENT));
        double start = yaw - Math.toRadians(angle / 2.0D);
        double step = Math.toRadians(angle) / segments;
        Vec3 first = pointAt(radius, start);
        Vec3 previous = first;
        for (int i = 1; i <= segments; i++) {
            Vec3 point = pointAt(radius, start + i * step);
            drawLine(pose, lines, previous, point, color);
            previous = point;
        }
        if (angle < FULL_CIRCLE) {
            drawLine(pose, lines, Vec3.ZERO, first, color);
            drawLine(pose, lines, Vec3.ZERO, previous, color);
        }
    }

    /** Même repère que le jeu pour un angle de vue : 0 = sud (+Z). */
    private static Vec3 pointAt(double radius, double yaw) {
        return new Vec3(-Math.sin(yaw) * radius, 0.0D, Math.cos(yaw) * radius);
    }

    private static void drawLine(PoseStack pose, VertexConsumer lines, Vec3 from, Vec3 to, int color) {
        Vec3 normal = to.subtract(from).normalize();
        PoseStack.Pose last = pose.last();
        lines.addVertex(last, (float) from.x, (float) from.y, (float) from.z).setColor(color).setNormal(last, (float) normal.x, (float) normal.y, (float) normal.z);
        lines.addVertex(last, (float) to.x, (float) to.y, (float) to.z).setColor(color).setNormal(last, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
