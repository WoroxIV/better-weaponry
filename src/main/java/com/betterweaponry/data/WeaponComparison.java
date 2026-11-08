package com.betterweaponry.data;

import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Tableau comparatif des armes, pour un matériau donné. */
public final class WeaponComparison {
    private static final double PERCENT_UNSET = 0.0D;

    private WeaponComparison() {
    }

    /** Une colonne du tableau. Le nom se trie à part, par ordre de déclaration des armes. */
    public enum Column {
        NAME("name", row -> row.type().ordinal()),
        DAMAGE("damage", Row::damage),
        SPEED("speed", Row::speed),
        DPS("dps", Row::dps),
        REACH("reach", Row::reach),
        PENETRATION("penetration", Row::penetration);

        private final String id;
        private final ToDoubleFunction<Row> value;

        Column(String id, ToDoubleFunction<Row> value) {
            this.id = id;
            this.value = value;
        }

        public String id() {
            return id;
        }
    }

    public record Row(WeaponType type, double damage, double speed, double dps, double reach, double penetration) {
    }

    /** Lignes triées. Les armes à égalité gardent l'ordre de déclaration, pour un affichage stable. */
    public static List<Row> rows(Map<WeaponType, WeaponDefinition> weapons, WeaponMaterial material, Column column, boolean descending) {
        List<Row> rows = new ArrayList<>();
        weapons.forEach((type, definition) -> rows.add(new Row(type, definition.damageFor(material), definition.attackSpeed(),
                definition.dps(material), definition.attackReach(), Math.max(PERCENT_UNSET, definition.armorPenetration()))));
        Comparator<Row> order = Comparator.comparingDouble(column.value);
        rows.sort((descending ? order.reversed() : order).thenComparingInt(row -> row.type().ordinal()));
        return rows;
    }
}
