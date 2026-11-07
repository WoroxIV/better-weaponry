package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    private ModSounds() {
    }
}
