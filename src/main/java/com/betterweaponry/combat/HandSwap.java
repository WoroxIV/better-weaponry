package com.betterweaponry.combat;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Échange temporairement les deux mains d'un joueur, objets ET bonus d'attributs.
 * <p>
 * Pendant une attaque de la main secondaire, Minecraft croit alors que l'arme secondaire
 * est en main principale : dégâts, vitesse, enchantements, durabilité, recul, coup balayé...
 * tout vient de la bonne arme, sans réécrire Player.attack().
 * <p>
 * L'échange se fait directement dans l'inventaire (sans événement d'équipement ni synchro réseau)
 * et doit toujours être annulé avec {@link #restore()} avant la fin du tick.
 */
public final class HandSwap {
    private final Player player;
    /** Bonus retirés (à remettre) et ajoutés (à retirer) pendant l'échange. */
    private final List<AppliedModifier> removed = new ArrayList<>();
    private final List<AppliedModifier> added = new ArrayList<>();

    private HandSwap(Player player) {
        this.player = player;
    }

    /** Échange les mains et renvoie l'objet qui permet de tout remettre en place. */
    public static HandSwap begin(Player player) {
        HandSwap swap = new HandSwap(player);
        Inventory inventory = player.getInventory();
        ItemStack main = inventory.getSelected();
        ItemStack off = inventory.offhand.get(0);

        swapModifiers(main, off, player::getAttribute, swap);

        inventory.items.set(inventory.selected, off);
        inventory.offhand.set(0, main);
        return swap;
    }

    /**
     * Remet les mains et les attributs comme avant.
     * On relit les emplacements au lieu de réutiliser les objets d'origine : si l'arme s'est cassée
     * pendant l'attaque, l'emplacement est vide et doit le rester (dans la bonne main).
     */
    public void restore() {
        for (AppliedModifier change : added) {
            change.instance().removeModifier(change.modifier().id());
        }
        for (AppliedModifier change : removed) {
            if (!change.instance().hasModifier(change.modifier().id())) {
                change.instance().addTransientModifier(change.modifier());
            }
        }

        Inventory inventory = player.getInventory();
        ItemStack nowInMain = inventory.getSelected();
        inventory.items.set(inventory.selected, inventory.offhand.get(0));
        inventory.offhand.set(0, nowInMain);
    }

    /**
     * Vitesse d'attaque qu'aurait le joueur avec les mains échangées. Sert à afficher la recharge de l'arme secondaire.
     */
    public static double attackSpeedWithHandsSwapped(Player player) {
        return attributeWithHandsSwapped(player, Attributes.ATTACK_SPEED);
    }

    /**
     * Valeur qu'aurait un attribut avec les mains échangées (ex : dégâts de l'arme secondaire), sans rien modifier :
     * le calcul se fait sur une copie de l'attribut.
     */
    public static double attributeWithHandsSwapped(Player player, Holder<Attribute> attribute) {
        AttributeInstance real = player.getAttribute(attribute);
        if (real == null) return attribute.value().getDefaultValue();

        AttributeInstance copy = new AttributeInstance(attribute, ignored -> {});
        copy.replaceFrom(real);
        swapModifiers(player.getMainHandItem(), player.getOffhandItem(),
                candidate -> candidate.equals(attribute) ? copy : null, null);
        return copy.getValue();
    }

    /**
     * Retire les bonus actuels (arme principale côté MAINHAND, secondaire côté OFFHAND)
     * puis applique les bonus inversés (secondaire côté MAINHAND, principale côté OFFHAND).
     *
     * @param journal si non null, note chaque changement pour pouvoir l'annuler
     */
    private static void swapModifiers(ItemStack main, ItemStack off,
                                      Function<Holder<Attribute>, AttributeInstance> lookup,
                                      @Nullable HandSwap journal) {
        removeModifiers(main, EquipmentSlot.MAINHAND, lookup, journal);
        removeModifiers(off, EquipmentSlot.OFFHAND, lookup, journal);
        addModifiers(off, EquipmentSlot.MAINHAND, lookup, journal);
        addModifiers(main, EquipmentSlot.OFFHAND, lookup, journal);
    }

    private static void removeModifiers(ItemStack stack, EquipmentSlot slot,
                                        Function<Holder<Attribute>, AttributeInstance> lookup,
                                        @Nullable HandSwap journal) {
        stack.forEachModifier(slot, (attribute, modifier) -> {
            AttributeInstance instance = lookup.apply(attribute);
            if (instance == null) return;
            // On garde le modificateur réellement appliqué (c'est lui qu'on remettra)
            AttributeModifier applied = instance.getModifier(modifier.id());
            if (applied != null) {
                instance.removeModifier(applied.id());
                if (journal != null) journal.removed.add(new AppliedModifier(instance, applied));
            }
        });
    }

    private static void addModifiers(ItemStack stack, EquipmentSlot slot,
                                     Function<Holder<Attribute>, AttributeInstance> lookup,
                                     @Nullable HandSwap journal) {
        stack.forEachModifier(slot, (attribute, modifier) -> {
            AttributeInstance instance = lookup.apply(attribute);
            // Déjà présent (même ID appliqué par l'autre main) : on n'y touche pas pour ne pas planter.
            if (instance == null || instance.hasModifier(modifier.id())) return;
            instance.addTransientModifier(modifier);
            if (journal != null) journal.added.add(new AppliedModifier(instance, modifier));
        });
    }

    private record AppliedModifier(AttributeInstance instance, AttributeModifier modifier) {
    }
}
