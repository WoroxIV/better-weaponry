package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Item unique pour toutes les armes du mod.
 * Ses stats (dégâts, vitesse, portée) sont lues dans les données des armes ({@link WeaponData}) à chaque demande,
 * donc un {@code /reload} ou une synchronisation du serveur s'applique sans redémarrer.
 */
public class WeaponItem extends SwordItem {
    // Valeurs de base du joueur : les modificateurs s'y ajoutent.
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_SPEED = 4.0D;
    private static final double PLAYER_BASE_REACH = 3.0D;

    private final WeaponType type;
    private final WeaponMaterial material;
    private final ResourceLocation reachModifierId;

    /** Derniers modificateurs construits, réutilisés tant que les données n'ont pas changé. */
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

    /** Stats actuelles du type de cette arme. */
    public WeaponDefinition definition() {
        return WeaponData.weapon(type);
    }

    /** Vrai si l'objet est une arme du mod configurée comme arme à deux mains. */
    public static boolean isTwoHanded(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem weapon && weapon.definition().twoHanded();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        // Cette méthode est appelée très souvent (tooltips, changements d'équipement...) :
        // on ne reconstruit l'objet que si les données des armes ont été remplacées depuis.
        WeaponData.Versioned data = WeaponData.versioned();
        CachedModifiers current = cache;
        if (current == null || current.generation() != data.generation()) {
            WeaponDefinition definition = data.snapshot().weapon(type);
            current = new CachedModifiers(data.generation(), buildModifiers(
                    definition.damageFor(material), definition.attackSpeed(), definition.attackReach()));
            cache = current;
        }
        return current.modifiers();
    }

    // Les données stockent les valeurs finales affichées au joueur, on retire donc ses valeurs de base.
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
    private record CachedModifiers(int generation, ItemAttributeModifiers modifiers) {
    }
}
