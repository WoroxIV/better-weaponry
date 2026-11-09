package com.betterweaponry.client.tutorial;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.client.ModKeyMappings;
import com.betterweaponry.config.ClientConfig;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.tutorial.TutorialHints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Propose un tutoriel la première fois que le joueur obtient un type d'arme, quelle qu'en soit la façon (ramassage,
 * craft, coffre...). Une seule proposition par type d'arme et par tutoriel, et jamais deux à la suite.
 */
@EventBusSubscriber(modid = BetterWeaponry.MODID, value = Dist.CLIENT)
public final class TutorialHintTracker {
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int TICKS_BETWEEN_HINTS = 600;

    private static int ticks;
    private static int nextHintTick;
    @Nullable private static String suggestedScene;

    private TutorialHintTracker() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ModKeyMappings.OPEN_TUTORIAL.consumeClick()) openTutorial(minecraft);

        ticks++;
        LocalPlayer player = minecraft.player;
        if (player == null || ticks % SCAN_INTERVAL_TICKS != 0 || ticks < nextHintTick || !ClientConfig.get(ClientConfig.TUTORIAL_HINTS)) return;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof WeaponItem weapon && suggestFor(minecraft, weapon, stack)) return;
        }
    }

    /** @return vrai si une proposition vient d'être affichée */
    private static boolean suggestFor(Minecraft minecraft, WeaponItem weapon, ItemStack stack) {
        String type = weapon.getType().id();
        if (ClientConfig.isHintShown(type)) return false;
        ClientConfig.markHintShown(type);
        Optional<String> scene = TutorialHints.suggestion(weapon.definition(), WeaponData.rules(),
                id -> ClientConfig.isTutorialSeen(id) || ClientConfig.isHintShown(id));
        if (scene.isEmpty()) return false;
        ClientConfig.markHintShown(scene.get());
        suggestedScene = scene.get();
        nextHintTick = ticks + TICKS_BETWEEN_HINTS;
        minecraft.getToasts().addToast(new TutorialHintToast(stack.copy(), scene.get()));
        return true;
    }

    private static void openTutorial(Minecraft minecraft) {
        minecraft.setScreen(suggestedScene != null ? new TutorialScreen(null, suggestedScene) : new TutorialListScreen(null, null));
        suggestedScene = null;
    }
}
