package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Attributs ajoutés par le mod.
 * <p>
 * La pénétration d'armure est un attribut (et pas une simple valeur lue sur l'arme) : elle est ainsi échangée
 * automatiquement avec les autres stats pendant les coups de la main secondaire, et utilisable par les mobs.
 */
public final class ModAttributes {
    /** Part de l'armure de la cible ignorée par les coups au corps à corps : 0 = aucune, 1 = toute l'armure. */
    public static final Registered<Attribute, Attribute> ARMOR_PENETRATION = Platform.get().register(Registries.ATTRIBUTE, "armor_penetration",
            () -> new RangedAttribute("attribute.name." + BetterWeaponry.MODID + ".armor_penetration", 0.0D, 0.0D, 1.0D).setSyncable(true));

    /** Part des dégâts d'un coup balayé : la 1.20.1 n'a pas cet attribut, seulement l'enchantement Affilage. */
    public static final Registered<Attribute, Attribute> SWEEPING_DAMAGE_RATIO = Platform.get().register(Registries.ATTRIBUTE, "sweeping_damage_ratio",
            () -> new RangedAttribute("attribute.name.player.sweeping_damage_ratio", 0.0D, 0.0D, 1.0D).setSyncable(true));

    /** Identifiant du modificateur posé par les armes : affiché en vert comme les dégâts (stat "de base"). */
    public static final UUID BASE_ARMOR_PENETRATION_ID = uuid("base_armor_penetration");

    /** Pénétration d'armure en pourcentage d'Apothic Attributes : la même stat que la nôtre, calculée par ce mod s'il est installé. */
    private static final ResourceLocation APOTHIC_ARMOR_SHRED = new ResourceLocation("attributeslib", "armor_shred");

    private ModAttributes() {
    }

    /** Force la déclaration de l'attribut. */
    public static void init() {
    }

    /** Attribut qui porte la pénétration d'armure des armes : celui d'Apothic Attributes s'il existe, sinon le nôtre. */
    public static Attribute armorPenetration() {
        return BuiltInRegistries.ATTRIBUTE.getOptional(APOTHIC_ARMOR_SHRED).orElseGet(ARMOR_PENETRATION);
    }

    /** Identifiant fixe d'un bonus d'attribut, déduit de son nom : toujours le même d'une partie à l'autre. */
    public static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes((BetterWeaponry.MODID + ":" + name).getBytes(StandardCharsets.UTF_8));
    }
}
