package com.betterweaponry.config;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponStats;
import com.betterweaponry.item.WeaponType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Config serveur (un fichier par monde : serverconfig/better_weaponry-server.toml),
 * synchronisée automatiquement vers les clients connectés.
 * <p>
 * Structure générée :
 * <pre>
 * [weapon_stats.&lt;type&gt;]                          attack_speed, attack_reach, two_handed
 * [weapon_stats.&lt;type&gt;.&lt;matériau&gt;_&lt;type&gt;]      attack_damage
 * [other]                                         options d'affichage et de dual wield
 * </pre>
 * Seuls les dégâts dépendent du matériau. Les valeurs par défaut viennent de {@link WeaponType}
 * (stats du type) et de {@link WeaponMaterial#damageMultiplier()}.
 * Les matériaux moddés n'apparaissent que si leur mod est installé.
 */
public final class ServerConfig {
    /** Les dégâts par défaut sont arrondis au demi-point pour des tooltips lisibles. */
    private static final double DAMAGE_ROUNDING = 0.5D;

    /** Réglages d'un type d'arme, indexés par type. */
    private static final Map<WeaponType, WeaponConfig> WEAPONS = new EnumMap<>(WeaponType.class);

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_ATTACK_REACH_TOOLTIP;
    public static final ModConfigSpec.BooleanValue SHOW_TWO_HANDED_TOOLTIP;
    public static final ModConfigSpec.DoubleValue DUAL_WIELD_SPEED_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("weapon_stats");
        for (WeaponType type : WeaponType.values()) {
            WeaponStats defaults = type.defaultStats();
            builder.push(type.id());

            // Dégâts : un sous-bloc par matériau
            Map<WeaponMaterial, ModConfigSpec.DoubleValue> damages = new IdentityHashMap<>();
            for (WeaponMaterial material : WeaponMaterials.active()) {
                String itemName = material.itemName(type);
                // L'écran de config réutilise le nom de l'item : pas de traduction en plus pour un nouveau matériau.
                builder.translation("item." + BetterWeaponry.MODID + "." + itemName).push(itemName);
                damages.put(material, builder.defineInRange("attack_damage",
                        defaultDamage(defaults, material), 0.0D, 1024.0D));
                builder.pop();
            }

            // Vitesse, portée et deux mains : communs à tous les matériaux du type
            ModConfigSpec.DoubleValue speed = builder.defineInRange("attack_speed", defaults.attackSpeed(), 0.0D, 1024.0D);
            ModConfigSpec.DoubleValue reach = builder.defineInRange("attack_reach", defaults.reach(), 0.0D, 64.0D);
            ModConfigSpec.BooleanValue twoHanded = builder.define("two_handed", defaults.twoHanded());
            WEAPONS.put(type, new WeaponConfig(speed, reach, twoHanded, damages));

            builder.pop();
        }
        builder.pop();

        builder.push("other");
        SHOW_ATTACK_REACH_TOOLTIP = builder
                .comment("Show the 'Attack Reach' stat on weapon tooltips.")
                .define("show_attack_reach_tooltip", true);
        SHOW_TWO_HANDED_TOOLTIP = builder
                .comment("Show 'Two Handed' on tooltips of weapons configured as two-handed.")
                .define("show_two_handed_tooltip", true);
        // 1.5 : le dual wield vaut alors le bonus d'environ 1.5× des armes à deux mains.
        DUAL_WIELD_SPEED_MULTIPLIER = builder
                .comment("Attack speed multiplier while holding a weapon in each hand (1.5 = 50% faster).")
                .defineInRange("dual_wield_speed_multiplier", 1.5D, 1.0D, 5.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    /** Dégâts de base du type × multiplicateur du matériau, arrondis au demi-point. */
    private static double defaultDamage(WeaponStats stats, WeaponMaterial material) {
        double raw = stats.baseDamage() * material.damageMultiplier();
        return Math.round(raw / DAMAGE_ROUNDING) * DAMAGE_ROUNDING;
    }

    // --- Accès aux valeurs ---

    public static double attackDamage(WeaponType type, WeaponMaterial material) {
        ModConfigSpec.DoubleValue damage = WEAPONS.get(type).damages().get(material);
        if (damage == null) {
            throw new IllegalStateException("Matériau inactif ou non déclaré dans WeaponMaterials : " + material);
        }
        return get(damage);
    }

    public static double attackSpeed(WeaponType type) {
        return get(WEAPONS.get(type).speed());
    }

    public static double attackReach(WeaponType type) {
        return get(WEAPONS.get(type).reach());
    }

    public static boolean isTwoHanded(WeaponType type) {
        return get(WEAPONS.get(type).twoHanded());
    }

    /**
     * Lit une valeur sans planter si la config n'est pas encore chargée
     * (ex : client hors d'un monde, où la config serveur n'existe pas) : on renvoie alors la valeur par défaut.
     */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private record WeaponConfig(ModConfigSpec.DoubleValue speed,
                                ModConfigSpec.DoubleValue reach,
                                ModConfigSpec.BooleanValue twoHanded,
                                Map<WeaponMaterial, ModConfigSpec.DoubleValue> damages) {
    }
}
