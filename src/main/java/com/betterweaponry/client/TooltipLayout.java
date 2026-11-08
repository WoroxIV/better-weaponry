package com.betterweaponry.client;

import com.betterweaponry.config.TooltipCategory;
import com.betterweaponry.config.TooltipPlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Range les lignes du tooltip d'une arme selon le placement et la couleur de chaque catégorie.
 * Aucune dépendance au client : ce rangement est vérifié par les tests automatiques.
 */
public final class TooltipLayout {
    /** Début des clés vanilla des titres de stats, ex : "item.modifiers.mainhand". */
    private static final String HEADER_KEY = "item.modifiers.";
    /** Toutes les lignes de stats (vanilla, du mod, d'autres mods) utilisent une clé qui commence ainsi. */
    private static final String ATTRIBUTE_KEY = "attribute.";
    private static final String INDENT = " ";

    private TooltipLayout() {
    }

    /** Placement et couleur de chaque catégorie. */
    public record Settings(Function<TooltipCategory, TooltipPlacement> placement, Function<TooltipCategory, ChatFormatting> color) {
    }

    /**
     * Retire du tooltip les lignes qui contiennent cette clé de traduction et les renvoie.
     *
     * @param keyPart morceau de clé, ex : "attack_damage"
     */
    public static List<Component> takeLines(List<Component> tooltip, String keyPart) {
        List<Component> taken = new ArrayList<>();
        tooltip.removeIf(line -> {
            boolean match = containsTranslationKey(line, keyPart);
            if (match) taken.add(line);
            return match;
        });
        return taken;
    }

    /**
     * Place les lignes de chaque catégorie : au-dessus du titre des stats, juste sous ce titre, ou après les stats
     * pour les détails. Le titre et la ligne vide qui le précède disparaissent s'il ne reste aucune stat dessous.
     *
     * @param lines          lignes par catégorie, avec l'indentation d'un espace des stats
     * @param detailsVisible la touche "Détails de l'arme" est maintenue
     * @param hint           ligne qui invite à maintenir cette touche, montrée à la place des détails
     */
    public static void arrange(List<Component> tooltip, Map<TooltipCategory, List<Component>> lines, Settings settings,
                               boolean detailsVisible, Component hint) {
        List<Component> above = new ArrayList<>();
        List<Component> stats = new ArrayList<>();
        List<Component> details = new ArrayList<>();
        for (TooltipCategory category : TooltipCategory.values()) {
            ChatFormatting color = settings.color().apply(category);
            for (Component line : lines.getOrDefault(category, List.of())) {
                switch (settings.placement().apply(category)) {
                    case ABOVE_STATS -> above.add(colored(withoutIndent(line), color));
                    case STATS -> stats.add(colored(line, color));
                    case DETAILS -> details.add(colored(line, color));
                    case HIDDEN -> { }
                }
            }
        }
        if (!details.isEmpty() && !detailsVisible) details = List.of(hint);

        int header = headerIndex(tooltip);
        if (header < 0) {
            // Pas de titre (stats cachées par un autre mod) : tout va sous le nom de l'objet.
            int position = Math.min(1, tooltip.size());
            List<Component> all = new ArrayList<>(above);
            all.addAll(stats);
            all.addAll(details);
            tooltip.addAll(position, all);
            return;
        }

        tooltip.addAll(header + 1, stats);
        // Après nos lignes (elles ne sont pas toutes des stats : effets, capacité...) et celles d'autres mods.
        tooltip.addAll(endOfStats(tooltip, header + stats.size()), details);
        boolean emptyHeader = stats.isEmpty() && details.isEmpty() && endOfStats(tooltip, header) == header + 1;
        if (emptyHeader) tooltip.remove(header);

        // La ligne vide qui précède le titre sépare les stats du reste : les lignes "au-dessus" se placent avant elle.
        int blank = header - 1;
        boolean hasBlank = blank > 0 && tooltip.get(blank).getString().isEmpty();
        if (emptyHeader && hasBlank) tooltip.remove(blank);
        tooltip.addAll(hasBlank ? blank : header, above);
    }

    /** Index du titre "En main principale :", ou -1. */
    public static int headerIndex(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (containsTranslationKey(tooltip.get(i), HEADER_KEY)) return i;
        }
        return -1;
    }

    /** Index juste après la dernière stat qui suit cette ligne. */
    private static int endOfStats(List<Component> tooltip, int line) {
        int end = line + 1;
        while (end < tooltip.size() && containsTranslationKey(tooltip.get(end), ATTRIBUTE_KEY)) end++;
        return end;
    }

    /** Copie de la ligne, dans cette couleur (y compris ses morceaux qui avaient leur propre couleur). */
    public static Component colored(Component line, ChatFormatting color) {
        MutableComponent copy = MutableComponent.create(line.getContents()).setStyle(line.getStyle().withColor(color));
        for (Component sibling : line.getSiblings()) copy.append(colored(sibling, color));
        return copy;
    }

    /** Sans l'espace qui indente les stats. */
    private static Component withoutIndent(Component line) {
        if (!(line.getContents() instanceof PlainTextContents.LiteralContents literal) || !literal.text().equals(INDENT)) return line;
        MutableComponent copy = Component.empty().setStyle(line.getStyle());
        line.getSiblings().forEach(copy::append);
        return copy;
    }

    /** Cherche une clé de traduction dans le composant, ses arguments et ses enfants (récursif). */
    public static boolean containsTranslationKey(Component component, String keyPart) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (translatable.getKey().contains(keyPart)) return true;
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component child && containsTranslationKey(child, keyPart)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (containsTranslationKey(sibling, keyPart)) return true;
        }
        return false;
    }
}
