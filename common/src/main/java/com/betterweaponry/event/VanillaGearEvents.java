package com.betterweaponry.event;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.combat.MobThrowShurikenGoal;
import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.item.ShurikenItem;
import com.betterweaponry.entity.ShurikenEntity;
import com.betterweaponry.item.MobEquipment;
import com.betterweaponry.item.RangedWeapon;
import com.betterweaponry.item.VanillaGear;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.registry.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;
import java.util.Map;

/** Convertit l'équipement vanilla des joueurs à la connexion, et adapte les échanges des villageois. */
public final class VanillaGearEvents {
    private static final int THROW_GOAL_PRIORITY = 2;
    private static final int SHIELD_MAX_TRADES = 12;
    private static final int SHIELD_XP = 10;
    private static final float SHIELD_PRICE_MULTIPLIER = 0.05F;

    /** Matériau vendu à ce niveau (1 = novice, 5 = maître). La netherite ne s'achète jamais. */
    private static final Map<Integer, WeaponMaterial> MATERIAL_BY_LEVEL = Map.of(1, WeaponMaterials.WOODEN, 2, WeaponMaterials.STONE,
            3, WeaponMaterials.IRON, 4, WeaponMaterials.DIAMOND, 5, WeaponMaterials.DIAMOND);
    /** Le niveau 3 vend déjà un bouclier en vanilla : seul celui-là est remplacé, les autres sont ajoutés. */
    private static final Map<Integer, Integer> EMERALDS_BY_NEW_LEVEL = Map.of(1, 3, 2, 5, 4, 14);

    /** Plus le matériau vendu est bon, plus l'échange coûte cher. */
    private static final Map<WeaponMaterial, Integer> PRICE_MULTIPLIER_BY_MATERIAL = Map.of(WeaponMaterials.WOODEN, 1, WeaponMaterials.STONE, 1,
            WeaponMaterials.IRON, 2, WeaponMaterials.DIAMOND, 3);
    private static final int MAX_EMERALDS = 64;

    private VanillaGearEvents() {
    }

    public static void onLogin(Player player) {
        if (WeaponData.gear().replaceVanillaGear()) VanillaGear.convertInventory(player);
    }

    /** L'armurier (boucliers) et le fléchier (arcs, arbalètes) vendent un meilleur matériau à chaque niveau. */
    public static void onVillagerTrades(VillagerProfession profession, Int2ObjectMap<List<VillagerTrades.ItemListing>> trades) {
        boolean armorer = profession == VillagerProfession.ARMORER;
        if (!armorer && profession != VillagerProfession.FLETCHER) return;
        MATERIAL_BY_LEVEL.forEach((level, material) -> {
            List<VillagerTrades.ItemListing> listings = trades.get(level.intValue());
            if (listings == null) return;
            listings.replaceAll(listing -> sellingMaterial(listing, material));
            Integer emeralds = EMERALDS_BY_NEW_LEVEL.get(level);
            if (armorer && emeralds != null) {
                listings.add(sellsShield(emeralds, material));
            }
        });
    }

    /** Armes vanilla des mobs converties, et flèches des mobs adaptées au matériau de leur arme. */
    public static void onEntityJoin(Entity entity, boolean loadedFromDisk) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof Mob mob) {
            if (!loadedFromDisk) MobEquipment.onFreshSpawn(mob, WeaponData.gear());
            VanillaGear.convertMobHands(mob, WeaponData.gear());
            if (BwCompat.mobBehaviorsActive() && mob instanceof AbstractSkeleton skeleton && mob.getMainHandItem().getItem() instanceof ShurikenItem) {
                skeleton.goalSelector.addGoal(THROW_GOAL_PRIORITY, new MobThrowShurikenGoal(skeleton));
            }
        } else if (!loadedFromDisk && entity instanceof AbstractArrow arrow && !(arrow instanceof ShurikenEntity) && arrow.getOwner() instanceof Mob shooter) {
            RangedWeapon weapon = rangedWeaponOf(shooter);
            if (weapon != null) arrow.setDeltaMovement(RangedWeapon.mobShotVelocity(arrow.getDeltaMovement(), weapon.speedMultiplier()));
        }
    }

    /** Une flèche ne sait pas de quelle arme elle vient : c'est celle que le tireur tient. */
    @Nullable
    private static RangedWeapon rangedWeaponOf(Mob mob) {
        if (mob.getMainHandItem().getItem() instanceof RangedWeapon weapon) return weapon;
        return mob.getOffhandItem().getItem() instanceof RangedWeapon weapon ? weapon : null;
    }

    private static VillagerTrades.ItemListing sellsShield(int emeralds, WeaponMaterial material) {
        return (trader, random) -> new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(ModItems.shield(material).get()), SHIELD_MAX_TRADES, SHIELD_XP, SHIELD_PRICE_MULTIPLIER);
    }

    /** Même échange, mais qui donne la version du mod si l'objet vendu est un objet vanilla remplacé. */
    private static VillagerTrades.ItemListing sellingMaterial(VillagerTrades.ItemListing listing, WeaponMaterial material) {
        return (trader, random) -> {
            MerchantOffer offer = listing.getOffer(trader, random);
            return offer == null ? null : convertOffer(offer, material);
        };
    }

    /** Offre avec la version du mod si l'objet vendu est un objet vanilla remplacé, sinon l'offre d'origine. */
    public static MerchantOffer convertOffer(MerchantOffer offer, WeaponMaterial material) {
        if (!WeaponData.gear().replaceVanillaGear() || !VanillaGear.isReplaced(offer.getResult())) return offer;
        ItemStack cost = offer.getBaseCostA();
        int emeralds = Math.min(MAX_EMERALDS, cost.getCount() * PRICE_MULTIPLIER_BY_MATERIAL.getOrDefault(material, 1));
        return new MerchantOffer(cost.copyWithCount(emeralds), offer.getCostB(), VanillaGear.convert(offer.getResult(), material),
                offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier());
    }
}
