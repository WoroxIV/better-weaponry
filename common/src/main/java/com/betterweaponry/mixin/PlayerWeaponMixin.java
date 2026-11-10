package com.betterweaponry.mixin;

import com.betterweaponry.combat.IWeaponPlayer;
import com.betterweaponry.combat.WeaponPlayerState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ce que le mod retient d'un joueur : shurikens cassés et force du dernier coup. */
@Mixin(Player.class)
public abstract class PlayerWeaponMixin implements IWeaponPlayer {
    @Unique private static final String BROKEN_SHURIKENS_KEY = "BetterWeaponryBrokenShurikens";
    @Unique private final WeaponPlayerState betterweaponry$state = new WeaponPlayerState();

    @Override
    public WeaponPlayerState betterweaponry$weaponState() {
        return betterweaponry$state;
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
