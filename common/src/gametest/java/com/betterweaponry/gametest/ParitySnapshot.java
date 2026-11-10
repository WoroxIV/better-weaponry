package com.betterweaponry.gametest;

import com.betterweaponry.combat.ModEnchantments;
import com.betterweaponry.data.GearDefinition;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.RulesDefinition;
import com.betterweaponry.data.WeaponData;
import com.betterweaponry.data.WeaponDefinition;
import com.betterweaponry.item.WeaponItem;
import com.betterweaponry.item.WeaponMaterials;
import com.betterweaponry.item.WeaponType;
import com.betterweaponry.platform.Platform;
import com.betterweaponry.registry.ModAttributes;
import com.betterweaponry.registry.ModItems;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Photographie du comportement de chaque arme, pour vérifier que la séparation en deux mods ne change rien :
 * stats réelles portées par l'objet et réglages de combat, écrits dans un fichier JSON trié.
 */
final class ParitySnapshot {
    private static final double PLAYER_BASE_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_SPEED = 4.0D;
    private static final double PLAYER_BASE_REACH = 3.0D;
    private static final double PERCENT = 100.0D;
    /** Champs qui décrivent l'objet lui-même : ils sont déjà représentés par les stats réelles de chaque arme. */
    private static final List<String> ITEM_FIELDS = List.of("format_version", "attack_speed", "attack_reach", "base_damage", "material_damage", "items");

    private ParitySnapshot() {
    }

    static Path write(RegistryAccess registries, String fileName) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject weapons = new JsonObject();
        for (Item item : ModItems.allWeapons()) {
            weapons.add(BuiltInRegistries.ITEM.getKey(item).toString(), describe((WeaponItem) item));
        }
        root.add("weapons", weapons);
        root.add("rules", encodeRules(WeaponData.rules()));
        root.add("gear", encodeGear(WeaponData.gear()));
        root.addProperty("unknown_enchantment", unknownEnchantmentOutcome(registries));

        Path file = Platform.get().gameDir().resolve(fileName);
        Files.writeString(file, JsonUtil.PRETTY.toJson(sorted(root)) + "\n", StandardCharsets.UTF_8);
        return file;
    }

    private static JsonObject describe(WeaponItem weapon) {
        ItemStack stack = new ItemStack(weapon);
        ItemAttributeModifiers modifiers = Platform.get().attributeModifiers(stack);
        JsonObject line = new JsonObject();
        line.addProperty("final_attack_damage", PLAYER_BASE_DAMAGE + sum(modifiers, Attributes.ATTACK_DAMAGE));
        line.addProperty("final_attack_speed", PLAYER_BASE_SPEED + sum(modifiers, Attributes.ATTACK_SPEED));
        line.addProperty("final_reach", PLAYER_BASE_REACH + sum(modifiers, Attributes.ENTITY_INTERACTION_RANGE));
        line.addProperty("final_armor_penetration", sum(modifiers, ModAttributes.armorPenetration()) * PERCENT);
        line.addProperty("final_sweep_ratio", sum(modifiers, Attributes.SWEEPING_DAMAGE_RATIO) * PERCENT);

        JsonObject definition = WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, weapon.definition()).getOrThrow().getAsJsonObject();
        ITEM_FIELDS.forEach(definition::remove);
        definition.entrySet().forEach(entry -> line.add(entry.getKey(), entry.getValue()));
        return line;
    }

    private static double sum(ItemAttributeModifiers modifiers, Holder<Attribute> attribute) {
        double total = 0.0D;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(attribute)) total += entry.modifier().amount();
        }
        return total;
    }

    private static JsonElement encodeRules(RulesDefinition rules) {
        return RulesDefinition.CODEC.encodeStart(JsonOps.INSTANCE, rules).getOrThrow();
    }

    private static JsonElement encodeGear(GearDefinition gear) {
        return GearDefinition.CODEC.encodeStart(JsonOps.INSTANCE, gear).getOrThrow();
    }

    /** Ce que devient, au chargement d'une sauvegarde, une arme dont l'un des enchantements n'existe plus. */
    private static String unknownEnchantmentOutcome(RegistryAccess registries) {
        Holder<Enchantment> riposte = registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(ModEnchantments.RIPOSTE);
        Holder<Enchantment> unbreaking = registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING);
        ItemStack stack = new ItemStack(ModItems.get(WeaponType.KATANA, WeaponMaterials.IRON).get());
        stack.enchant(riposte, 2);
        stack.enchant(unbreaking, 1);

        CompoundTag saved = (CompoundTag) stack.save(registries);
        CompoundTag altered;
        try {
            altered = TagParser.parseTag(saved.toString().replace("better_weaponry:riposte", "better_weaponry:missing_enchantment"));
        } catch (CommandSyntaxException e) {
            return "test_error: " + e.getMessage();
        }
        Optional<ItemStack> parsed = ItemStack.parse(registries, altered);
        if (parsed.isEmpty()) return "item_lost";
        return "item_kept, enchantments_left=" + EnchantmentHelper.getEnchantmentsForCrafting(parsed.get()).size();
    }

    /** Copie avec les clés triées, pour pouvoir comparer deux fichiers ligne à ligne. */
    private static JsonElement sorted(JsonElement element) {
        if (element.isJsonObject()) {
            Map<String, JsonElement> entries = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(entry -> entries.put(entry.getKey(), sorted(entry.getValue())));
            JsonObject result = new JsonObject();
            entries.forEach(result::add);
            return result;
        }
        if (element.isJsonArray()) {
            com.google.gson.JsonArray result = new com.google.gson.JsonArray();
            element.getAsJsonArray().forEach(child -> result.add(sorted(child)));
            return result;
        }
        return element;
    }
}
