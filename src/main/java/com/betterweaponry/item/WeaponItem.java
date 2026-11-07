package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.WeaponAbility;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Item unique pour toutes les armes du mod.
 * Ses stats sont lues dans les données des armes ({@link WeaponData}) à chaque demande,
 * donc un {@code /reload} ou une synchronisation du serveur s'applique sans redémarrer.
 */
public class WeaponItem extends SwordItem {
    // Valeurs de base du joueur : les modificateurs s'y ajoutent.
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_SPEED = 4.0D;
    private static final double PLAYER_BASE_REACH = 3.0D;
    private static final double PERCENT = 100.0D;
    /** Hauteur de la zone de balayage autour de la cible (vanilla : 0.25 bloc). */
    private static final double SWEEP_HEIGHT = 0.25D;
    /** Un seul identifiant pour le balayage de toutes les armes, comme les dégâts et la vitesse vanilla. */
    public static final ResourceLocation BASE_SWEEPING_RATIO_ID =
            ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, "base_sweeping_damage_ratio");

    private final WeaponType type;
    private final WeaponMaterial material;
    private final ResourceLocation reachModifierId;

    /** Derniers modificateurs construits, réutilisés tant que les données n'ont pas changé. */
    private volatile CachedModifiers cache;

    public WeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
        super(material.tier(), properties);
        this.type = type;
        this.material = material;
        this.reachModifierId = ResourceLocation.fromNamespaceAndPath(BetterWeaponry.MODID, material.itemName(type) + "_reach");
    }

    public WeaponType getType() {
        return type;
    }

    public WeaponMaterial getMaterial() {
        return material;
    }

    /** Stats actuelles du type de cette arme. */
    public WeaponDefinition definition() {
        return WeaponData.weapon(type);
    }

    /** Vrai si l'objet est une arme du mod configurée comme arme à deux mains. */
    public static boolean isTwoHanded(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem weapon && weapon.definition().twoHanded();
    }

    // --- Stats ---

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        // Cette méthode est appelée très souvent (tooltips, changements d'équipement...) :
        // on ne reconstruit l'objet que si les données des armes ont été remplacées depuis.
        WeaponData.Versioned data = WeaponData.versioned();
        CachedModifiers current = cache;
        if (current == null || current.generation() != data.generation()) {
            current = new CachedModifiers(data.generation(), buildModifiers(data.snapshot().weapon(type)));
            cache = current;
        }
        return current.modifiers();
    }

    // Les données stockent les valeurs finales affichées au joueur, on retire donc ses valeurs de base.
    private ItemAttributeModifiers buildModifiers(WeaponDefinition definition) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, addValue(BASE_ATTACK_DAMAGE_ID, definition.damageFor(material) - PLAYER_BASE_DAMAGE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, addValue(BASE_ATTACK_SPEED_ID, definition.attackSpeed() - PLAYER_BASE_SPEED),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, addValue(reachModifierId, definition.attackReach() - PLAYER_BASE_REACH),
                        EquipmentSlotGroup.MAINHAND);
        // Absents à 0 : la ligne du tooltip n'apparaît que si la stat sert à quelque chose.
        if (definition.armorPenetration() > 0.0D) {
            builder.add(ModAttributes.ARMOR_PENETRATION,
                    addValue(ModAttributes.BASE_ARMOR_PENETRATION_ID, definition.armorPenetration() / PERCENT), EquipmentSlotGroup.MAINHAND);
        }
        if (definition.sweep().enabled() && definition.sweep().damageRatio() > 0.0D) {
            builder.add(Attributes.SWEEPING_DAMAGE_RATIO,
                    addValue(BASE_SWEEPING_RATIO_ID, definition.sweep().damageRatio() / PERCENT), EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    private static AttributeModifier addValue(ResourceLocation id, double amount) {
        return new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    // --- Combat ---

    /**
     * Appelé quand un coup de cette arme touche une créature (côté serveur).
     * Pendant un coup de la main secondaire, c'est bien l'arme secondaire qui est appelée.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide) {
            for (OnHitEffect effect : definition().onHitEffects()) {
                if (attacker.getRandom().nextDouble() * PERCENT < effect.chance()) {
                    effect.createInstance().ifPresent(instance -> target.addEffect(instance, attacker));
                }
            }
        }
        return result;
    }

    /** Zone touchée par le coup balayé autour de la cible (vanilla : 1 bloc). La portée d'attaque la limite aussi. */
    @Override
    public AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {
        double radius = definition().sweep().radius();
        return target.getBoundingBox().inflate(radius, SWEEP_HEIGHT, radius);
    }

    /** Comme une hache vanilla : un coup sur un bouclier levé le désactive. */
    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return definition().disablesShield();
    }

    // --- Capacité du clic droit ---

    /**
     * Clic droit : lance la parade si l'arme pare et que les conditions sont remplies ({@link ParrySystem#canParry}).
     * Sinon PASS : le jeu essaie alors l'objet de la main secondaire (un bouclier se lève, une torche se pose...).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ParrySystem.canParry(player, hand, stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    /** Pose "bloquer" pendant la parade : bras levé en troisième personne, comme en 1.8. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return definition().ability() == WeaponAbility.PARRY ? UseAnim.BLOCK : super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return definition().ability() == WeaponAbility.PARRY ? ParrySystem.USE_DURATION : super.getUseDuration(stack, entity);
    }

    /**
     * Vue première personne : pas d'animation de "rééquipement" (l'arme descend puis remonte) quand seule
     * la durabilité change, par exemple à chaque coup paré. Changer d'objet ou d'emplacement l'anime toujours.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || shouldCauseBlockBreakReset(oldStack, newStack);
    }

    // Record immuable : l'item est partagé entre le thread client et le thread serveur en solo.
    private record CachedModifiers(int generation, ItemAttributeModifiers modifiers) {
    }
}
