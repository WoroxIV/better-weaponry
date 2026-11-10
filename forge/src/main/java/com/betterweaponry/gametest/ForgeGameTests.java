package com.betterweaponry.gametest;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

/** Tests propres à Forge : la reprise des configs des versions 1.x, qui n'existaient pas sur Fabric. */
@GameTestHolder(BetterWeaponry.MODID)
@PrefixGameTestTemplate(false)
public final class ForgeGameTests {
    private static final String ARENA = "empty";

    private ForgeGameTests() {
    }

    /** Réglages 1.x propres à un monde : convertis dans un datapack du monde, et l'ancien fichier est mis de côté. */
    @GameTest(template = ARENA)
    public static void legacyWorldConfigIsConverted(GameTestHelper helper) {
        try {
            Path worldRoot = Files.createTempDirectory("better_weaponry_migration");
            Path legacy = worldRoot.resolve("serverconfig").resolve(LegacyConfigMigration.LEGACY_FILE_NAME);
            Files.createDirectories(legacy.getParent());
            Files.writeString(legacy, "[weapon_stats.glaive]\nattack_speed = 2.5\n\n[other]\ndual_wield_speed_multiplier = 3.0\n");

            helper.assertTrue(LegacyConfigMigration.migrateWorld(worldRoot), "the world config should be converted");
            helper.assertFalse(Files.exists(legacy), "the old file should be moved away");
            helper.assertTrue(Files.exists(legacy.resolveSibling(LegacyConfigMigration.LEGACY_FILE_NAME + ".old")), "the old file should be kept as .old");

            Path data = worldRoot.resolve("datapacks/better_weaponry_migrated_1x/data/better_weaponry/better_weaponry/weapons");
            JsonObject glaive = JsonUtil.parseLenient(Files.readString(data.resolve("glaive.json"))).getAsJsonObject();
            helper.assertTrue(glaive.get("attack_speed").getAsDouble() == 2.5D, "the changed value should be converted: " + glaive);

            helper.assertFalse(LegacyConfigMigration.migrateWorld(worldRoot), "a converted world should not be converted twice");
            try (var files = Files.walk(worldRoot)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
            }
        } catch (java.io.IOException e) {
            helper.fail("could not prepare the world folder: " + e.getMessage());
        }
        helper.succeed();
    }
}
