package com.betterweaponry.item;

import com.betterweaponry.data.WeaponData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Ce que les arcs et arbalètes du mod ont en commun. Le matériau ne change que la vitesse du projectile
 * (les dégâts d'une flèche dépendent de sa vitesse) et la durabilité. L'or fait exception : il tire plus vite.
 */
public interface RangedWeapon {
    /** En vanilla, les mobs visent plus haut de 20 % de la distance pour compenser la chute de la flèche. */
    double MOB_AIM_LIFT = 0.2D;
    /** Rapport de durabilité de référence : celle des outils en fer. */
    double IRON_TOOL_DURABILITY = 250.0D;

    WeaponMaterial getMaterial();

    /** Multiplicateur de vitesse du projectile. */
    default double speedMultiplier() {
        return WeaponData.gear().projectileSpeedMultiplier(getMaterial());
    }

    /** Vitesse de bandage ou de chargement par rapport au vanilla (2 = deux fois plus rapide). */
    default double drawSpeed() {
        return getMaterial() == WeaponMaterials.GOLDEN ? 1.0D / WeaponData.gear().goldenDrawTime() : 1.0D;
    }

    /** Seuls les joueurs passent par ici : les tirs des mobs sont corrigés séparément pour qu'ils visent juste. */
    default float velocityFor(LivingEntity shooter, float velocity) {
        return shooter instanceof Player ? (float) (velocity * speedMultiplier()) : velocity;
    }

    /**
     * Durabilité : celle de l'arc ou de l'arbalète vanilla, suivant le rapport entre les outils du matériau et ceux
     * en fer. Le bois et la pierre gardent la valeur vanilla : leur rapport les rendrait inutilisables.
     */
    static int durabilityOf(WeaponMaterial material, int vanillaDurability) {
        if (material == WeaponMaterials.WOODEN || material == WeaponMaterials.STONE) return vanillaDurability;
        return (int) Math.round(vanillaDurability * material.tier().getUses() / IRON_TOOL_DURABILITY);
    }

    /**
     * Tir d'un mob avec une arme du mod : le mob a déjà visé pour une flèche normale. On accélère la flèche et on réduit
     * sa marge en hauteur, car une flèche plus rapide tombe moins (la chute varie comme l'inverse du carré de la vitesse).
     */
    static Vec3 mobShotVelocity(Vec3 velocity, double speedMultiplier) {
        double horizontal = velocity.horizontalDistance();
        if (horizontal < 1.0E-6D) return velocity.scale(speedMultiplier);
        double slope = velocity.y / horizontal - MOB_AIM_LIFT + MOB_AIM_LIFT / (speedMultiplier * speedMultiplier);
        return new Vec3(velocity.x, slope * horizontal, velocity.z).normalize().scale(velocity.length() * speedMultiplier);
    }
}
