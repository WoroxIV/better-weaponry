package com.betterweaponry.client;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
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

/**
 * Icône des armes en 32x32 dont le dessin ne remplit pas l'image (24 pixels de large) : dans les interfaces, le dessin
 * est agrandi pour occuper le slot. Le reste (en main, au sol...) ne change pas.
 * <p>
 * L'agrandissement dépend de l'échelle de l'interface : si un nombre entier de pixels de l'écran par pixel du dessin
 * remplit presque tout le slot, tous les pixels ont la même taille. Sinon, remplir le slot passe en premier : le dessin
 * y est ajusté, à un pixel d'écran près pour ne jamais dépasser, quitte à avoir des pixels légèrement inégaux.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class WeaponIconModel extends BakedModelWrapper<BakedModel> {
    private static final int LARGE_TEXTURE = 32;
    private static final int SLOT_SIZE = 16;
    /** Largeur du dessin de ces armes, utilisée pour toutes même si l'une fait quelques pixels de plus. */
    private static final int DRAWING_SIZE = 24;
    /** En dessous de cette part du slot, on préfère remplir le slot plutôt que garder des pixels égaux. */
    private static final float MIN_SHARP_FILL = 0.9F;

    private final float centerX;
    private final float centerY;

    private WeaponIconModel(BakedModel original, float centerX, float centerY) {
        super(original);
        this.centerX = centerX;
        this.centerY = centerY;
    }

    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (WeaponType type : WeaponType.values()) {
            for (WeaponMaterial material : WeaponMaterials.all()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, material.itemName(type));
                models.computeIfPresent(ModelResourceLocation.inventory(id), (key, model) -> wrap(model));
            }
        }
    }

    /** Renvoie le modèle tel quel si sa texture n'est pas en 32x32 ou si son dessin remplit déjà l'image. */
    private static BakedModel wrap(BakedModel model) {
        SpriteContents sprite = model.getParticleIcon().contents();
        if (sprite.width() != LARGE_TEXTURE || sprite.height() != LARGE_TEXTURE) return model;

        int minX = LARGE_TEXTURE;
        int minY = LARGE_TEXTURE;
        int maxX = 0;
        int maxY = 0;
        for (int y = 0; y < LARGE_TEXTURE; y++) {
            for (int x = 0; x < LARGE_TEXTURE; x++) {
                if (sprite.isTransparent(0, x, y)) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + 1);
                maxY = Math.max(maxY, y + 1);
            }
        }
        int size = Math.max(maxX - minX, maxY - minY);
        if (size <= 0 || size >= LARGE_TEXTURE) return model;
        // Centre arrondi au pixel près : les bords des pixels du dessin restent alignés sur ceux de l'écran.
        return new WeaponIconModel(model, Math.floorDiv(minX + maxX, 2), Math.floorDiv(minY + maxY, 2));
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        if (context != ItemDisplayContext.GUI) return super.applyTransform(context, pose, leftHand);

        int guiScale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        float slot = SLOT_SIZE * guiScale;
        int whole = SLOT_SIZE * guiScale / DRAWING_SIZE;
        float screenPixelsPerTexel = whole * DRAWING_SIZE >= slot * MIN_SHARP_FILL ? whole : (slot - 1.0F) / DRAWING_SIZE;
        float scale = screenPixelsPerTexel * LARGE_TEXTURE / slot;

        pose.translate(-scale * (centerX / LARGE_TEXTURE - 0.5F), -scale * (0.5F - centerY / LARGE_TEXTURE), 0.0F);
        pose.scale(scale, scale, 1.0F);
        return this;
    }
}
