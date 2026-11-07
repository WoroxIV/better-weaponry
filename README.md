# Better Weaponry

A NeoForge mod for Minecraft **1.21.1** that adds 13 new weapon types in every vanilla material tier, plus two-handed weapon handling and dual wielding. Every weapon's damage, attack speed, reach and two-handed behaviour can be changed in JSON files, for all worlds or per world.

- **Mod ID:** `better_weaponry`
- **Version:** 2.0.0 (in development: the 2.0 rework is being done in phases)
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
- Each weapon can have **armor penetration**, **on-hit effects** (slowness...), its own **sweep attack** (none for thrusting weapons, wider for the scythe) and can **disable shields** like an axe (see [Combat properties](#combat-properties)).
- Netherite weapons (and those made of fire-resistant modded materials: warden, fiery, ignitium) float in lava and do not burn, like vanilla netherite.

- Every weapon can be crafted (see [Crafting recipes](#crafting-recipes)). Netherite versions are smithing-table upgrades of the diamond ones, like vanilla tools.

### Two-handed weapons

By default, the katana, claymore, battleaxe, hammer, glaive and scythe are two-handed. You can change this per weapon type with the `two_handed` field of its JSON file (see [Configuration](#configuration)). While a two-handed weapon is in your main hand:

- the off-hand item is hidden in first person
- you can't use the off hand to use items, place blocks or interact with entities

When Better Combat is installed, the mod turns this off and lets Better Combat handle two-handed behaviour.

### Dual wielding

Hold a weapon (anything in the `better_weaponry:dual_wieldable` item tag: swords, axes, the trident and the common `c:tools/melee_weapon` tag, vanilla or modded) in **both** hands. If neither one is two-handed, your attacks alternate between the main hand and the off hand:

- An off-hand hit really uses the off-hand weapon: its damage, attack speed, enchantments, knockback, sweep and durability. If it breaks, it breaks in the off hand.
- The attack cooldown and the crosshair indicator follow the weapon whose turn it is, and `dual_wield_speed_multiplier` (in `rules.json`) makes the cooldown shorter.
- A target is protected for only `dual_wield_invulnerability_seconds` (0.25 s by default, vanilla 0.5 s) after a dual-wield hit, so fast weapons such as daggers never lose a hit.
- A data pack can add or remove items from the `better_weaponry:dual_wieldable` tag.

> One-handed weapons (dagger, sai, sickle, rapier, chakram, broadsword, spear) can be dual wielded. With the default multiplier of 1.5, dual wielding is worth about as much as the two-handed bonus.

Dual wielding is turned off when Better Combat is installed, because Better Combat has its own dual-wield system.

### Right-click abilities

Each weapon has at most one right-click ability, set by the `ability` field of its JSON file: `"parry"`, `"dash"` or `"none"`. Only the main-hand weapon's ability is used.

#### Parry

By default the broadsword, katana, glaive, claymore and sai can **parry**, like swords in Minecraft 1.8:

- Hold right click with the weapon in the **main hand**: the weapon tilts in front of you (1.8 pose in first person, arm raised in third person) and you move slowly.
- Hits from the **front** are reduced by `parry.damage_reduction` (50% by default, like 1.8). Hits from behind, falls, fire, poison and other damage that a shield cannot block go through.
- Each parried hit costs durability (`parry.durability_per_damage`, at least 1) and makes a metallic sound (`better_weaponry:item.weapon.parry`, with a subtitle; a resource pack can replace it).
- It works with one-handed and two-handed weapons. A one-handed weapon only parries with an **empty off hand**:
  - a **shield** in the off hand is raised instead;
  - a torch, food or a block in the off hand keeps its normal right-click action;
  - with a weapon in the off hand, only weapons with `parry.while_dual_wielding` parry (the sai, by default).
    Both weapons then take the parry pose, in first and third person.
- A two-handed weapon always parries, since the off hand is locked.
- `parry_enabled: false` in `rules.json` turns the parry off for every weapon.

#### Dash

By default the dagger (sideways dodge) and the rapier (forward lunge) can **dash**:

- Right click with the weapon in the **main hand**: you leap about `dash.distance` blocks (farther on ice or in mid-air), with a small hop, a whoosh (`better_weaponry:item.weapon.dash`) and a puff of cloud.
- `dash.direction`: `"look"` goes where you look (horizontally), `"input"` goes where your movement keys point (A/D to dodge sideways, S to step back, forward when no key is held).
- Then the weapon type has a cooldown (`dash.cooldown_seconds`), shown on the hotbar like an ender pearl's. It is shared by every material: switching to another dagger does not reset it.
- Each dash uses a little food (`dash.hunger_cost`, 0.1 food point by default, about two sprint jumps). Like sprinting, it is impossible with 3 drumsticks or less.
- It only works with the off hand **empty or holding a weapon** (dual wielding is fine): a shield is raised instead, a torch or food keeps its normal action. A two-handed weapon always dashes.
- Never while riding, gliding with an elytra, or in water or lava. On the ground only, unless `dash.allow_airborne` is `true`.
- The movement is done by the player's own game (like vanilla movement); the server handles the cooldown and the hunger.
- `dash_enabled: false` in `rules.json` turns the dash off for every weapon.

### Tooltips

Weapon tooltips can show **Attack Reach** and a **Two Handed** tag. Each player can turn them off in the client config (`config/better_weaponry-client.toml`, or the mod's Config button). Weapons with an ability show a gold line such as "Right Button: Parry (-50% damage from the front)" or "Right Button: Dash (~3.5 blocks, 1.5 s cooldown)", with the key the player actually bound to "Use Item".

### Localization

10 languages: English (`en_us`), French (`fr_fr`), Spanish (`es_es`), German (`de_de`), Dutch (`nl_nl`), Italian (`it_it`), Brazilian Portuguese (`pt_br`), European Portuguese (`pt_pt`), Russian (`ru_ru`) and Simplified Chinese (`zh_cn`).

Weapon names follow the official naming style of each language in vanilla Minecraft:

| Language | Vanilla reference | Rule | Example |
|---|---|---|---|
| English | Netherite Sword | material + weapon | Netherite Broadsword |
| French | Épée en netherite | weapon + "en" + material | Épée large en netherite |
| Spanish | Espada de netherita | weapon + "de" + material | Espada ancha de netherita |
| German | Netheritschwert | compound word | Netheritbreitschwert |
| Dutch | Netherieten zwaard | material adjective + weapon | Netherieten breedzwaard |
| Italian | Spada d'oro | weapon + "di" + material, elision | Ascia da battaglia d'oro |
| Portuguese (BR / PT) | Espada de Netherita / de Netherite | weapon + "de" + Material, Title Case | Espada Larga de Netherita |
| Russian | Незеритовый меч | adjective agreeing with the weapon's gender | Незеритовая коса |
| Chinese | 下界合金剑 | material + weapon | 下界合金阔剑 |

Materials from other mods reuse those mods' official translations when they exist (Create, Mekanism, Botania, Twilight Forest, AE2, Immersive Engineering, Tinkers' Construct, Undergarden). Stat lines use the vanilla word order of each language ("3 en Portée d'attaque", "Portata d'attacco: 3"). The client config screen, the chat messages and the README and schemas of `config/better_weaponry/` are translated in every language.

---

## Configuration

Weapon stats and global rules are **JSON files**. Each player's display preferences are in a small client TOML.

### Where the files are

```
config/better_weaponry/             ← created at first launch, applies to every world
├── README.txt                      ← how to use this folder, written in the game's language
├── weapon.schema.json              ← autocompletion and help on hover in VS Code
├── rules.schema.json
├── rules.json                      ← your global rules (only what you change)
├── weapons/<weapon>.json           ← your weapon changes (only what you change)
└── defaults/                       ← the mod's default values, rewritten at every launch (read only)

config/better_weaponry-client.toml  ← display preferences (tooltips)
```

Layers, from lowest to highest priority (the last one wins):

1. the mod's defaults, `data/better_weaponry/better_weaponry/` in the mod jar;
2. `config/better_weaponry/` (every world);
3. the world's data packs (`<world>/datapacks/`), with the same path as the defaults, for per-world settings.

- Files only contain what you change. When an update changes a default, it still applies to everything you did not touch.
- Changes apply with `/reload` (or when a world is loaded), without restarting the game. Clients receive the server's values automatically.
- An invalid value is ignored and the previous layer's value is kept. Problems (syntax errors, values out of range, misspelled fields with a suggestion) are shown in the chat to operators and to the host of a single-player game, and written to `logs/latest.log`.
- `//` comments are allowed, and so are keys starting with `_` (for example `"_note"`).

### Weapon file

```json
{
  "$schema": "../weapon.schema.json",
  "attack_speed": 1.8,
  "material_damage": { "golden": 6 }
}
```

| Field | Range | Meaning |
|---|---|---|
| `two_handed` | `true` / `false` | While held in the main hand, the off hand is hidden and locked |
| `attack_speed` | 0.1 – 20 | Attacks per second at full strength |
| `attack_reach` | 0.5 – 64 | Reach in blocks |
| `base_damage` | 0 – 1024 | Damage of the wooden version. Each material multiplies it (stone ×1.25, iron ×1.5, diamond ×1.75, netherite ×2, gold ×1, modded `(4 + bonus) / 4`), rounded to 0.5 |
| `material_damage` | 0 – 1024 each | Forces the damage of specific materials, by material id |
| `armor_penetration` | 0 – 100 | % of the target's armor ignored by melee hits. Shown on the tooltip only above 0 |
| `disables_shield` | `true` / `false` | Like an axe, a hit on a raised shield disables it for 5 seconds |
| `on_hit_effects` | up to 8 | Effects given to the target: `{ "effect": "minecraft:slowness", "duration_seconds": 2, "level": 1, "chance": 100 }` (`effect` is required) |
| `sweep.enabled` | `true` / `false` | `false`: no sweep attack at all |
| `sweep.radius` | 0 – 8 | Size of the sweep zone around the target, in blocks (vanilla: 1), still limited by the reach |
| `sweep.damage_ratio` | 0 – 100 | Sweep damage in % of the weapon's damage (vanilla: 0 = 1 damage). Adds up with Sweeping Edge |
| `ability` | `"none"` / `"parry"` / `"dash"` | What a right click does with the weapon in the main hand |
| `parry.damage_reduction` | 0 – 100 | % of the damage blocked by the parry, front hits only (default 50) |
| `parry.durability_per_damage` | 0 – 10 | Durability lost per point of damage blocked, rounded, at least 1 per hit (0 = no wear; default 1) |
| `parry.while_dual_wielding` | `true` / `false` | `true`: also parries with a weapon in the off hand (default `false`) |
| `dash.distance` | 0.5 – 16 | Approximate length of the dash on flat ground, in blocks (default 4) |
| `dash.cooldown_seconds` | 0.1 – 60 | Time before the next dash, shared by every material of the type (default 2) |
| `dash.direction` | `"look"` / `"input"` | Where you look, or where your movement keys point (default `"look"`) |
| `dash.allow_airborne` | `true` / `false` | `true`: also dashes while jumping or falling (default `false`) |
| `dash.hunger_cost` | 0 – 20 | Food points used per dash, 1 = half a drumstick (default 0.1) |

All values are the **final** values the player sees: the mod subtracts the player's base values for you (1 damage, 4 speed, 3 reach). Objects such as `sweep`, `parry` and `dash` merge field by field across layers (`{ "sweep": { "radius": 2 } }` keeps the other sweep values); lists such as `on_hit_effects` are replaced as a whole. Misspelled fields are reported even inside `sweep`, `parry`, `dash` or `on_hit_effects`.

Example: give the spear a parry that blocks 30% of the damage.

```json
{
  "ability": "parry",
  "parry": { "damage_reduction": 30 }
}
```

### rules.json

| Field | Range | Default | Meaning |
|---|---|---|---|
| `dual_wield_speed_multiplier` | 1 – 5 | 1.5 | Attack speed multiplier while holding a weapon in each hand |
| `dual_wield_invulnerability_seconds` | 0 – 1 | 0.25 | How long a target ignores new hits after a dual-wield hit (vanilla: 0.5) |
| `parry_enabled` | `true` / `false` | `true` | `false`: no weapon can parry, whatever its file says |
| `dash_enabled` | `true` / `false` | `true` | `false`: no weapon can dash, whatever its file says |

### Upgrading from 1.x

1.x used `config/better_weaponry-server.toml`. At first launch, 2.0 copies the values you had changed into `config/better_weaponry/` and renames the old file to `better_weaponry-server.toml.old`. The tooltip options are now in the client config. A 1.x file in `<world>/serverconfig/` or `defaultconfigs/` is not converted; a warning is written to the log.

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

### Combat properties

| Weapon | Armor penetration | On hit | Sweep | Disables shields | Right click |
|---|---|---|---|---|---|
| Broadsword | — | — | vanilla | — | parry |
| Spear | 15% | — | none (thrust) | — | — |
| Dagger | — | — | none | — | dash (dodge, ~3.5 blocks, 1.5 s) |
| Scythe | — | — | radius 2, 50% damage | — | — |
| Rapier | 20% | — | none (thrust) | — | dash (lunge, ~4.5 blocks, 2.5 s) |
| Sickle | — | Slowness I, 1 s | radius 0.75 | — | — |
| Katana | 10% | — | vanilla | — | parry |
| Battleaxe | 15% | — | vanilla | yes | — |
| Glaive | 10% | — | radius 1.5 | — | parry |
| Hammer | 30% | Slowness II, 1.5 s | none | yes | — |
| Claymore | 5% | — | radius 1.25 | — | parry |
| Chakram | — | — | vanilla | — | — |
| Sai | — | — | none | — | parry, even with a weapon in each hand |

Every parry blocks 50% of the damage and costs 1 durability per point of damage blocked. Every dash costs 0.1 food point.

These are starting values: damage has not been rebalanced yet to account for them.

---

## Better Combat compatibility

The mod checks at runtime whether `bettercombat` is loaded. If it is:

- the two-handed off-hand restrictions and the off-hand render hiding are turned off
- the dual-wield alternation is turned off

`./gradlew runData` writes one Better Combat weapon attribute file per weapon (`data/better_weaponry/weapon_attributes/<item>.json`, 650 files). Each one uses a Better Combat preset and forces the number of hands of Better Weaponry's defaults:

| Weapon | Preset | Weapon | Preset |
|---|---|---|---|
| Broadsword | `sword` | Battleaxe | `heavy_axe` |
| Spear | `spear` (one-handed) | Glaive | `glaive` |
| Dagger | `dagger` | Hammer | `hammer` |
| Scythe | `scythe` | Claymore | `claymore` |
| Rapier | `rapier` | Chakram | `sickle` |
| Sickle | `sickle` | Sai | `dagger` |
| Katana | `katana` | | |

The mapping is in `datagen/BetterCombatAttributesProvider.java`. Armor penetration, on-hit effects, shield disabling, the parry and the dash still work with Better Combat. Better Combat ignores the files of weapons whose mod is not installed.

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
./gradlew runGameTestServer  # run the automated combat tests, then exit (fails if a test fails)
./gradlew runData       # regenerate item models, translations, tags and recipes into src/generated/resources
./gradlew build         # build the mod jar -> build/libs/better_weaponry-<version>.jar
```

If dependencies get out of sync: `./gradlew --refresh-dependencies`, or `./gradlew clean`, then build again.

### Project layout

```
src/main/java/com/betterweaponry/
├── BetterWeaponry.java          # Mod entry point: items, creative tab, datagen, config folder
├── BetterWeaponryClient.java    # Client-only entry point (client config, config screen, render extensions)
├── client/                     # 1.8 parry pose in first person (both hands when parrying with two weapons)
├── combat/
│   ├── CombatEvents.java        # Armor penetration, sweep on/off, dual-wield invulnerability time
│   ├── ParrySystem.java         # When a right click parries, frontal damage reduction, wear and sound
│   ├── DashAbility.java         # When a right click dashes, impulse (client), cooldown and hunger (server)
│   ├── DualWieldSystem.java     # Rules for dual wielding + cooldown of the hand whose turn it is
│   ├── HandSwap.java            # Swaps both hands (items + attributes) during an off-hand attack
│   ├── TwoHandedSystem.java     # Rules for when the off hand is locked
│   └── IDualWieldPlayer.java    # Duck interface mixed into Player for dual-wield state
├── compat/BetterCombatCompat.java  # Detects Better Combat (turns off our systems)
├── config/
│   ├── ClientConfig.java        # Display preferences (client TOML)
│   ├── ConfigFolder.java        # config/better_weaponry/: defaults copy, user files, schemas, README
│   ├── Docs.java, ReadmeWriter.java, SchemaWriter.java  # Documentation generated from the field declarations
│   └── LegacyConfigMigration.java  # Converts the 1.x TOML
├── data/
│   ├── WeaponDefinition.java    # Fields of a weapon file
│   ├── OnHitEffect.java, SweepDefinition.java, ParryDefinition.java, DashDefinition.java  # Sub-objects of a weapon file
│   ├── WeaponAbility.java, DashDirection.java  # Values of the "ability" and "dash.direction" fields
│   ├── RulesDefinition.java     # Fields of rules.json
│   ├── DataField.java, FieldSet.java  # Field declarations, validation, merging of the layers
│   ├── WeaponDataLoader.java    # Loads the files when a world loads and on /reload
│   ├── WeaponData.java          # Data in effect (loaded by the server, or synced from it)
│   └── WeaponDataEvents.java    # Reload listener, sync after /reload, error messages for operators
├── network/                     # Sends the weapon data to clients (at login and after /reload)
├── registry/
│   ├── ModItems.java            # Registers one item per WeaponType × active WeaponMaterial
│   ├── ModAttributes.java       # Armor penetration attribute (on every living entity)
│   ├── ModSounds.java           # Parry and dash sounds (files and subtitles in assets/better_weaponry/sounds.json)
│   └── ModTags.java             # better_weaponry:dual_wieldable
├── gametest/CombatGameTests.java  # Automated tests (gradlew runGameTestServer)
├── item/
│   ├── WeaponType.java          # The 13 weapon types (+ base model and crafting pattern)
│   ├── WeaponMaterial.java      # One material: Tier, default stats, how it is crafted, required mod
│   ├── WeaponMaterials.java     # List of all materials (vanilla + mod compatibilities)
│   ├── WeaponRecipe.java        # Crafting pattern with M (material) and S (stick)
│   └── WeaponItem.java          # Single item class; stats read live from the weapon data
├── event/                       # Two-handed restrictions, tooltips, first-person hand hiding
├── mixin/                       # Dual-wield combat and third-person rendering hooks
└── datagen/                     # Item model, item tag, recipe and Better Combat providers
    └── lang/                    # Builds the lang files from src/main/lang/ and checks they are complete

src/main/lang/                   # Translation sources, one per language (not packaged, read by runData)

src/main/resources/
├── assets/better_weaponry/      # Textures, hand-written models, sounds.json
├── data/better_weaponry/        # Default weapon stats and rules (better_weaponry/), empty GameTest structure
├── META-INF/neoforge.mods.toml  # Mod metadata (filled in from gradle.properties)
└── betterweaponry.mixins.json   # Mixin config

src/generated/resources/         # Output of runData (do not edit by hand)
```

### Adding a new weapon type

1. Add one line to `item/WeaponType.java`, for example `CUTLASS("cutlass", "cutlass_base", null)`. Pass `null` as the base model to use vanilla `item/handheld`. The third argument is the crafting pattern (`null` = no recipe). The items, models, tags and recipes all come from this enum. Then add its stats file `data/better_weaponry/better_weaponry/weapons/cutlass.json` (copy an existing one).
2. If you gave it a base model, add `assets/better_weaponry/models/item/<base>.json`.
3. Add one texture per material under `assets/better_weaponry/textures/item/`. In each translation source `src/main/lang/<language>.json`, add the weapon's name in `names.types` (with its grammatical gender for German, Dutch, Portuguese and Russian) and its plural in `strings` (`better_weaponry.configuration.<type>`). The 50 item names per language are built automatically (see [Translations](#translations)).
4. Run `./gradlew runData` to generate the models, translations, tags and recipes.
5. Add the weapon's Better Combat preset in `datagen/BetterCombatAttributesProvider.java` (`runData` fails without one).

### Translations

The lang files in `src/generated/resources/assets/better_weaponry/lang/` are **generated**: edit the sources in `src/main/lang/<language>.json`, then run `./gradlew runData`.

A source has two parts:

```json
{
  "names": {
    "pattern": "{material}{type|lower}",
    "types": { "scythe": { "name": "Sense", "gender": "f" }, "katana": { "name": "Katana", "gender": "n" } },
    "materials": {
      "netherite": "Netherit",
      "forgotten": { "pattern": "{material} {type}", "m": "Vergessener", "f": "Vergessene", "n": "Vergessenes" }
    },
    "overrides": { "golden_sai": "A name that breaks the rule" }
  },
  "strings": { "better_weaponry.configuration.scythe": "Sensen" }
}
```

- The 650 weapon names (13 types × 50 materials) are built from `pattern`: `{type}` and `{material}` are replaced by the names above. `{type|lower}` puts the first letter in lower case (German compounds, Dutch and Russian adjectives), `|upper` in upper case.
- A type can have a grammatical `gender`. A material can then give one form per gender (the key is the gender), a common form `name`, and its own `pattern`.
- `overrides` forces the full name of one item.
- `strings` holds every other text, with exactly the same keys as `en_us`.

`runData` fails with the list of problems if a language misses a text, a weapon type or a material, or contains an unknown key. Nothing is written until every language is complete.

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

44 materials from other mods are declared in `item/WeaponMaterials.java`. Each one only exists (items, recipes) when at least one of its mods is installed. Its damage follows the weapon's `base_damage` and can be forced with `material_damage`.

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
- Add the material's name to `names.materials` in each translation source `src/main/lang/<language>.json` (one entry per language, not 13 item names; see [Translations](#translations)), and the 13 textures `<material>_<type>.png`. A missing texture does not stop `runData`: the model is still generated and the console lists the missing textures.
- Modded weapons are added to `minecraft:swords` as optional entries, so the tag keeps working when the mod is absent.

---

## License

All Rights Reserved. © WoroxIV

Mojang mapping names follow the [Mojang mappings license](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).
