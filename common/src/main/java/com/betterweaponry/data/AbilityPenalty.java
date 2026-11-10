package com.betterweaponry.data;

import com.betterweaponry.compat.BwCompat;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Le mode "équilibrage global" ({@code balance_abilities}) : les armes à capacités font un peu moins de dégâts.
 * Les capacités sont lues dans les fichiers Combat Reforged livrés avec le mod, sans jamais appeler ce mod ;
 * sans lui, ces capacités n'existent pas et il n'y a rien à retirer.
 */
public final class AbilityPenalty {
    // Réduction des dégâts, en fraction des dégâts.
    private static final double PARRY_PENALTY = 0.04D;
    private static final double DASH_PENALTY = 0.04D;
    private static final double AREA_COMBO_PENALTY = 0.03D;
    private static final double ON_HIT_EFFECTS_PENALTY = 0.02D;
    private static final double SHIELD_DISABLING_PENALTY = 0.02D;
    /** 1 % de dégâts en moins par tranche de 10 % de pénétration d'armure. */
    private static final double PENALTY_PER_PENETRATION_PERCENT = 0.001D;
    /** Les capacités comptent moins que les dégâts bruts : jamais plus de 12 % en moins. */
    private static final double MAX_PENALTY = 0.12D;

    private static final Map<WeaponType, Double> CACHE = new EnumMap<>(WeaponType.class);

    private AbilityPenalty() {
    }

    /** Part des dégâts retirée pour ce type d'arme, 0 si le mode est coupé ou si Combat Reforged est absent. */
    public static double of(WeaponType type) {
        if (!BwCompat.combatReforged() || !WeaponData.compat().balanceAbilities()) return 0.0D;
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(type, AbilityPenalty::read);
        }
    }

    private static double read(WeaponType type) {
        Optional<String> file = ModFiles.read("data", "better_weaponry", "combat_reforged", "weapons", type.id() + ".json");
        if (file.isEmpty()) return 0.0D;
        JsonElement root = JsonUtil.parseLenient(file.get());
        if (!root.isJsonObject()) return 0.0D;
        JsonObject weapon = root.getAsJsonObject();
        double penalty = 0.0D;
        String ability = weapon.has("ability") ? weapon.get("ability").getAsString() : "none";
        if ("parry".equals(ability)) penalty += PARRY_PENALTY;
        if ("dash".equals(ability)) penalty += DASH_PENALTY;
        if (hasAreaCombo(weapon)) penalty += AREA_COMBO_PENALTY;
        if (weapon.has("on_hit_effects") && !weapon.getAsJsonArray("on_hit_effects").isEmpty()) penalty += ON_HIT_EFFECTS_PENALTY;
        if (weapon.has("disables_shield") && weapon.get("disables_shield").getAsBoolean()) penalty += SHIELD_DISABLING_PENALTY;
        if (weapon.has("armor_penetration")) penalty += weapon.get("armor_penetration").getAsDouble() * PENALTY_PER_PENETRATION_PERCENT;
        return Math.min(penalty, MAX_PENALTY);
    }

    private static boolean hasAreaCombo(JsonObject weapon) {
        if (!weapon.has("combo")) return false;
        JsonObject combo = weapon.getAsJsonObject("combo");
        if (!combo.has("enabled") || !combo.get("enabled").getAsBoolean() || !combo.has("steps")) return false;
        JsonArray steps = combo.getAsJsonArray("steps");
        for (JsonElement step : steps) {
            if (step.isJsonObject() && step.getAsJsonObject().has("radius") && step.getAsJsonObject().get("radius").getAsDouble() > 0.0D) return true;
        }
        return false;
    }
}
