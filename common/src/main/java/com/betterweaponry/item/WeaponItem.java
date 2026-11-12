package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.data.AbilityPenalty;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.platform.Platform;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
    private static final String MODIFIER_NAME = "Weapon modifier";

    private final WeaponType type;
    private final WeaponMaterial material;
    private final UUID reachModifierId;

    /** Derniers modificateurs construits, réutilisés tant que les données n'ont pas changé. */
    private volatile CachedModifiers cache;

    public WeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
        // Dégâts et vitesse viennent des données des armes : voir getDefaultAttributeModifiers.
        super(material.tier(), 0, 0.0F, properties);
        this.type = type;
        this.material = material;
        // Même identifiant qu'avant pour les matériaux du mod ; celui de l'addon pour les siens.
        this.reachModifierId = UUID.nameUUIDFromBytes((material.namespace() + ":" + material.itemName(type) + "_reach").getBytes(StandardCharsets.UTF_8));
    }

    /** Identifiant stable d'un modificateur d'attribut du mod. */
    public static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes((BetterWeaponry.MODID + ":" + name).getBytes(StandardCharsets.UTF_8));
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

    /**
     * Le jeu n'applique les bonus d'une arme qu'au moment où on la prend en main : après un changement de réglages,
     * on remplace ceux de l'arme déjà tenue. La portée peut avoir disparu, d'où le retrait par identifiant.
     */
    public static void refreshEquippedModifiers(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponItem weapon)) return;
        AttributeInstance reach = entity.getAttribute(Platform.get().entityReachAttribute());
        if (reach != null) reach.removeModifier(weapon.reachModifierId);
        stack.getAttributeModifiers(EquipmentSlot.MAINHAND).forEach((attribute, modifier) -> {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) return;
            instance.removeModifier(modifier.getId());
            instance.addTransientModifier(modifier);
        });
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();
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

    /** Les mobs comparent les épées avec cette valeur pour choisir laquelle garder. */
    @Override
    public float getDamage() {
        return (float) (damage() - PLAYER_BASE_DAMAGE);
    }

    // Les données stockent les valeurs finales affichées au joueur, on retire donc ses valeurs de base.
    private Multimap<Attribute, AttributeModifier> buildModifiers(WeaponDefinition definition) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, addValue(BASE_ATTACK_DAMAGE_UUID, definition.damageFor(material, AbilityPenalty.of(type)) - PLAYER_BASE_DAMAGE));
        builder.put(Attributes.ATTACK_SPEED, addValue(BASE_ATTACK_SPEED_UUID, definition.attackSpeed() - PLAYER_BASE_SPEED));
        if (BwCompat.reachActive()) {
            builder.put(Platform.get().entityReachAttribute(), addValue(reachModifierId, definition.attackReach() - PLAYER_BASE_REACH));
        }
        return builder.build();
    }

    private static AttributeModifier addValue(UUID id, double amount) {
        return new AttributeModifier(id, MODIFIER_NAME, amount, AttributeModifier.Operation.ADDITION);
    }

    /**
     * Vue première personne : pas d'animation de "rééquipement" (l'arme descend puis remonte) quand seule
     * la durabilité change. Changer d'objet ou d'emplacement l'anime toujours.
     */
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged || !newStack.is(oldStack.getItem())) return true;
        ItemStack sameWear = oldStack.copy();
        sameWear.setDamageValue(newStack.getDamageValue());
        return !ItemStack.isSameItemSameTags(sameWear, newStack);
    }

    /** Méthode de Fabric : même règle que {@link #shouldCauseReequipAnimation}. */
    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return shouldCauseReequipAnimation(oldStack, newStack, false);
    }

    // Record immuable : l'item est partagé entre le thread client et le thread serveur en solo.
    private record CachedModifiers(int generation, Multimap<Attribute, AttributeModifier> modifiers) {
    }
}
