package com.betterweaponry.registry;

import net.minecraft.resources.ResourceLocation;
import com.betterweaponry.platform.Registered;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ParryMaterial;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sons du mod. Le fichier joué et son sous-titre sont définis dans assets/better_weaponry/sounds.json :
 * un pack de ressources peut donc les remplacer.
 */
public final class ModSounds {
    /** Coup arrêté par une parade (bruit métallique). */
    public static final Registered<SoundEvent, SoundEvent> PARRY = register("item.weapon.parry");

    /** Dash (bruit de souffle). */
    public static final Registered<SoundEvent, SoundEvent> DASH = register("item.weapon.dash");

    /** Le prochain coup est le coup spécial du combo (entendu seulement par le joueur qui attaque). */
    public static final Registered<SoundEvent, SoundEvent> COMBO_READY = register("combo.ready");

    /** Parade parfaite : un son par famille de matériau. */
    private static final Map<ParryMaterial, Registered<SoundEvent, SoundEvent>> PERFECT_PARRY = new EnumMap<>(ParryMaterial.class);

    static {
        for (ParryMaterial material : ParryMaterial.values()) {
            PERFECT_PARRY.put(material, register("item.weapon.perfect_parry." + material.id()));
        }
    }

    private ModSounds() {
    }

    /** Force la déclaration des sons. */
    public static void init() {
    }

    private static Registered<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, name);
        return Platform.get().register(Registries.SOUND_EVENT, name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static SoundEvent perfectParry(ParryMaterial material) {
        return PERFECT_PARRY.get(material).get();
    }
}
