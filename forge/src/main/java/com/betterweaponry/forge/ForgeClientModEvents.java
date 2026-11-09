package com.betterweaponry.forge;

import com.betterweaponry.client.WeaponIconModel;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Événements du bus du mod, côté client. */
public final class ForgeClientModEvents {
    private ForgeClientModEvents() {
    }

    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        WeaponIconModel.wrapModels(event);
    }
}
