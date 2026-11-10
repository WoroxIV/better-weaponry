package com.betterweaponry.fabric.mixin;

import com.betterweaponry.fabric.FabricAttributes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;

/** La portée du mod existe sur toutes les créatures (joueurs, mais aussi mobs qui tiendraient une arme), à sa valeur par défaut. */
@Mixin(AttributeSupplier.class)
public abstract class AttributeSupplierMixin {
    @Shadow @Final private Map<Attribute, AttributeInstance> instances;

    @ModifyReturnValue(method = "createInstance", at = @At("RETURN"))
    @Nullable
    private AttributeInstance betterweaponry$modAttributes(@Nullable AttributeInstance instance, Consumer<AttributeInstance> onDirty, Attribute attribute) {
        return instance == null && betterweaponry$isModAttribute(attribute) ? new AttributeInstance(attribute, onDirty) : instance;
    }

    /** Lecture de la valeur par défaut, tant que la créature n'a pas sa propre copie de l'attribut. */
    @Inject(method = "getAttributeInstance", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$modDefaults(Attribute attribute, CallbackInfoReturnable<AttributeInstance> cir) {
        if (!instances.containsKey(attribute) && betterweaponry$isModAttribute(attribute)) cir.setReturnValue(new AttributeInstance(attribute, instance -> { }));
    }

    @Unique
    private static boolean betterweaponry$isModAttribute(Attribute attribute) {
        return attribute == FabricAttributes.ENTITY_REACH.get();
    }
}
