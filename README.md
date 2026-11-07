# Better Weaponry

A NeoForge mod for Minecraft **1.21.1** that adds 13 new weapon types in every vanilla material tier, plus two-handed weapon handling and dual wielding. Every weapon's damage, attack speed, reach and two-handed behaviour can be changed in the server config.

- **Mod ID:** `better_weaponry`
- **Version:** 1.1.4
- **Author:** WoroxIV
- **Loader:** NeoForge 21.1.x (built against 21.1.220)
- **Java:** 21
- **Optional:** [Better Combat](https://modrinth.com/mod/better-combat) compatibility

---

## Features

### Weapons

Each weapon type comes in six vanilla tiers: **wooden, stone, iron, golden, diamond, netherite**. That makes 78 items. Up to 44 more materials come from other mods (see [Mod compatibility](#mod-compatibility)).

| Weapon     | Item ID pattern            |
|------------|----------------------------|
| Broadsword | `<tier>_broadsword`        |
| Spear      | `<tier>_spear`             |
| Dagger     | `<tier>_dagger`            |
| Scythe     | `<tier>_scythe`            |
| Rapier     | `<tier>_rapier`            |
| Sickle     | `<tier>_sickle`            |
| Katana     | `<tier>_katana`            |
| Battleaxe  | `<tier>_battleaxe`         |
| Glaive     | `<tier>_glaive`            |
| Hammer     | `<tier>_hammer`            |
| Claymore   | `<tier>_claymore`          |
| Chakram    | `<tier>_chakram`           |
| Sai        | `<tier>_sai`               |

Example: `/give @s better_weaponry:netherite_katana`

- All weapons extend `SwordItem`. They use their tier's durability, enchantability and repair material.
- They are added to the `minecraft:swords` item tag, so sword enchantments (Sharpness, Looting, etc.) work on them.
- In the **Combat** creative tab they appear just before the Trident.
- Each weapon sets its own **Attack Reach** through the `entity_interaction_range` attribute.
- The sweep-attack particle appears at `reach / 3` blocks in front of the player (vanilla: always 1 block, which matches reach 3). A dagger sweeps close, a spear or scythe sweeps far away.

- Every weapon can be crafted (see [Crafting recipes](#crafting-recipes)). Netherite versions are smithing-table upgrades of the diamond ones, like vanilla tools.

### Two-handed weapons

By default, the katana, claymore, battleaxe, hammer, glaive and scythe are two-handed. You can change this per weapon type in the config. While a two-handed weapon is in your main hand:

- the off-hand item is hidden in first person
- you can't use the off hand to use items, place blocks or interact with entities

When Better Combat is installed, the mod turns this off and lets Better Combat handle two-handed behaviour.

### Dual wielding

Hold a weapon (any `SwordItem`, `AxeItem` or `TridentItem`, vanilla or modded) in **both** hands. If neither one is two-handed, your attacks alternate between the main hand and the off hand:

- An off-hand hit really uses the off-hand weapon: its damage, attack speed, enchantments, knockback, sweep and durability. If it breaks, it breaks in the off hand.
- The attack cooldown and the crosshair indicator follow the weapon whose turn it is, and `dual_wield_speed_multiplier` makes the cooldown shorter.

> One-handed weapons (dagger, sai, sickle, rapier, chakram, broadsword, spear) can be dual wielded. With the default multiplier of 1.5, dual wielding is worth about as much as the two-handed bonus.

Dual wielding is turned off when Better Combat is installed, because Better Combat has its own dual-wield system.

### Tooltips

Weapon tooltips can show **Attack Reach** and a **Two Handed** tag. You can turn each one off in the config.

### Localization

English (`en_us`), French (`fr_fr`), Spanish (`es_es`), Russian (`ru_ru`) and Simplified Chinese (`zh_cn`). Weapon names follow the official naming style of each language (for example *Épée large en netherite*, *Espada ancha de netherita*, *Незеритовый палаш*, *下界合金阔剑*). The config screen is translated too, tooltips included.

---

## Configuration

The config is a **server** config, so each world has its own copy:

```
<world>/serverconfig/better_weaponry-server.toml
```

For a new world, copy a tuned file into `defaultconfigs/` before you create the world.

### Structure

```toml
[weapon_stats]
  [weapon_stats.katana]
    attack_speed = 1.6      # attacks per second (0–1024)
    attack_reach = 3.0      # blocks (0–64)
    two_handed   = true

    [weapon_stats.katana.wooden_katana]
      attack_damage = 6.0   # total damage shown on the tooltip (0–1024)
    # ...stone / iron / golden / diamond / netherite / modded materials

  # ...one block per weapon type

[other]
  show_attack_reach_tooltip   = true
  show_two_handed_tooltip     = true
  dual_wield_speed_multiplier = 1.5   # 1.0–5.0
```

- `attack_damage`, `attack_speed` and `attack_reach` are the **final** values the player sees. The mod subtracts the player's base values for you (1 damage, 4 speed, 3 reach).
- **Only damage depends on the material.** Speed, reach and two-handed are set per weapon type.
- Stats are read live from the config, so when you edit the file they apply without restarting the game.
- Materials from other mods only appear in the config when that mod is installed.
- A config file that already exists keeps its old values. Delete it to get the current defaults.

### Default stats

Each weapon is calibrated against the vanilla sword (4 damage × 1.6 attacks/s = 6.4 damage per second in wood). Two-handed weapons get about 1.5× that, because they lock the off hand. Reach is factored in: short reach is riskier and gets more DPS, long reach is safer and gets less.

| Reach | 2.0 | 2.5 | 3.0 | 4.0 | 4.5 | 5.0 |
|---|---|---|---|---|---|---|
| DPS factor | 1.25 | 1.15 | 1.00 | 0.85 | 0.80 | 0.75 |

Every weapon lands within 0.98–1.05 of its target.

| Weapon | Hands | Reach | Speed | Wood | Stone | Iron | Diamond | Netherite | Style |
|---|---|---|---|---|---|---|---|---|---|
| Dagger | 1 | 2.0 | 2.0 | 4 | 5 | 6 | 7 | 8 | fastest, point-blank, made for pairs |
| Sai | 1 | 2.5 | 1.8 | 4 | 5 | 6 | 7 | 8 | quick close-range, dual wield |
| Sickle | 1 | 2.5 | 1.4 | 5.5 | 7 | 8 | 9.5 | 11 | heavy hooked cuts up close |
| *Vanilla sword* | 1 | 3.0 | 1.6 | 4 | 5 | 6 | 7 | 8 | *reference* |
| Rapier | 1 | 3.0 | 1.9 | 3.5 | 4.5 | 5 | 6 | 7 | fast light thrusts |
| Chakram | 1 | 3.0 | 1.3 | 5 | 6 | 7.5 | 9 | 10 | slow, every hit counts |
| Katana | 2 | 3.0 | 1.6 | 6 | 7.5 | 9 | 10.5 | 12 | fast two-hander |
| Broadsword | 1 | 4.0 | 1.2 | 4.5 | 5.5 | 7 | 8 | 9 | long one-hander, pairs with a shield |
| Claymore | 2 | 4.0 | 1.1 | 7.5 | 9.5 | 11 | 13 | 15 | most agile heavy weapon |
| Battleaxe | 2 | 4.0 | 0.9 | 9 | 11 | 13.5 | 16 | 18 | heavy and slow |
| Hammer | 2 | 4.0 | 0.7 | 11.5 | 14.5 | 17 | 20 | 23 | slowest, crushing hits (best vs armor) |
| Spear | 1 | 5.0 | 1.2 | 4 | 5 | 6 | 7 | 8 | longest reach, keeps the shield |
| Glaive | 2 | 4.5 | 1.0 | 7.5 | 9.5 | 11 | 13 | 15 | balanced polearm |
| Scythe | 2 | 5.0 | 0.8 | 9 | 11 | 13.5 | 16 | 18 | huge reach, slow wide swings |

Golden = wood damage, like vanilla. Material damage uses the vanilla sword scale (×1 / ×1.25 / ×1.5 / ×1.75 / ×2), so the balance holds at every tier. Modded materials use `(4 + damage bonus) / 4`. No weapon goes above 2.0 attacks/s: after a hit, a mob is invulnerable for 0.5 s, so anything faster would be wasted.

---

## Better Combat compatibility

The mod checks at runtime whether `bettercombat` is loaded. If it is:

- the two-handed off-hand restrictions and the off-hand render hiding are turned off
- the dual-wield alternation is turned off

Better Combat weapon attribute files go in `src/main/resources/data/better_weaponry/weapon_attributes/`. Right now only `wooden_broadsword.json` exists. The other weapons need their own files to get Better Combat attack animations.

---

## Development

### Requirements

- JDK 21
- An IDE with Gradle support (IntelliJ IDEA recommended)

`gradle.properties` sets `org.gradle.jvmargs=-Xmx16G`. If your machine has less RAM, lower this value.

### Common tasks

```powershell
./gradlew runClient     # launch a dev client
./gradlew runServer     # launch a dev dedicated server (nogui)
./gradlew runData       # regenerate item models, tags and recipes into src/generated/resources
./gradlew build         # build the mod jar -> build/libs/better_weaponry-<version>.jar
```

If dependencies get out of sync: `./gradlew --refresh-dependencies`, or `./gradlew clean`, then build again.

### Project layout

```
src/main/java/com/betterweaponry/
├── BetterWeaponry.java          # Mod entry point: registers items, config, creative tab, datagen
├── BetterWeaponryClient.java    # Client-only entry point (config screen)
├── combat/
│   ├── DualWieldSystem.java     # Rules for dual wielding + cooldown of the hand whose turn it is
│   ├── HandSwap.java            # Swaps both hands (items + attributes) during an off-hand attack
│   ├── TwoHandedSystem.java     # Rules for when the off hand is locked
│   └── IDualWieldPlayer.java    # Duck interface mixed into Player for dual-wield state
├── compat/BetterCombatCompat.java  # Detects Better Combat (turns off our systems)
├── config/ServerConfig.java     # All weapon stats and toggles, generated from types × materials
├── registry/ModItems.java       # Registers one item per WeaponType × active WeaponMaterial
├── item/
│   ├── WeaponType.java          # The 13 weapon types (+ base model and crafting pattern)
│   ├── WeaponMaterial.java      # One material: Tier, default stats, how it is crafted, required mod
│   ├── WeaponMaterials.java     # List of all materials (vanilla + mod compatibilities)
│   ├── WeaponRecipe.java        # Crafting pattern with M (material) and S (stick)
│   └── WeaponItem.java          # Single item class; stats read live from config
├── event/                       # Two-handed restrictions, tooltips, first-person hand hiding
├── mixin/                       # Dual-wield combat and third-person rendering hooks
└── datagen/                     # Item model, item tag and recipe providers

src/main/resources/
├── assets/better_weaponry/      # Textures, hand-written models, lang files
├── data/better_weaponry/        # Better Combat weapon_attributes
├── META-INF/neoforge.mods.toml  # Mod metadata (filled in from gradle.properties)
└── betterweaponry.mixins.json   # Mixin config

src/generated/resources/         # Output of runData (do not edit by hand)
```

### Adding a new weapon type

1. Add one line to `item/WeaponType.java`, for example `CUTLASS("cutlass", "cutlass_base", null)`. Pass `null` as the base model to use vanilla `item/handheld`. The third argument is the crafting pattern (`null` = no recipe). The items, config entries, models, tags and recipes all come from this enum.
2. If you gave it a base model, add `assets/better_weaponry/models/item/<base>.json`.
3. Add one texture per material under `assets/better_weaponry/textures/item/`. In each of the 5 lang files, add the item names and a `better_weaponry.configuration.<type>` entry.
4. Run `./gradlew runData` to generate the models, tags and recipes.
5. (Optional) Add Better Combat `weapon_attributes` JSON files.

### Crafting recipes

Each weapon type has a pattern in `WeaponType.java`. `M` is the weapon's material and `S` is a stick (`c:rods/wooden`). `./gradlew runData` writes one recipe per material: `M` becomes planks for the wooden version, `c:ingots/gold` for the golden one, the material's tag for modded materials, and so on. Netherite and Warden do not use the pattern. They are smithing-table upgrades (diamond → netherite → warden).

| Weapon | Pattern | Weapon | Pattern |
|---|---|---|---|
| Broadsword | `MM` / `MM` / `S·` | Battleaxe | `MSM` / `MSM` / `·S·` |
| Spear | `··M` / `·S·` / `S··` | Glaive | `·MM` / `·S·` / `S··` |
| Dagger | `M` / `S` | Hammer | `MMM` / `MSM` / `·S·` |
| Scythe | `MMM` / `··S` / `·S·` | Claymore | `·M·` / `MMM` / `·S·` |
| Rapier | `··M` / `·M·` / `S··` | Chakram | `·M·` / `M·M` / `·M·` |
| Sickle | `MM` / `·M` / `S·` | Sai | `M·M` / `·M·` / `·S·` |
| Katana | `·M` / `·M` / `S·` | | |

These patterns were checked automatically against all 1,290 vanilla 1.21.1 recipes, including mirrored shapes, for every vanilla material: there are no conflicts, with vanilla or between weapons. If you change one, avoid vanilla shapes: sword `M/M/S`, axe, hoe, sign `MMM/MMM/·S·`, lever (`S` above stone), stick (`M/M` with planks). For a fixed extra ingredient, add a symbol:

```java
WeaponRecipe.of("MLM", " S ", " S ").with('L', Tags.Items.LEATHERS)
```

### Mod compatibility

44 materials from other mods are declared in `item/WeaponMaterials.java`. Each one only exists (items, config entries, recipes) when at least one of its mods is installed.

| Mod | Materials |
|---|---|
| Shared metals (Mekanism, Immersive Engineering, Thermal, Occultism, Oreganized, Galosphere, Ice and Fire) | steel, bronze, silver, lead, nickel, aluminum, electrum, constantan |
| Mekanism | osmium, refined obsidian, refined glowstone |
| Thermal Series | invar, signalum, lumium, enderium |
| Create | brass, andesite alloy |
| Botania | manasteel, elementium, terrasteel |
| The Twilight Forest | ironwood, steeleaf, knightmetal, fiery |
| Applied Energistics 2 | certus quartz, fluix |
| The Aether | zanite, gravitite |
| Deeper and Darker | warden (smithing upgrade from netherite) |
| Ice and Fire | dragonbone, fire / ice / lightning dragonsteel |
| Tinkers' Construct | cobalt, manyullyn |
| Allthemodium | allthemodium, vibranium, unobtainium |
| L_Ender's Cataclysm | ignitium, cursium |
| The Undergarden | cloggrum, froststeel, utherium, forgotten |

A shared metal like silver turns on as soon as any of its mods is installed.

### Adding a material from another mod

```java
public static final WeaponMaterial STEEL = register(WeaponMaterial.builder("steel")
        .stats(500, 10, 2.5F)                 // durability, enchantability, damage bonus
        .craftedFrom("#c:ingots/steel")       // "#tag" or "namespace:item", all optional
        .requiresAnyMod("mekanism", "immersiveengineering"));
```

- `stats` builds the Tier without referencing the other mod's classes. Each weapon's damage is multiplied by `(4 + bonus) / 4`: bonus 2 → ×1.5 like iron, bonus 3 → ×1.75 like diamond. The anvil repair item is the material's ingredient.
- `craftedFrom` with several entries generates a tag `better_weaponry:weapon_materials/<id>` whose entries are all **optional**. Put the common `c:` tag first and the mod's exact item as a fallback. A wrong or missing ID breaks nothing.
- Generated recipes only load if one of the mods is installed **and** the ingredient tag is not empty, so you never get load errors or uncraftable recipes.
- For upgrade-style materials, use `.smithingUpgrade(baseMaterial, "template", "addition")` instead of `.craftedFrom(...)`.
- Add the 13 item names to each lang file and the 13 textures `<material>_<type>.png`. The config screen reuses the item names. A missing texture does not stop `runData`: the model is still generated and the console lists the missing textures.
- Modded weapons are added to `minecraft:swords` as optional entries, so the tag keeps working when the mod is absent.

---

## License

All Rights Reserved. © WoroxIV

Mojang mapping names follow the [Mojang mappings license](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).
