package com.betterweaponry.entity;

import com.betterweaponry.combat.ModEnchantments;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shuriken lancé. C'est une flèche pour le jeu (type dans le tag {@code minecraft:arrows}) : Puissance, Frappe, Flamme,
 * Perforation et Tir multiple marchent sans code en plus. Contrairement à une flèche, il tombe au sol après avoir touché
 * une cible, pour pouvoir être récupéré.
 */
public class ShurikenEntity extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ShurikenEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(ShurikenEntity.class, EntityDataSerializers.BOOLEAN);
    /** Comme le trident Loyauté : attend quelques ticks planté avant de revenir. */
    private static final int RETURN_DELAY_TICKS = 4;
    /** Équivalent de Loyauté III : le retour doit rester rapide, les shurikens sont nombreux. */
    private static final int RETURN_STRENGTH = 3;
    private static final double POWER_DAMAGE_PER_LEVEL = 0.5D;
    private static final int FLAME_SECONDS = 100;
    private static final String ITEM_KEY = "Item";

    private boolean dealtDamage;
    /** Pendant l'impact, le jeu supprimerait le projectile : on le garde pour le faire tomber. */
    private boolean keepOnRemoval;
    private boolean removalRefused;
    private boolean returnSoundPlayed;

    public ShurikenEntity(EntityType<? extends ShurikenEntity> type, Level level) {
        super(type, level);
    }

    /**
     * @param recoverable le joueur peut le récupérer : le lancer a coûté un point de durabilité et ce n'est pas un extra de Tir multiple
     */
    public ShurikenEntity(Level level, LivingEntity owner, ItemStack stack, boolean recoverable) {
        super(ModEntities.SHURIKEN.get(), owner, level);
        setBaseDamage(WeaponData.gear().shurikens().baseDamage());
        applyEnchantments(stack);
        if (owner instanceof Player player && player.getAbilities().instabuild) {
            this.pickup = Pickup.CREATIVE_ONLY;
        } else if (!recoverable) {
            this.pickup = Pickup.DISALLOWED;
        }
        this.entityData.set(DATA_ITEM, stack.copyWithCount(1));
        this.entityData.set(DATA_RETURNING, this.pickup == Pickup.ALLOWED && ModEnchantments.level(level, stack, ModEnchantments.RETURNING) > 0);
    }

    /** Le jeu lit ces enchantements dans l'arc qui tire : ici, c'est l'objet lancé. */
    private void applyEnchantments(ItemStack stack) {
        int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
        if (power > 0) setBaseDamage(getBaseDamage() + power * POWER_DAMAGE_PER_LEVEL + POWER_DAMAGE_PER_LEVEL);
        int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
        if (punch > 0) setKnockback(punch);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) setSecondsOnFire(FLAME_SECONDS);
        int piercing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, stack);
        if (piercing > 0) setPierceLevel((byte) piercing);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_RETURNING, false);
    }

    /** L'objet lancé, pour le rendu. */
    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public boolean isReturning() {
        return this.entityData.get(DATA_RETURNING);
    }

    public boolean isStuck() {
        return this.inGround;
    }

    @Override
    public void tick() {
        if (this.inGroundTime > RETURN_DELAY_TICKS) this.dealtDamage = true;
        Entity owner = getOwner();
        if (isReturning() && (this.dealtDamage || isNoPhysics()) && owner != null) {
            if (isAcceptableOwner(owner)) {
                setNoPhysics(true);
                Vec3 toOwner = owner.getEyePosition().subtract(position());
                setPosRaw(getX(), getY() + toOwner.y * 0.015D * RETURN_STRENGTH, getZ());
                if (level().isClientSide) this.yOld = getY();
                setDeltaMovement(getDeltaMovement().scale(0.95D).add(toOwner.normalize().scale(0.05D * RETURN_STRENGTH)));
                if (!this.returnSoundPlayed) {
                    playSound(SoundEvents.TRIDENT_RETURN, 2.0F, 1.6F);
                    this.returnSoundPlayed = true;
                }
            } else if (!level().isClientSide) {
                this.entityData.set(DATA_RETURNING, false);
                setNoPhysics(false);
            }
        }
        super.tick();
    }

    private static boolean isAcceptableOwner(Entity owner) {
        return owner.isAlive() && !(owner instanceof ServerPlayer player && player.isSpectator());
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        return this.dealtDamage ? null : super.findHitEntity(startVec, endVec);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Un shuriken qu'on ne peut pas récupérer (mob, Tir multiple) disparaît à l'impact, comme une flèche.
        this.keepOnRemoval = this.pickup == Pickup.ALLOWED;
        try {
            super.onHitEntity(result);
        } finally {
            this.keepOnRemoval = false;
        }
        if (this.removalRefused) {
            this.removalRefused = false;
            this.dealtDamage = true;
            setDeltaMovement(getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.keepOnRemoval && reason == RemovalReason.DISCARDED) {
            this.removalRefused = true;
            return;
        }
        super.remove(reason);
    }

    /** Une flèche resterait visible plantée dans la cible : pas un shuriken. */
    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (!level().isClientSide && getPierceLevel() <= 0 && target.getArrowCount() > 0) target.setArrowCount(target.getArrowCount() - 1);
    }

    @Override
    protected boolean tryPickup(Player player) {
        if (this.pickup != Pickup.ALLOWED) return super.tryPickup(player);
        if (!ownedBy(player)) return false;
        if (ShurikenItem.recover(player, getItem())) return true;
        if (!isNoPhysics()) return false;
        // Revenu vers un joueur qui ne peut pas le reprendre : il disparaît au lieu de le suivre sans fin, sans perdre l'objet.
        ShurikenItem.dropIfBroken(player, getItem());
        return true;
    }

    /** Ramassage direct, pour les tests. */
    public boolean tryRecover(Player player) {
        return tryPickup(player);
    }

    /** Vide : un shuriken ne redevient jamais un objet au sol, il rend un point de durabilité (voir {@link ShurikenItem#recover}). */
    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected void tickDespawn() {
        if (!isReturning()) super.tickDespawn();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("DealtDamage", this.dealtDamage);
        compound.putBoolean("Returning", isReturning());
        compound.put(ITEM_KEY, getItem().save(new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.dealtDamage = compound.getBoolean("DealtDamage");
        this.entityData.set(DATA_RETURNING, compound.getBoolean("Returning"));
        this.entityData.set(DATA_ITEM, ItemStack.of(compound.getCompound(ITEM_KEY)));
    }
}
