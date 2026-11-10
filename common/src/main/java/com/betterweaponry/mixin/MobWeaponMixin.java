package com.betterweaponry.mixin;

import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Un mob frappe avec les dégâts d'une épée du matériau de son arme : il frappe au rythme de son espèce, quelle que soit
 * la vitesse de l'arme, et une arme lente mais puissante serait bien plus dangereuse entre ses mains.
 */
@Mixin(Mob.class)
public abstract class MobWeaponMixin {
    @Unique private static final ResourceLocation SWORD_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("better_weaponry", "mob_sword_damage");

    @WrapMethod(method = "doHurtTarget")
    private boolean betterweaponry$hitLikeSword(Entity target, Operation<Boolean> original) {
        Mob mob = (Mob) (Object) this;
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage == null || !(mob.getMainHandItem().getItem() instanceof WeaponItem weapon)) return original.call(target);
        WeaponMaterial material = weapon.getMaterial();
        damage.addTransientModifier(new AttributeModifier(SWORD_DAMAGE_ID, material.swordDamage() - weapon.damage(), AttributeModifier.Operation.ADD_VALUE));
        try {
            return original.call(target);
        } finally {
            damage.removeModifier(SWORD_DAMAGE_ID);
        }
    }
}
