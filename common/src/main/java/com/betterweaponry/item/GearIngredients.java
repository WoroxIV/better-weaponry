package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.registry.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Les recettes (du jeu ou d'autres mods) qui demandent un arc, une arbalète ou un bouclier vanilla acceptent aussi les
 * versions du mod, quel que soit le matériau : ces objets vanilla ne se fabriquent plus, leurs recettes seraient
 * sinon impossibles. Appelé par IngredientMixin pour chaque ingrédient créé, très tôt au démarrage : cette classe ne
 * doit donc rien charger d'autre que les tags.
 */
public final class GearIngredients {

    private GearIngredients() {
    }

    /** Ajoute le tag du mod à côté de chaque arc, arbalète ou bouclier vanilla de cet ingrédient. */
    public static Stream<? extends Ingredient.Value> withModVersions(Stream<? extends Ingredient.Value> values) {
        List<Ingredient.Value> all = values.collect(Collectors.toCollection(ArrayList::new));
        List<Ingredient.Value> additions = new ArrayList<>();
        for (Ingredient.Value value : all) {
            if (value instanceof Ingredient.ItemValue(ItemStack stack)) {
                // Reçu d'un serveur : l'ingrédient contient déjà la liste complète, objet par objet.
                if (isFromThisMod(stack)) return all.stream();
                TagKey<Item> tag = modVersions(stack);
                if (tag != null) additions.add(new Ingredient.TagValue(tag));
            }
        }
        all.addAll(additions);
        return all.stream();
    }

    /** Même chose pour un ingrédient donné sous forme de tableau. */
    public static Ingredient.Value[] withModVersions(Ingredient.Value[] values) {
        return values.length == 0 ? values : withModVersions(Arrays.stream(values)).toArray(Ingredient.Value[]::new);
    }

    private static boolean isFromThisMod(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(BetterWeaponry.MODID);
    }

    @Nullable
    private static TagKey<Item> modVersions(ItemStack stack) {
        if (stack.is(Items.BOW)) return ModTags.BOWS;
        if (stack.is(Items.CROSSBOW)) return ModTags.CROSSBOWS;
        if (stack.is(Items.SHIELD)) return ModTags.SHIELDS;
        return null;
    }
}
