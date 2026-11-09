package com.betterweaponry.fabric.mixin;

import com.betterweaponry.registry.ModAttributes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/** Toutes les créatures ont la pénétration d'armure (valeur 0) : joueurs, mais aussi mobs qui tiendraient une arme. */
@Mixin(AttributeSupplier.class)
public abstract class AttributeSupplierMixin {

    @ModifyReturnValue(method = "createInstance", at = @At("RETURN"))
    @Nullable
    private AttributeInstance betterweaponry$armorPenetration(@Nullable AttributeInstance instance, Consumer<AttributeInstance> onDirty, Holder<Attribute> attribute) {
        return instance == null && attribute.equals(ModAttributes.ARMOR_PENETRATION.holder()) ? new AttributeInstance(attribute, onDirty) : instance;
    }
}
