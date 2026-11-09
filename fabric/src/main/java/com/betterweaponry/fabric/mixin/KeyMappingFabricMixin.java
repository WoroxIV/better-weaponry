package com.betterweaponry.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Une seule action par touche : "Détails de l'arme", sur Maj par défaut, volerait la touche pour s'accroupir. Elle reste
 * réglable dans les options mais n'entre pas dans cette table, car le mod lit directement l'état du clavier dans les menus.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingFabricMixin {
    @Unique private static final String WEAPON_DETAILS = "key.better_weaponry.weapon_details";

    @WrapOperation(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1))
    private Object betterweaponry$keepSneakKey(Map<Object, Object> map, Object key, Object value, Operation<Object> original) {
        return betterweaponry$put(map, key, value, original);
    }

    @WrapOperation(method = "resetMapping", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object betterweaponry$keepSneakKeyAfterReset(Map<Object, Object> map, Object key, Object value, Operation<Object> original) {
        return betterweaponry$put(map, key, value, original);
    }

    @Unique
    private static Object betterweaponry$put(Map<Object, Object> map, Object key, Object value, Operation<Object> original) {
        return value instanceof KeyMapping mapping && WEAPON_DETAILS.equals(mapping.getName()) ? null : original.call(map, key, value);
    }
}
