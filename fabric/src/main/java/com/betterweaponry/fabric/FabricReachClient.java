package com.betterweaponry.fabric;

import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Ce que vise le joueur quand son arme change la portée : le jeu de base compte toujours 3 blocs (5 en créatif). */
public final class FabricReachClient {
    private static final double VANILLA_REACH = 3.0D;
    private static final double VANILLA_CREATIVE_REACH = 5.0D;

    private FabricReachClient() {
    }

    public static void pick(Minecraft minecraft, float partialTicks) {
        if (!BwCompat.reachActive()) return;
        LocalPlayer player = minecraft.player;
        Entity camera = minecraft.getCameraEntity();
        if (player == null || camera != player || minecraft.gameMode == null || player.isSpectator()) return;
        double reach = Platform.get().entityReach(player);
        if (reach == (minecraft.gameMode.hasFarPickRange() ? VANILLA_CREATIVE_REACH : VANILLA_REACH)) return;

        Vec3 eye = camera.getEyePosition(partialTicks);
        Vec3 look = camera.getViewVector(1.0F);
        HitResult block = camera.pick(minecraft.gameMode.getPickRange(), partialTicks, false);
        double maxDistanceSqr = reach * reach;
        if (block.getType() != HitResult.Type.MISS) maxDistanceSqr = Math.min(maxDistanceSqr, block.getLocation().distanceToSqr(eye));

        AABB searchBox = camera.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(camera, eye, eye.add(look.scale(reach)), searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(), maxDistanceSqr);
        minecraft.hitResult = entityHit != null ? entityHit : block;
        minecraft.crosshairPickEntity = entityHit != null && (entityHit.getEntity() instanceof LivingEntity || entityHit.getEntity() instanceof ItemFrame)
                ? entityHit.getEntity() : null;
    }
}
