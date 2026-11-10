package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Tags du mod, modifiables par datapack comme n'importe quel tag. */
public final class ModTags {
    /**
     * Objets d'autres mods utilisables en dual wield sans être déclarés dans une arme. Vide par défaut :
     * pour les armes du mod, c'est l'option "dual_wielded" de leur fichier qui compte.
     */
    public static final TagKey<Item> DUAL_WIELDABLE = TagKey.create(Registries.ITEM, new ResourceLocation(BetterWeaponry.MODID, "dual_wieldable"));

    /**
     * Objets qui comptent comme des armes de mêlée (ligne de portée du tooltip, dash avec une arme dans l'autre main).
     * Par défaut : épées, haches, trident et le tag commun des armes de mêlée ({@code c:tools/melee_weapon}).
     */
    public static final TagKey<Item> MELEE_WEAPONS = TagKey.create(Registries.ITEM, new ResourceLocation(BetterWeaponry.MODID, "melee_weapons"));

    /** Tous les boucliers du mod. */
    public static final TagKey<Item> SHIELDS = TagKey.create(Registries.ITEM, new ResourceLocation(BetterWeaponry.MODID, "shields"));

    /** Arcs du mod et arc vanilla (recette du distributeur). */
    public static final TagKey<Item> BOWS = TagKey.create(Registries.ITEM, new ResourceLocation(BetterWeaponry.MODID, "bows"));
    /** Arbalètes du mod et arbalète vanilla (succès Ol' Betsy). */
    public static final TagKey<Item> CROSSBOWS = TagKey.create(Registries.ITEM, new ResourceLocation(BetterWeaponry.MODID, "crossbows"));

    /** Tous les shurikens : reçoivent les enchantements des arcs et des arbalètes. */
    public static final TagKey<Item> SHURIKENS = TagKey.create(Registries.ITEM, new ResourceLocation(BetterWeaponry.MODID, "shurikens"));

    /** Faucilles, faux et katanas : les armes qui peuvent recevoir Saignement. */
    public static final TagKey<Item> ENCHANTABLE_BLEEDING = TagKey.create(Registries.ITEM, new ResourceLocation(BetterWeaponry.MODID, "enchantable/bleeding"));

    private ModTags() {
    }
}
