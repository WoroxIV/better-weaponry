package com.betterweaponry.item;

import com.betterweaponry.data.WeaponData;
import com.betterweaponry.entity.ShurikenEntity;
import com.betterweaponry.registry.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shuriken : lancé au clic droit, chaque lancer retire un point de durabilité et le ramasser le rend.
 * Le matériau change la durabilité (celle des outils divisée par 3) et la vitesse, donc les dégâts, comme pour les arcs.
 */
public class ShurikenItem extends Item implements RangedWeapon {
    private static final int DURABILITY_DIVISOR = 3;
    private static final String BATCH_KEY = "better_weaponry_shuriken";
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final float THROW_INACCURACY = 1.0F;
    /** Les mobs visent plus haut de 20 % de la distance pour une flèche à 1,6 de vitesse : la marge varie selon la vitesse. */
    private static final double MOB_REFERENCE_SPEED = 1.6D;
    private static final float MOB_INACCURACY = 6.0F;

    private final WeaponMaterial material;

    public ShurikenItem(WeaponMaterial material, Item.Properties properties) {
        super(configure(material, properties));
        this.material = material;
    }

    private static Item.Properties configure(WeaponMaterial material, Item.Properties properties) {
        properties.durability(Math.max(1, material.tier().getUses() / DURABILITY_DIVISOR));
        if (material.isFireResistant()) properties.fireResistant();
        return properties;
    }

    @Override
    public WeaponMaterial getMaterial() {
        return material;
    }

    /** Vitesse de lancer selon le matériau. */
    public double throwSpeed() {
        return WeaponData.gear().shurikens().throwSpeed() * speedMultiplier();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            throwFrom(serverLevel, player, hand, stack);
            player.getCooldowns().addCooldown(this, (int) Math.round(WeaponData.gear().shurikens().cooldownSeconds() * TICKS_PER_SECOND));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /**
     * Un lancer : le premier projectile est récupérable si le lancer a coûté un point de durabilité ; ceux ajoutés par
     * Tir multiple ne se ramassent jamais, comme en vanilla.
     */
    public void throwFrom(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        UUID batch = batchOf(stack).orElseGet(() -> assignBatch(stack));
        ItemStack thrown = stack.copyWithCount(1);
        int count = EnchantmentHelper.processProjectileCount(level, stack, player, 1);
        float spread = EnchantmentHelper.processProjectileSpread(level, stack, player, 0.0F);

        int damageBefore = stack.getDamageValue();
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        boolean broke = stack.isEmpty();
        boolean paid = broke || stack.getDamageValue() > damageBefore;
        if (broke) rememberBroken(player, batch);

        // Même répartition que le Tir multiple d'une arbalète : un au centre, les autres de chaque côté.
        float step = count == 1 ? 0.0F : 2.0F * spread / (count - 1);
        float side = 1.0F;
        float start = ((count - 1) % 2) * step / 2.0F;
        for (int i = 0; i < count; i++) {
            float angle = start + side * ((i + 1) / 2) * step;
            side = -side;
            ShurikenEntity shuriken = new ShurikenEntity(level, player, thrown, i == 0 && paid);
            shuriken.shootFromRotation(player, player.getXRot(), player.getYRot() + angle, 0.0F, (float) throwSpeed(), THROW_INACCURACY);
            level.addFreshEntity(shuriken);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    /** Lancer d'un mob : ne s'use pas et ne se ramasse pas. Vise au-dessus de la cible selon la vitesse. */
    public void throwFromMob(ServerLevel level, Mob mob, LivingEntity target, ItemStack stack) {
        ShurikenEntity shuriken = new ShurikenEntity(level, mob, stack.copyWithCount(1), false);
        double speed = throwSpeed();
        double dx = target.getX() - shuriken.getX();
        double dz = target.getZ() - shuriken.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double lift = RangedWeapon.MOB_AIM_LIFT * (MOB_REFERENCE_SPEED / speed) * (MOB_REFERENCE_SPEED / speed);
        shuriken.shoot(dx, target.getY(1.0D / 3.0D) - shuriken.getY() + horizontal * lift, dz, (float) speed, MOB_INACCURACY);
        level.addFreshEntity(shuriken);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 0.6F, 1.6F);
    }

    /**
     * Ramassage : rend un point à l'objet d'origine. S'il a été cassé par ce lancer, il est recréé à 1 point de durabilité,
     * avec ses enchantements. S'il a seulement été rangé ailleurs, rien ne se passe : ramasser ne doit pas pouvoir le copier.
     *
     * @return vrai si le shuriken au sol a été récupéré
     */
    public static boolean recover(Player player, ItemStack thrown) {
        Optional<UUID> batch = batchOf(thrown);
        if (batch.isEmpty()) return false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack held = player.getInventory().getItem(slot);
            if (held.getItem() instanceof ShurikenItem && batch.equals(batchOf(held))) {
                if (held.isDamaged()) held.setDamageValue(held.getDamageValue() - 1);
                return true;
            }
        }
        List<UUID> broken = player.getData(ModAttachments.BROKEN_SHURIKENS);
        if (!broken.contains(batch.get())) return false;
        ItemStack recreated = thrown.copyWithCount(1);
        recreated.setDamageValue(recreated.getMaxDamage() - 1);
        if (!player.getInventory().add(recreated)) return false;
        List<UUID> remaining = new ArrayList<>(broken);
        remaining.remove(batch.get());
        player.setData(ModAttachments.BROKEN_SHURIKENS, List.copyOf(remaining));
        return true;
    }

    private static void rememberBroken(Player player, UUID batch) {
        List<UUID> broken = new ArrayList<>(player.getData(ModAttachments.BROKEN_SHURIKENS));
        broken.add(batch);
        player.setData(ModAttachments.BROKEN_SHURIKENS, List.copyOf(broken));
    }

    /** Identifie cet objet parmi tous les shurikens : les projectiles savent ainsi à qui rendre leur point. */
    private static Optional<UUID> batchOf(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(BATCH_KEY) ? Optional.of(tag.getUUID(BATCH_KEY)) : Optional.empty();
    }

    private static UUID assignBatch(ItemStack stack) {
        UUID batch = UUID.randomUUID();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(BATCH_KEY, batch));
        return batch;
    }

    @Override
    public int getEnchantmentValue() {
        return material.tier().getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return material.tier().getRepairIngredient().test(repair);
    }

    /** Il accepte les enchantements des arcs et des arbalètes, sauf ceux qui n'ont pas de sens sur un objet lancé. */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.INFINITY) || enchantment.is(Enchantments.QUICK_CHARGE)) return false;
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addSpeedLine(tooltip);
    }
}
