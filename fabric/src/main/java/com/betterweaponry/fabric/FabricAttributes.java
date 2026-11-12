package com.betterweaponry.fabric;

import com.betterweaponry.platform.Platform;
import com.betterweaponry.platform.Registered;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** La portée d'attaque : la 1.20.1 n'a pas cet attribut, Forge le fournit mais pas Fabric. */
public final class FabricAttributes {
    public static final double DEFAULT_ENTITY_REACH = 3.0D;
    private static final double MAX_ENTITY_REACH = 1024.0D;

    public static final Registered<Attribute, Attribute> ENTITY_REACH = Platform.get().register(Registries.ATTRIBUTE, "entity_reach",
            () -> new RangedAttribute("attribute.better_weaponry.attack_reach", DEFAULT_ENTITY_REACH, 0.0D, MAX_ENTITY_REACH).setSyncable(true));

    private FabricAttributes() {
    }

    /** Force la déclaration de l'attribut, avant les armes qui s'en servent. */
    static void init() {
    }
}
