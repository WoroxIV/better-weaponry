package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ParryMaterial;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sons du mod. Le fichier joué et son sous-titre sont définis dans assets/better_weaponry/sounds.json :
 * un pack de ressources peut donc les remplacer.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BetterWeaponry.MODID);

    /** Coup arrêté par une parade (bruit métallique). */
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY = SOUNDS.register("item.weapon.parry", SoundEvent::createVariableRangeEvent);

    /** Dash (bruit de souffle). */
    public static final DeferredHolder<SoundEvent, SoundEvent> DASH = SOUNDS.register("item.weapon.dash", SoundEvent::createVariableRangeEvent);

    /** Le prochain coup est le coup spécial du combo (entendu seulement par le joueur qui attaque). */
    public static final DeferredHolder<SoundEvent, SoundEvent> COMBO_READY = SOUNDS.register("combo.ready", SoundEvent::createVariableRangeEvent);

    /** Parade parfaite : un son par famille de matériau. */
    private static final Map<ParryMaterial, DeferredHolder<SoundEvent, SoundEvent>> PERFECT_PARRY = new EnumMap<>(ParryMaterial.class);

    static {
        for (ParryMaterial material : ParryMaterial.values()) {
            PERFECT_PARRY.put(material, SOUNDS.register("item.weapon.perfect_parry." + material.id(), SoundEvent::createVariableRangeEvent));
        }
    }

    private ModSounds() {
    }

    public static SoundEvent perfectParry(ParryMaterial material) {
        return PERFECT_PARRY.get(material).get();
    }
}
