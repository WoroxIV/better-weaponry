package com.betterweaponry.combat;

import com.betterweaponry.data.DataProblem;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le profil d'arme d'un objet : les armes du mod, et les objets "adoptés" par un fichier d'arme via son champ
 * {@code items} (ex : la parade du katana sur l'épée en diamant vanilla).
 * <p>
 * Un objet adopté reprend les comportements de l'arme (capacité, combo, effets, balayage, pénétration, deux mains,
 * dual wield) mais garde ses propres dégâts, sa vitesse et sa portée. Une arme du mod n'est jamais adoptée.
 * Tout le code qui se demande "quelle arme est-ce ?" passe par ici.
 */
public final class WeaponProfiles {
    private static final String TAG_PREFIX = "#";

    /** Dernière table construite, réutilisée tant que les données des armes n'ont pas changé. */
    private static volatile Bindings cache;

    private WeaponProfiles() {
    }

    /**
     * @param adopted vrai si l'objet n'est pas une arme du mod
     */
    public record Profile(WeaponType type, WeaponDefinition definition, boolean adopted) {
    }

    /** Un id précis passe avant un tag ; les tags sont testés à la demande, car ils changent à chaque /reload. */
    private record Bindings(int generation, Map<ResourceLocation, WeaponType> exact, List<TagBinding> tags) {
    }

    private record TagBinding(TagKey<Item> tag, WeaponType type) {
    }

    public static Optional<Profile> of(ItemStack stack) {
        if (stack.getItem() instanceof WeaponItem weapon) return Optional.of(new Profile(weapon.getType(), weapon.definition(), false));
        if (stack.isEmpty()) return Optional.empty();

        WeaponData.Versioned data = WeaponData.versioned();
        Bindings bindings = bindings(data);
        if (bindings.exact().isEmpty() && bindings.tags().isEmpty()) return Optional.empty();

        WeaponType type = bindings.exact().get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (type == null) {
            for (TagBinding binding : bindings.tags()) {
                if (stack.is(binding.tag())) {
                    type = binding.type();
                    break;
                }
            }
        }
        return type == null ? Optional.empty() : Optional.of(new Profile(type, data.snapshot().weapon(type), true));
    }

    public static Optional<WeaponDefinition> definition(ItemStack stack) {
        return of(stack).map(Profile::definition);
    }

    public static Optional<WeaponType> type(ItemStack stack) {
        return of(stack).map(Profile::type);
    }

    /** Vrai pour un objet qui n'est pas une arme du mod, mais qu'un fichier d'arme a adopté. */
    public static boolean isAdopted(ItemStack stack) {
        return of(stack).map(Profile::adopted).orElse(false);
    }

    private static Bindings bindings(WeaponData.Versioned data) {
        Bindings current = cache;
        if (current != null && current.generation() == data.generation()) return current;

        Map<ResourceLocation, WeaponType> exact = new HashMap<>();
        List<TagBinding> tags = new ArrayList<>();
        for (WeaponType type : WeaponType.values()) {
            for (String entry : data.snapshot().weapon(type).items()) {
                if (entry.startsWith(TAG_PREFIX)) {
                    tags.add(new TagBinding(TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.substring(1))), type));
                } else {
                    exact.putIfAbsent(ResourceLocation.parse(entry), type);
                }
            }
        }
        current = new Bindings(data.generation(), Map.copyOf(exact), List.copyOf(tags));
        cache = current;
        return current;
    }

    /** Signale un objet (ou un tag) revendiqué par deux fichiers d'armes : seul le premier compte. */
    public static void reportConflicts(Map<WeaponType, WeaponDefinition> weapons, List<DataProblem> problems) {
        Map<String, WeaponType> owners = new HashMap<>();
        for (WeaponType type : WeaponType.values()) {
            WeaponDefinition definition = weapons.get(type);
            if (definition == null) continue;
            for (String entry : definition.items()) {
                WeaponType owner = owners.putIfAbsent(entry, type);
                if (owner != null && owner != type) {
                    problems.add(new DataProblem(DataProblem.Kind.ITEM_CONFLICT, WeaponDataLoader.weaponPath(type),
                            WeaponDataLoader.weaponPath(owner), entry));
                }
            }
        }
    }
}
