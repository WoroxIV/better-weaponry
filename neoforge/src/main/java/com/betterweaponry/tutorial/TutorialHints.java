package com.betterweaponry.tutorial;

import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Quel tutoriel proposer au joueur qui obtient une arme. Aucune dépendance au client : vérifié par les tests automatiques. */
public final class TutorialHints {
    private static final double NORMAL_REACH = 3.0D;

    private TutorialHints() {
    }

    /** Tutoriels liés à cette arme, du plus important au moins important. Seuls les systèmes actifs comptent. */
    public static List<String> relatedScenes(WeaponDefinition weapon, RulesDefinition rules) {
        List<String> scenes = new ArrayList<>();
        if (rules.twoHandedActive() && weapon.twoHanded()) {
            scenes.add("two_handed");
        } else if (rules.dualWieldActive() && weapon.dualWielded()) {
            scenes.add("dual_wield");
        }
        if (rules.parryActive() && weapon.ability() == WeaponAbility.PARRY) scenes.add("parry");
        if (rules.dashActive() && weapon.ability() == WeaponAbility.DASH) scenes.add("dash");
        if (rules.combosActive() && weapon.combo().enabled()) scenes.add("combos");
        if (rules.armorPenetrationActive() && weapon.armorPenetration() > 0.0D) scenes.add("armor_penetration");
        if (rules.reachActive() && weapon.attackReach() != NORMAL_REACH) scenes.add("reach");
        return scenes;
    }

    /** Le premier tutoriel lié à cette arme que le joueur ne connaît pas encore. */
    public static Optional<String> suggestion(WeaponDefinition weapon, RulesDefinition rules, Predicate<String> alreadyKnown) {
        return relatedScenes(weapon, rules).stream().filter(alreadyKnown.negate()).findFirst();
    }
}
