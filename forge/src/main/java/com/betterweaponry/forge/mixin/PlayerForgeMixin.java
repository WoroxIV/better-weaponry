package com.betterweaponry.forge.mixin;

import com.betterweaponry.combat.CombatEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ce que Forge décide dans le joueur à partir de l'objet seul, sans demander au mod. */
@Mixin(Player.class)
public abstract class PlayerForgeMixin {

    /** Armes sans coup balayé, et coup spécial en zone qui remplace le balayage. */
    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canPerformAction(Lnet/minecraftforge/common/ToolAction;)Z"))
    private boolean betterweaponry$mayNotSweep(ItemStack stack, ToolAction action, Operation<Boolean> original) {
        return original.call(stack, action) && !(action == ToolActions.SWORD_SWEEP && CombatEvents.preventsSweep((Player) (Object) this));
    }

    /** Forge ne demande qu'à l'objet : les objets adoptés par une arme qui désactive les boucliers (champ "items") comptent aussi. */
    @WrapOperation(method = "blockUsingShield", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canDisableShield(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean betterweaponry$adoptedDisablesShield(ItemStack weapon, ItemStack shield, LivingEntity entity, LivingEntity attacker, Operation<Boolean> original) {
        return original.call(weapon, shield, entity, attacker) || attacker.canDisableShield();
    }
}
