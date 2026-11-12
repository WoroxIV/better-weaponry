package com.betterweaponry.gametest;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.item.GearType;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterial;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModCreativeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

import java.util.Objects;

/** Tests de l'API des addons (matériaux déclarés par d'autres mods). */
//@LOADER_TEST_ANNOTATIONS@
public final class ApiGameTests {

    private static final String ARENA = "@ARENA@";

    /** Fabric crée une instance de la classe de tests. */
    public ApiGameTests() {
    }

    /** Un matériau d'addon garde son namespace partout : nom des items, clé dans les données, tag d'ingrédient généré. */
    @GameTest(template = ARENA)
    public static void addonMaterialUsesItsNamespace(GameTestHelper helper) {
        WeaponMaterial ruby = WeaponMaterial.builder("test_addon", "ruby", Tiers.IRON)
                .craftedFrom("test_addon:ruby", "#c:gems/ruby").gearDurability(GearType.SHIELD, 77).build();
        helper.assertTrue(Objects.equals(ruby.gearDurability(GearType.SHIELD).getAsInt(), 77), "imposed shield durability" + ": " + ruby.gearDurability(GearType.SHIELD).getAsInt());
        helper.assertFalse(ruby.gearDurability(GearType.BOW).isPresent(), "no imposed bow durability");
        helper.assertTrue(ruby.isAddon(), "a material of another namespace is an addon material");
        helper.assertTrue(Objects.equals(ruby.key(), "test_addon:ruby"), "data key" + ": " + ruby.key());
        helper.assertTrue(Objects.equals(ruby.itemId(WeaponType.KATANA).toString(), "test_addon:ruby_katana"), "item id" + ": " + ruby.itemId(WeaponType.KATANA).toString());
        helper.assertTrue(Objects.equals(ruby.craftingIngredient().tag().location().toString(), "test_addon:weapon_materials/ruby"), "generated ingredient tag" + ": " + ruby.craftingIngredient().tag().location().toString());
        helper.assertTrue(ruby.hasIngredients(), "an addon material without required mod is always craftable");

        helper.assertFalse(WeaponMaterials.IRON.isAddon(), "iron is a built-in material");
        helper.assertTrue(Objects.equals(WeaponMaterials.IRON.key(), "iron"), "built-in materials keep their short key" + ": " + WeaponMaterials.IRON.key());
        helper.assertTrue(Objects.equals(WeaponMaterials.IRON.itemId(WeaponType.KATANA).toString(), BetterWeaponry.MODID + ":iron_katana"), "built-in item id" + ": " + WeaponMaterials.IRON.itemId(WeaponType.KATANA).toString());
        helper.succeed();
    }

    /** Les listes de matériaux contiennent ceux des addons installés, et l'onglet du mod ne montre que les siens. */
    @GameTest(template = ARENA)
    public static void addonMaterialsAreListedButNotInTheModTab(GameTestHelper helper) {
        int addonMaterials = Platform.get().materialProviders().stream().mapToInt(provider -> provider.materials().size()).sum();
        long listed = WeaponMaterials.all().stream().filter(WeaponMaterial::isAddon).count();
        helper.assertTrue(Objects.equals(listed, (long) addonMaterials), "addon materials in WeaponMaterials.all()" + ": " + listed);
        helper.assertTrue(WeaponMaterials.builtIn().stream().noneMatch(WeaponMaterial::isAddon), "builtIn() has no addon material");

        for (ItemStack stack : ModCreativeTabs.displayOrder()) {
            helper.assertTrue(Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace(), BetterWeaponry.MODID), "namespace of " + stack + ": " + BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
        }
        for (WeaponMaterial material : WeaponMaterials.active()) {
            WeaponItem katana = com.betterweaponry.registry.ModItems.get(WeaponType.KATANA, material).get();
            helper.assertTrue(Objects.equals(BuiltInRegistries.ITEM.getKey(katana), material.itemId(WeaponType.KATANA)), "registered id of " + material + ": " + BuiltInRegistries.ITEM.getKey(katana));
        }
        helper.succeed();
    }
}
