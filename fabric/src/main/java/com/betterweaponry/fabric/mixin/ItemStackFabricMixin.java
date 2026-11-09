package com.betterweaponry.fabric.mixin;

import com.betterweaponry.combat.AdoptedItemEvents;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Bonus ajoutés aux objets adoptés par un fichier d'arme, et affichage de la pénétration d'armure en pourcentage. */
@Mixin(ItemStack.class)
public abstract class ItemStackFabricMixin {
    @Unique private static final double PERCENT = 100.0D;

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
    private void betterweaponry$adoptedModifiers(EquipmentSlotGroup group, BiConsumer<Holder<Attribute>, AttributeModifier> action, CallbackInfo ci) {
        if (group == EquipmentSlotGroup.MAINHAND) AdoptedItemEvents.collectModifiers((ItemStack) (Object) this, action);
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
    private void betterweaponry$adoptedModifiers(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> action, CallbackInfo ci) {
        if (slot == EquipmentSlot.MAINHAND) AdoptedItemEvents.collectModifiers((ItemStack) (Object) this, action);
    }

    @Inject(method = "addModifierTooltip", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$armorPenetrationLine(Consumer<Component> output, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier, CallbackInfo ci) {
        if (!attribute.equals(ModAttributes.ARMOR_PENETRATION.holder())) return;
        output.accept(Component.translatable("attribute.modifier.equals.1",
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(modifier.amount() * PERCENT),
                Component.translatable(attribute.value().getDescriptionId())).withStyle(ChatFormatting.DARK_GREEN));
        ci.cancel();
    }
}
