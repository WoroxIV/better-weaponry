package com.betterweaponry.registry;

import com.betterweaponry.BetterWeaponry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Statistiques du joueur, affichées dans l'écran vanilla (onglet Général). */
public final class ModStats {
    public static final DeferredRegister<ResourceLocation> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, BetterWeaponry.MODID);

    public static final Supplier<ResourceLocation> PERFECT_PARRIES = register("perfect_parries");
    public static final Supplier<ResourceLocation> SHURIKENS_THROWN = register("shurikens_thrown");
    /** Enregistrée en dixièmes de point de vie, comme les dégâts vanilla. */
    public static final Supplier<ResourceLocation> HEALTH_STOLEN = register("health_stolen");

    private ModStats() {
    }

    private static Supplier<ResourceLocation> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, name);
        return STATS.register(name, () -> id);
    }

    /** À appeler une fois les registres figés : donne à chaque statistique sa façon de s'afficher. */
    public static void registerFormats() {
        Stats.CUSTOM.get(PERFECT_PARRIES.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(SHURIKENS_THROWN.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(HEALTH_STOLEN.get(), StatFormatter.DIVIDE_BY_TEN);
    }
}
