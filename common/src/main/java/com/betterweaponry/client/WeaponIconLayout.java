package com.betterweaponry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BakedModel;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Icône des armes en 32x32 dont le dessin ne remplit pas l'image (24 pixels de large) : dans les interfaces, le dessin
 * est agrandi pour occuper le slot.
 * <p>
 * L'agrandissement dépend de l'échelle de l'interface : si un nombre entier de pixels de l'écran par pixel du dessin
 * remplit presque tout le slot, tous les pixels ont la même taille. Sinon, remplir le slot passe en premier : le dessin
 * y est ajusté, à un pixel d'écran près pour ne jamais dépasser, quitte à avoir des pixels légèrement inégaux.
 */
public final class WeaponIconLayout {
    private static final int LARGE_TEXTURE = 32;
    private static final int SLOT_SIZE = 16;
    /** Largeur du dessin de ces armes, utilisée pour toutes même si l'une fait quelques pixels de plus. */
    private static final int DRAWING_SIZE = 24;
    /** En dessous de cette part du slot, on préfère remplir le slot plutôt que garder des pixels égaux. */
    private static final float MIN_SHARP_FILL = 0.9F;
    /** Résultats déjà calculés, oubliés avec l'atlas quand les textures sont rechargées (thread de rendu uniquement). */
    private static final Map<SpriteContents, Optional<WeaponIconLayout>> CACHE = new WeakHashMap<>();

    private final float centerX;
    private final float centerY;

    private WeaponIconLayout(float centerX, float centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    /** Vide si la texture n'est pas en 32x32 ou si son dessin remplit déjà l'image. */
    public static Optional<WeaponIconLayout> of(BakedModel model) {
        SpriteContents sprite = model.getParticleIcon().contents();
        if (sprite.width() != LARGE_TEXTURE || sprite.height() != LARGE_TEXTURE) return Optional.empty();

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
        if (size <= 0 || size >= LARGE_TEXTURE) return Optional.empty();
        // Centre arrondi au pixel près : les bords des pixels du dessin restent alignés sur ceux de l'écran.
        return Optional.of(new WeaponIconLayout(Math.floorDiv(minX + maxX, 2), Math.floorDiv(minY + maxY, 2)));
    }

    /** Même résultat que {@link #of}, sans refaire le calcul à chaque image. */
    public static Optional<WeaponIconLayout> cached(BakedModel model) {
        return CACHE.computeIfAbsent(model.getParticleIcon().contents(), sprite -> of(model));
    }

    /** Agrandit et centre le dessin dans le slot, à la place de la transformation normale de l'interface. */
    public void apply(PoseStack pose) {
        int guiScale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        float slot = SLOT_SIZE * guiScale;
        int whole = SLOT_SIZE * guiScale / DRAWING_SIZE;
        float screenPixelsPerTexel = whole * DRAWING_SIZE >= slot * MIN_SHARP_FILL ? whole : (slot - 1.0F) / DRAWING_SIZE;
        float scale = screenPixelsPerTexel * LARGE_TEXTURE / slot;

        pose.translate(-scale * (centerX / LARGE_TEXTURE - 0.5F), -scale * (0.5F - centerY / LARGE_TEXTURE), 0.0F);
        pose.scale(scale, scale, 1.0F);
    }
}
