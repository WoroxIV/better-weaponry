package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Item unique pour toutes les armes du mod.
 * Ses stats (dégâts, vitesse, portée) sont lues dans la config serveur à chaque demande,
 * donc une modification de la config s'applique sans redémarrer.
 */
public class WeaponItem extends SwordItem {
    // Valeurs de base du joueur : les modificateurs s'y ajoutent.
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_SPEED = 4.0D;
    private static final double PLAYER_BASE_REACH = 3.0D;

    private final WeaponType type;
    private final WeaponMaterial material;
    private final ResourceLocation reachModifierId;

    /** Derniers modificateurs construits, réutilisés tant que la config n'a pas changé. */
    private volatile CachedModifiers cache;

    public WeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
        super(material.tier(), properties);
        this.type = type;
        this.material = material;
        this.reachModifierId = ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, material.itemName(type) + "_reach");
    }

    public WeaponType getType() {
        return type;
    }

    public WeaponMaterial getMaterial() {
        return material;
    }

    /** Vrai si l'objet est une arme du mod configurée comme arme à deux mains. */
    public static boolean isTwoHanded(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem weapon && ServerConfig.isTwoHanded(weapon.type);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        double damage = ServerConfig.attackDamage(type, material);
        double speed = ServerConfig.attackSpeed(type);
        double reach = ServerConfig.attackReach(type);

        // Cette méthode est appelée très souvent (tooltips, changements d'équipement...) :
        // on ne reconstruit l'objet que si une valeur de config a changé.
        CachedModifiers current = cache;
        if (current == null || !current.matches(damage, speed, reach)) {
            current = new CachedModifiers(damage, speed, reach, buildModifiers(damage, speed, reach));
            cache = current;
        }
        return current.modifiers();
    }

    // La config stocke les valeurs finales affichées au joueur, on retire donc ses valeurs de base.
    private ItemAttributeModifiers buildModifiers(double damage, double speed, double reach) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - PLAYER_BASE_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, speed - PLAYER_BASE_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(reachModifierId, reach - PLAYER_BASE_REACH, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // Record immuable : l'item est partagé entre le thread client et le thread serveur en solo.
    private record CachedModifiers(double damage, double speed, double reach, ItemAttributeModifiers modifiers) {
        boolean matches(double damage, double speed, double reach) {
            return this.damage == damage && this.speed == speed && this.reach == reach;
        }
    }
}
