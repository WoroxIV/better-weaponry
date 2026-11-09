package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Attributs ajoutés par le mod.
 * <p>
 * La pénétration d'armure est un attribut (et pas une simple valeur lue sur l'arme) : elle est ainsi échangée
 * automatiquement avec les autres stats pendant les coups de la main secondaire, et utilisable par les mobs.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID)
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, BetterWeaponry.MODID);

    /** Part de l'armure de la cible ignorée par les coups au corps à corps : 0 = aucune, 1 = toute l'armure. */
    public static final DeferredHolder<Attribute, Attribute> ARMOR_PENETRATION =
            ATTRIBUTES.register("armor_penetration", ArmorPenetrationAttribute::new);

    /** Identifiant du modificateur posé par les armes : affiché en vert comme les dégâts (stat "de base"). */
    public static final ResourceLocation BASE_ARMOR_PENETRATION_ID =
            ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "base_armor_penetration");

    /** Pénétration d'armure en pourcentage d'Apothic Attributes : la même stat que la nôtre, calculée par ce mod s'il est installé. */
    private static final ResourceLocation APOTHIC_ARMOR_SHRED = ResourceLocation.fromNamespaceAndPath("apothic_attributes", "armor_shred");

    private ModAttributes() {
    }

    /** Attribut qui porte la pénétration d'armure des armes : celui d'Apothic Attributes s'il existe, sinon le nôtre. */
    public static Holder<Attribute> armorPenetration() {
        return BuiltInRegistries.ATTRIBUTE.getHolder(APOTHIC_ARMOR_SHRED).<Holder<Attribute>>map(holder -> holder).orElse(ARMOR_PENETRATION);
    }

    /** Toutes les créatures reçoivent l'attribut (valeur 0) : joueurs, mais aussi mobs qui tiendraient une arme. */
    @SubscribeEvent
    public static void addToEntities(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (!event.has(type, ARMOR_PENETRATION)) event.add(type, ARMOR_PENETRATION);
        }
    }

    private static final class ArmorPenetrationAttribute extends PercentageAttribute {
        ArmorPenetrationAttribute() {
            super("attribute.name." + BetterWeaponry.MODID + ".armor_penetration", 0.0D, 0.0D, 1.0D);
        }

        @Override
        public ResourceLocation getBaseId() {
            return BASE_ARMOR_PENETRATION_ID;
        }

        /** Ligne verte en pourcentage, au format vanilla de chaque langue ("30% Armor Penetration", "Penetrazione dell'armatura: 30%"). */
        @Override
        public MutableComponent toBaseComponent(double value, double entityBase, boolean merged, TooltipFlag flag) {
            return Component.translatable("attribute.modifier.equals.1", FORMAT.format(value * scaleFactor),
                    Component.translatable(getDescriptionId()));
        }
    }
}
