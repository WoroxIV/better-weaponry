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

    private ModSounds() {
    }
}
