package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Tags du mod, modifiables par datapack comme n'importe quel tag. */
public final class ModTags {
    /**
     * Objets d'autres mods utilisables en dual wield sans être déclarés dans une arme. Vide par défaut :
     * pour les armes du mod, c'est l'option "dual_wielded" de leur fichier qui compte.
     */
    public static final TagKey<Item> DUAL_WIELDABLE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "dual_wieldable"));

    /**
     * Objets qui comptent comme des armes de mêlée (ligne de portée du tooltip, dash avec une arme dans l'autre main).
     * Par défaut : épées, haches, trident et le tag commun des armes de mêlée ({@code c:tools/melee_weapon}).
     */
    public static final TagKey<Item> MELEE_WEAPONS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "melee_weapons"));

    private ModTags() {
    }
}
