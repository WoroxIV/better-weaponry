package com.betterweaponry.mixin;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import com.betterweaponry.combat.ComboSystem;
import com.betterweaponry.combat.DualWieldSystem;
import com.betterweaponry.combat.HandSwap;
import com.betterweaponry.combat.ICombatPlayer;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.PlayerCombatState;
import com.betterweaponry.combat.WeaponProfiles;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.WeaponItem;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Donne à chaque joueur son état de combat ({@link PlayerCombatState}) et entoure ses attaques :
 * dual wield (la bonne main frappe) et combos (coups spéciaux).
 * Tourne côté client (prédiction) et côté serveur (dégâts réels) avec la même logique.
 */
@Mixin(Player.class)
public abstract class PlayerCombatMixin implements ICombatPlayer {

    @Unique private static final String BROKEN_SHURIKENS_KEY = "BetterWeaponryBrokenShurikens";
    @Unique private final PlayerCombatState betterweaponry$combat = new PlayerCombatState();

    /** Identifiant du bonus que le jeu donne aux joueurs en créatif. */
    @Unique private static final ResourceLocation CREATIVE_REACH_BONUS = ResourceLocation.withDefaultNamespace("creative_mode_entity_range");

    @Override
    public PlayerCombatState betterweaponry$combat() {
        return betterweaponry$combat;
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

    /**
     * Entoure toute la méthode attack() :
     * <ul>
     *   <li>combo : l'étape est décidée avant le coup et le combo avance après ({@link ComboSystem}) ;</li>
     *   <li>coup spécial : toujours porté par l'arme principale, avec ses bonus ;</li>
     *   <li>sinon, si c'est au tour de la main secondaire, les deux mains sont échangées pendant l'attaque,
     *       puis remises en place dans un finally (même en cas de crash ou de retour anticipé).</li>
     * </ul>
     */
    @WrapMethod(method = "attack")
    private void betterweaponry$attack(Entity target, Operation<Void> original) {
        Player player = (Player) (Object) this;
        PlayerCombatState state = betterweaponry$combat;
        // Appel imbriqué pendant un échange de mains ou un coup spécial : comportement vanilla.
        if (state.activeSwap != null || state.activeStep != null) {
            original.call(target);
            return;
        }

        ComboSystem.Hit hit = ComboSystem.beginHit(player, state);
        state.attackLanded = false;
        state.lastAttackStrength = hit.strength();
        boolean special = hit.special();
        boolean dualWielding = DualWieldSystem.isDualWielding(player);
        boolean offhandAttack = dualWielding && state.offhandTurn && !special;
        // Mémorisé pour que l'animation (swing) qui suit l'attaque montre la main qui a frappé.
        if (dualWielding) {
            state.justAttacked = true;
            state.lastAttackOffhand = offhandAttack;
            state.lastAttackSpecial = special;
        }

        try {
            if (special) {
                ComboSystem.performSpecialHit(player, target, hit, () -> original.call(target));
            } else if (offhandAttack) {
                state.activeSwap = HandSwap.begin(player);
                try {
                    original.call(target);
                } finally {
                    state.activeSwap.restore();
                    state.activeSwap = null;
                }
            } else {
                original.call(target);
            }
        } finally {
            ComboSystem.endHit(player, state, hit, target);
        }
    }

    /** Note si la cible principale a encaissé le coup. Le coup balayé passe par LivingEntity.hurt, il n'est donc pas compté ici. */
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean betterweaponry$rememberHit(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        boolean landed = original.call(target, source, amount);
        betterweaponry$combat.attackLanded = landed;
        return landed;
    }

    /** Rayon du coup balayé d'un objet adopté par un fichier d'arme ({@code WeaponItem} règle le sien lui-même). */
    @WrapOperation(method = "attack", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getSweepHitBox(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/AABB;"))
    private AABB betterweaponry$adoptedSweepZone(ItemStack stack, Player player, Entity target, Operation<AABB> original) {
        if (WeaponData.rules().sweepActive() && WeaponProfiles.isAdopted(stack)) {
            return WeaponItem.sweepZone(WeaponProfiles.definition(stack).orElseThrow(), target);
        }
        return original.call(stack, player, target);
    }

    /** Shurikens cassés en attente d'être ramassés : gardés avec les données du joueur. */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void betterweaponry$saveBrokenShurikens(CompoundTag tag, CallbackInfo ci) {
        if (betterweaponry$combat.brokenShurikens.isEmpty()) return;
        ListTag list = new ListTag();
        betterweaponry$combat.brokenShurikens.forEach(batch -> list.add(NbtUtils.createUUID(batch)));
        tag.put(BROKEN_SHURIKENS_KEY, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void betterweaponry$loadBrokenShurikens(CompoundTag tag, CallbackInfo ci) {
        List<UUID> broken = new ArrayList<>();
        tag.getList(BROKEN_SHURIKENS_KEY, Tag.TAG_INT_ARRAY).forEach(entry -> broken.add(NbtUtils.loadUUID(entry)));
        betterweaponry$combat.brokenShurikens = List.copyOf(broken);
    }

    /** Une parade parfaite au bouclier n'est jamais brisée par une hache. */
    @Inject(method = "disableShield", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$perfectParryKeepsShield(CallbackInfo ci) {
        if (ParrySystem.isInPerfectWindow((Player) (Object) this)) ci.cancel();
    }

    /**
     * Recharge : celle imposée par un coup spécial du combo, sinon en dual wield celle de l'arme qui va frapper,
     * divisée par le multiplicateur de la config. Sert à la force des coups (client et serveur) et à l'indicateur
     * de recharge du viseur.
     */
    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("HEAD"), cancellable = true)
    private void betterweaponry$attackDelay(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        Float comboDelay = ComboSystem.attackDelayOverride(player, betterweaponry$combat);
        if (comboDelay != null) {
            cir.setReturnValue(comboDelay);
        } else if (DualWieldSystem.isDualWielding(player)) {
            cir.setReturnValue(DualWieldSystem.attackStrengthDelay(player, betterweaponry$combat));
        }
    }
}
