package com.betterweaponry.item;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.combat.DashAbility;
import com.betterweaponry.combat.ModEnchantments;
import com.betterweaponry.combat.ParrySystem;
import com.betterweaponry.combat.WeaponProfiles;
import com.betterweaponry.compat.CombatCompat;
import com.betterweaponry.data.OnHitEffect;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

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

    /** Vrai si l'objet a le profil d'une arme à deux mains, et que cette règle est active. */
    public static boolean isTwoHanded(ItemStack stack) {
        return WeaponData.rules().twoHandedActive() && WeaponProfiles.definition(stack).map(WeaponDefinition::twoHanded).orElse(false);
    }

    /**
     * Le jeu n'applique les bonus d'une arme qu'au moment où on la prend en main : après un changement de réglages,
     * on remplace ceux de l'arme déjà tenue. Certains bonus peuvent avoir disparu (ex : portée coupée dans rules.json),
     * d'où le retrait par identifiant avant d'ajouter les nouveaux.
     */
    public static void refreshEquippedModifiers(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        if (WeaponProfiles.of(stack).isEmpty()) return;
        if (stack.getItem() instanceof WeaponItem weapon) removeModifier(entity, Attributes.ENTITY_INTERACTION_RANGE, weapon.reachModifierId);
        removeModifier(entity, ModAttributes.armorPenetration(), ModAttributes.BASE_ARMOR_PENETRATION_ID);
        removeModifier(entity, Attributes.SWEEPING_DAMAGE_RATIO, BASE_SWEEPING_RATIO_ID);
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) return;
            instance.removeModifier(modifier.id());
            instance.addTransientModifier(modifier);
        });
    }

    private static void removeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    // --- Stats ---

    /** Remplace la méthode de NeoForge du même nom ; les autres chargeurs l'appellent à leur façon. */
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        // Cette méthode est appelée très souvent (tooltips, changements d'équipement...) :
        // on ne reconstruit l'objet que si les données des armes ont été remplacées depuis.
        WeaponData.Versioned data = WeaponData.versioned();
        CachedModifiers current = cache;
        if (current == null || current.generation() != data.generation()) {
            current = new CachedModifiers(data.generation(), buildModifiers(data.snapshot().weapon(type), data.snapshot().rules()));
            cache = current;
        }
        return current.modifiers();
    }

    // Les données stockent les valeurs finales affichées au joueur, on retire donc ses valeurs de base.
    private ItemAttributeModifiers buildModifiers(WeaponDefinition definition, RulesDefinition rules) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, addValue(BASE_ATTACK_DAMAGE_ID, definition.damageFor(material) - PLAYER_BASE_DAMAGE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, addValue(BASE_ATTACK_SPEED_ID, definition.attackSpeed() - PLAYER_BASE_SPEED),
                        EquipmentSlotGroup.MAINHAND);
        if (rules.reachActive()) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE, addValue(reachModifierId, definition.attackReach() - PLAYER_BASE_REACH),
                    EquipmentSlotGroup.MAINHAND);
        }
        // Absents à 0 : la ligne du tooltip n'apparaît que si la stat sert à quelque chose.
        if (rules.armorPenetrationActive() && definition.armorPenetration() > 0.0D) {
            builder.add(ModAttributes.armorPenetration(),
                    addValue(ModAttributes.BASE_ARMOR_PENETRATION_ID, definition.armorPenetration() / PERCENT), EquipmentSlotGroup.MAINHAND);
        }
        if (rules.sweepActive() && definition.sweep().enabled() && definition.sweep().damageRatio() > 0.0D) {
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
        applyOnHitEffects(stack, target, attacker);
        return result;
    }

    /** Donne à la cible les effets à l'impact de cet objet, s'il a un profil d'arme (côté serveur), chacun selon sa chance. */
    public static void applyOnHitEffects(ItemStack weapon, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide || !WeaponData.rules().onHitEffectsActive() || CombatCompat.isFakePlayer(attacker)) return;
        for (OnHitEffect effect : WeaponProfiles.definition(weapon).map(WeaponDefinition::onHitEffects).orElse(List.of())) {
            if (attacker.getRandom().nextDouble() * PERCENT < effect.chance()) {
                effect.createInstance().ifPresent(instance -> target.addEffect(instance, attacker));
            }
        }
    }

    public AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {
        if (!WeaponData.rules().sweepActive()) return target.getBoundingBox().inflate(1.0D, SWEEP_HEIGHT, 1.0D);
        return sweepZone(definition(), target);
    }

    /** Zone touchée par le coup balayé autour de la cible (vanilla : 1 bloc). La portée d'attaque la limite aussi. */
    public static AABB sweepZone(WeaponDefinition definition, Entity target) {
        double radius = definition.sweep().radius();
        return target.getBoundingBox().inflate(radius, SWEEP_HEIGHT, radius);
    }

    /** Comme une hache vanilla : un coup sur un bouclier levé le désactive. */
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return WeaponData.rules().shieldDisablingActive() && definition().disablesShield() && !CombatCompat.isFakePlayer(attacker);
    }

    // --- Capacité du clic droit ---

    /**
     * Clic droit : lance la capacité de l'arme si ses conditions sont remplies (parade : {@link ParrySystem#canParry},
     * dash : {@link DashAbility#canDash}). Sinon PASS : le jeu essaie alors l'objet de la main secondaire
     * (un bouclier se lève, une torche se pose...). Appelé côté client puis côté serveur.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ParrySystem.canParry(player, hand, stack)) {
            ParrySystem.beginParry(player, stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (DashAbility.canDash(player, hand, stack)) {
            DashAbility.dash(level, player, stack, definition().dash());
            // CONSUME et pas SUCCESS : pas de coup de bras, qui ferait alterner les mains en dual wield.
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    /** Pose "bloquer" pendant la parade : bras levé en troisième personne, comme en 1.8. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return ParrySystem.parryOf(stack).isPresent() ? UseAnim.BLOCK : super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ParrySystem.parryOf(stack).isPresent() ? ParrySystem.USE_DURATION : super.getUseDuration(stack, entity);
    }

    /**
     * Vue première personne : pas d'animation de "rééquipement" (l'arme descend puis remonte) quand seule
     * la durabilité change, par exemple à chaque coup paré. Changer d'objet ou d'emplacement l'anime toujours.
     */
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged || !newStack.is(oldStack.getItem())) return true;
        ItemStack sameWear = oldStack.copy();
        sameWear.setDamageValue(newStack.getDamageValue());
        return !ItemStack.isSameItemSameComponents(sameWear, newStack);
    }

    /** Reflective, Riposte et Élan suivent la capacité configurée de l'arme ; les autres enchantements, leur tag. */
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return ModEnchantments.supportedByAbility(definition(), enchantment).orElseGet(() -> enchantment.value().isSupportedItem(stack));
    }

    // Record immuable : l'item est partagé entre le thread client et le thread serveur en solo.
    private record CachedModifiers(int generation, ItemAttributeModifiers modifiers) {
    }
}
