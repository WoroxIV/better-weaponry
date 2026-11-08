package com.betterweaponry.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Motif de craft d'un type d'arme, décliné automatiquement pour chaque matériau.
 * <ul>
 *   <li>{@code M} : le matériau de l'arme (planches, lingot d'or, ou le tag d'un matériau moddé)</li>
 *   <li>{@code S} : un bâton (tag c:rods/wooden)</li>
 *   <li>espace : case vide</li>
 * </ul>
 * Exemple : {@code WeaponRecipe.of(" M ", " M ", " S ")}.
 * Pour d'autres ingrédients fixes : {@code WeaponRecipe.of("MLM", " S ", " S ").with('L', Tags.Items.LEATHERS)}.
 * <p>
 * Les matériaux obtenus à la table de forge (netherite) n'utilisent pas ce motif :
 * ils améliorent la version du matériau de base.
 */
public final class WeaponRecipe {
    public static final char MATERIAL = 'M';
    public static final char STICK = 'S';

    private final List<String> pattern;
    private final Map<Character, TagKey<Item>> extraIngredients = new LinkedHashMap<>();
    private final Map<Character, Supplier<Item>> extraItems = new LinkedHashMap<>();

    private WeaponRecipe(List<String> pattern) {
        if (pattern.isEmpty() || pattern.size() > 3) {
            throw new IllegalArgumentException("Un motif de craft fait de 1 à 3 lignes : " + pattern);
        }
        int width = pattern.get(0).length();
        for (String row : pattern) {
            if (row.length() != width || width > 3) {
                throw new IllegalArgumentException("Toutes les lignes doivent avoir la même largeur (max 3) : " + pattern);
            }
        }
        this.pattern = List.copyOf(pattern);
    }

    public static WeaponRecipe of(String... rows) {
        return new WeaponRecipe(List.of(rows));
    }

    /** Ajoute un ingrédient fixe (identique pour tous les matériaux) sous le symbole donné. */
    public WeaponRecipe with(char symbol, TagKey<Item> ingredient) {
        if (symbol == MATERIAL || symbol == STICK || symbol == ' ') {
            throw new IllegalArgumentException("Symbole réservé : '" + symbol + "'");
        }
        extraIngredients.put(symbol, ingredient);
        return this;
    }

    /** Comme {@link #with}, pour un objet précis qui n'a pas de tag (ex : le crochet). */
    public WeaponRecipe withItem(char symbol, Supplier<Item> item) {
        extraItems.put(symbol, item);
        return this;
    }

    public Map<Character, Supplier<Item>> extraItems() {
        return extraItems;
    }

    public List<String> pattern() {
        return pattern;
    }

    public Map<Character, TagKey<Item>> extraIngredients() {
        return extraIngredients;
    }

    /** Vrai si le symbole apparaît dans le motif (un ingrédient défini mais absent fait échouer le datagen). */
    public boolean uses(char symbol) {
        return pattern.stream().anyMatch(row -> row.indexOf(symbol) >= 0);
    }
}
