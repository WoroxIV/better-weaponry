package com.betterweaponry.item;

/**
 * Statistiques par défaut d'un type d'arme (modifiables ensuite dans la config).
 * <p>
 * Seuls les dégâts dépendent du matériau : {@code baseDamage} est la valeur en bois/or, multipliée
 * ensuite par le facteur du matériau (fer ×1.5, diamant ×1.75, netherite ×2, comme les épées vanilla).
 *
 * @param baseDamage  dégâts affichés en bois (épée vanilla : 4)
 * @param attackSpeed attaques par seconde (épée vanilla : 1.6) ; au-delà de 2.0 les coups sont perdus
 *                    à cause de l'invulnérabilité de 0.5 s après chaque coup reçu
 * @param reach       portée d'attaque en blocs (joueur vanilla : 3)
 * @param twoHanded   arme à deux mains : main secondaire bloquée, en échange d'environ 1.5× plus de puissance
 */
public record WeaponStats(double baseDamage, double attackSpeed, double reach, boolean twoHanded) {

    public static WeaponStats oneHanded(double baseDamage, double attackSpeed, double reach) {
        return new WeaponStats(baseDamage, attackSpeed, reach, false);
    }

    public static WeaponStats twoHanded(double baseDamage, double attackSpeed, double reach) {
        return new WeaponStats(baseDamage, attackSpeed, reach, true);
    }
}
