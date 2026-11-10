package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.data.AbilityPenalty;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Item unique pour toutes les armes du mod.
 * Ses stats sont lues dans les données des armes ({@link WeaponData}) à chaque demande,
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

    /** Dégâts actuels de cette arme. */
    public double damage() {
        return definition().damageFor(material, AbilityPenalty.of(type));
    }

    /** Vrai si le joueur tient une arme du mod et que sa portée s'applique : elle porte alors à la distance affichée, même en créatif. */
    public static boolean hasExactReach(Player player) {
        return player.getMainHandItem().getItem() instanceof WeaponItem && BwCompat.reachActive();
    }

    /** Remplace la méthode de NeoForge du même nom ; les autres chargeurs l'appellent à leur façon. */
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        // Cette méthode est appelée très souvent (tooltips, changements d'équipement...) :
        // on ne reconstruit l'objet que si les données des armes ont été remplacées depuis.
        WeaponData.Versioned data = WeaponData.versioned();
        CachedModifiers current = cache;
        if (current == null || current.generation() != data.generation()) {
            current = new CachedModifiers(data.generation(), buildModifiers(data.snapshot().weapon(type)));
            cache = current;
        }
        return current.modifiers();
    }

    /** Version du jeu de base (sans NeoForge) : les mêmes bonus, recalculés quand les données changent. */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return getDefaultAttributeModifiers(ItemStack.EMPTY);
    }

    // Les données stockent les valeurs finales affichées au joueur, on retire donc ses valeurs de base.
    private ItemAttributeModifiers buildModifiers(WeaponDefinition definition) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, addValue(BASE_ATTACK_DAMAGE_ID, damage() - PLAYER_BASE_DAMAGE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, addValue(BASE_ATTACK_SPEED_ID, definition.attackSpeed() - PLAYER_BASE_SPEED), EquipmentSlotGroup.MAINHAND);
        if (BwCompat.reachActive()) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE, addValue(reachModifierId, definition.attackReach() - PLAYER_BASE_REACH), EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    private static AttributeModifier addValue(ResourceLocation id, double amount) {
        return new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * Vue première personne : pas d'animation de "rééquipement" (l'arme descend puis remonte) quand seule
     * la durabilité change. Changer d'objet ou d'emplacement l'anime toujours.
     */
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged || !newStack.is(oldStack.getItem())) return true;
        ItemStack sameWear = oldStack.copy();
        sameWear.setDamageValue(newStack.getDamageValue());
        return !ItemStack.isSameItemSameComponents(sameWear, newStack);
    }

    /** Méthode de Fabric : même règle que {@link #shouldCauseReequipAnimation}. */
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return shouldCauseReequipAnimation(oldStack, newStack, false);
    }

    // Record immuable : l'item est partagé entre le thread client et le thread serveur en solo.
    private record CachedModifiers(int generation, ItemAttributeModifiers modifiers) {
    }
}
