package com.betterweaponry.gametest;

import com.betterweaponry.BetterWeaponry;
import com.betterweaponry.config.LegacyConfigMigration;
import com.betterweaponry.data.JsonUtil;
import com.betterweaponry.data.WeaponDataLoader;
import com.betterweaponry.item.WeaponType;
import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

/** Tests propres à NeoForge : la reprise des configs des versions 1.x, qui n'existaient pas sur Fabric. */
@GameTestHolder(BetterWeaponry.MODID)
@PrefixGameTestTemplate(false)
public final class NeoForgeGameTests {
    private static final String ARENA = "empty";

    private NeoForgeGameTests() {
    }

    /** Réglages 1.x propres à un monde : convertis dans le datapack de l'éditeur, et l'ancien fichier est mis de côté. */
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

            Path data = worldRoot.resolve("datapacks/better_weaponry_migrated_1x/data").resolve(BetterWeaponry.MODID).resolve(WeaponDataLoader.DATA_DIRECTORY);
            JsonObject glaive = JsonUtil.parseLenient(Files.readString(data.resolve(WeaponDataLoader.weaponPath(WeaponType.GLAIVE)))).getAsJsonObject();
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
