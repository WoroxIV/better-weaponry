package com.betterweaponry.combat;

import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponShieldItem;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Famille de matériau d'une arme, qui décide du son et des particules d'une parade parfaite.
 * Les sons sont dans assets/better_weaponry/sounds.json : un pack de ressources peut les remplacer.
 */
public enum ParryMaterial {
    WOOD("wood", List.of(blocks(Blocks.OAK_PLANKS))),
    STONE("stone", List.of(blocks(Blocks.STONE))),
    METAL("metal", List.of(ParticleTypes.CRIT, ParticleTypes.ELECTRIC_SPARK)),
    GOLD("gold", List.of(blocks(Blocks.GOLD_BLOCK), ParticleTypes.ELECTRIC_SPARK)),
    DIAMOND("diamond", List.of(blocks(Blocks.DIAMOND_BLOCK), ParticleTypes.END_ROD)),
    NETHERITE("netherite", List.of(ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.LARGE_SMOKE));

    private final String id;
    private final List<ParticleOptions> particles;

    ParryMaterial(String id, List<ParticleOptions> particles) {
        this.id = id;
        this.particles = particles;
    }

    public String id() {
        return id;
    }

    public List<ParticleOptions> particles() {
        return particles;
    }

    /** Famille de cet objet. Tout ce qui n'est pas reconnu (matériaux d'autres mods, objets sans palier) est du métal. */
    public static ParryMaterial of(ItemStack stack) {
        if (stack.getItem() instanceof WeaponItem weapon) return of(weapon.getMaterial());
        if (stack.getItem() instanceof WeaponShieldItem shield) return of(shield.getMaterial());
        if (stack.is(Items.SHIELD)) return WOOD;
        if (stack.getItem() instanceof TieredItem tiered) return of(tiered.getTier());
        return METAL;
    }

    private static ParryMaterial of(WeaponMaterial material) {
        if (material == WeaponMaterials.WOODEN) return WOOD;
        if (material == WeaponMaterials.STONE) return STONE;
        if (material == WeaponMaterials.GOLDEN) return GOLD;
        if (material == WeaponMaterials.DIAMOND) return DIAMOND;
        if (material == WeaponMaterials.NETHERITE) return NETHERITE;
        return METAL;
    }

    private static ParryMaterial of(Tier tier) {
        if (tier == Tiers.WOOD) return WOOD;
        if (tier == Tiers.STONE) return STONE;
        if (tier == Tiers.GOLD) return GOLD;
        if (tier == Tiers.DIAMOND) return DIAMOND;
        if (tier == Tiers.NETHERITE) return NETHERITE;
        return METAL;
    }

    private static ParticleOptions blocks(net.minecraft.world.level.block.Block block) {
        return new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
    }
}
