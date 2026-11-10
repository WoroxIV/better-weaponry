package com.betterweaponry.mixin;

import com.betterweaponry.combat.IWeaponPlayer;
import com.betterweaponry.combat.WeaponPlayerState;
import com.betterweaponry.item.WeaponItem;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ce que le mod retient d'un joueur (shurikens cassés, force du dernier coup), et la portée exacte de ses armes en créatif. */
@Mixin(Player.class)
public abstract class PlayerWeaponMixin implements IWeaponPlayer {
    @Unique private static final String BROKEN_SHURIKENS_KEY = "BetterWeaponryBrokenShurikens";
    @Unique private static final ResourceLocation CREATIVE_REACH_BONUS = ResourceLocation.withDefaultNamespace("creative_mode_entity_range");
    @Unique private final WeaponPlayerState betterweaponry$state = new WeaponPlayerState();

    @Override
    public WeaponPlayerState betterweaponry$weaponState() {
        return betterweaponry$state;
    }

    /** Le mode créatif ajoute 2 blocs de portée : une arme du mod porte exactement à la distance affichée. */
    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    private double betterweaponry$exactWeaponReach(double reach) {
        Player player = (Player) (Object) this;
        if (!WeaponItem.hasExactReach(player)) return reach;
        AttributeInstance range = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeModifier creativeBonus = range == null ? null : range.getModifier(CREATIVE_REACH_BONUS);
        return creativeBonus == null ? reach : reach - creativeBonus.amount();
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void betterweaponry$rememberStrength(Entity target, CallbackInfo ci) {
        betterweaponry$state.lastAttackStrength = ((Player) (Object) this).getAttackStrengthScale(0.5F);
    }

    /** Shurikens cassés en attente d'être ramassés : gardés avec les données du joueur. */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void betterweaponry$saveBrokenShurikens(CompoundTag tag, CallbackInfo ci) {
        if (betterweaponry$state.brokenShurikens.isEmpty()) return;
        ListTag list = new ListTag();
        betterweaponry$state.brokenShurikens.forEach(batch -> list.add(NbtUtils.createUUID(batch)));
        tag.put(BROKEN_SHURIKENS_KEY, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void betterweaponry$loadBrokenShurikens(CompoundTag tag, CallbackInfo ci) {
        List<UUID> broken = new ArrayList<>();
        tag.getList(BROKEN_SHURIKENS_KEY, Tag.TAG_INT_ARRAY).forEach(entry -> broken.add(NbtUtils.loadUUID(entry)));
        betterweaponry$state.brokenShurikens = List.copyOf(broken);
    }
}
