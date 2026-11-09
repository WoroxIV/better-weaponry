package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Attributs ajoutés par le mod.
 * <p>
 * La pénétration d'armure est un attribut (et pas une simple valeur lue sur l'arme) : elle est ainsi échangée
 * automatiquement avec les autres stats pendant les coups de la main secondaire, et utilisable par les mobs.
 */
public final class ModAttributes {
    /** Part de l'armure de la cible ignorée par les coups au corps à corps : 0 = aucune, 1 = toute l'armure. */
    public static final Registered<Attribute, Attribute> ARMOR_PENETRATION = Platform.get().register(Registries.ATTRIBUTE, "armor_penetration",
            () -> Platform.get().createArmorPenetrationAttribute("attribute.name." + BetterWeaponry.MODID + ".armor_penetration"));

    /** Identifiant du modificateur posé par les armes : affiché en vert comme les dégâts (stat "de base"). */
    public static final ResourceLocation BASE_ARMOR_PENETRATION_ID =
            ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "base_armor_penetration");

    /** Pénétration d'armure en pourcentage d'Apothic Attributes : la même stat que la nôtre, calculée par ce mod s'il est installé. */
    private static final ResourceLocation APOTHIC_ARMOR_SHRED = ResourceLocation.fromNamespaceAndPath("apothic_attributes", "armor_shred");

    private ModAttributes() {
    }

    /** Force la déclaration de l'attribut. */
    public static void init() {
    }

    /** Attribut qui porte la pénétration d'armure des armes : celui d'Apothic Attributes s'il existe, sinon le nôtre. */
    public static Holder<Attribute> armorPenetration() {
        return BuiltInRegistries.ATTRIBUTE.getHolder(APOTHIC_ARMOR_SHRED).<Holder<Attribute>>map(holder -> holder).orElse(ARMOR_PENETRATION.holder());
    }
}
