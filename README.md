# Better Weaponry

A mod for Minecraft **1.20.1**, on Forge and Fabric, that adds 13 new weapon types, shields, bows, crossbows and shurikens in every vanilla material tier and up to 44 materials from other mods, with generated textures and data-driven stats.

The combat system (combos, parries, dashes, two-handed weapons, dual wielding, the in-game editor and the tutorials) is now a separate mod, **Combat Reforged**. Better Weaponry does not need it: with both installed, the weapons behave exactly as they did in 2.0.

- **Mod ID:** `better_weaponry`
- **Version:** 3.0.0
- **Author:** WoroxIV
- **Loaders:** Forge 47.4 or newer, or Fabric with [Fabric API](https://modrinth.com/mod/fabric-api). The 1.21.1 version (NeoForge and Fabric) is on the `mc/1.21.1` branch
- **Java:** 17
- **Works with:** Combat Reforged, Better Combat, Epic Fight (all optional), JEI and EMI information pages
- **Download:** [Modrinth](https://modrinth.com/mod/better-weaponry), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/better-weaponry-system)
- **Bug reports:** [GitHub issues](https://github.com/WoroxIV/better-weaponry/issues)

---

## Features

### Weapons

Each weapon type comes in six vanilla tiers: **wooden, stone, iron, golden, diamond, netherite**. With the shield, bow, crossbow and shuriken, that makes 17 item types and 102 items. Up to 44 more materials come from other mods (see [Mod compatibility](#mod-compatibility)).

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
- They have their own **Better Weaponry** creative tab, from the shortest reach to the longest.
- Each weapon sets its own **Attack Reach** through the `entity_interaction_range` attribute. It is the real distance in blocks in every game mode: the extra reach of Creative mode only applies to other items.
- The sweep-attack particle appears at `reach / 3` blocks in front of the player (vanilla: always 1 block, which matches reach 3). A dagger sweeps close, a spear or scythe sweeps far away.
- Each weapon can have **armor penetration**, **on-hit effects** (slowness...), its own **sweep attack** (none for thrusting weapons, wider for the scythe) and can **disable shields** like an axe (see [Combat properties](#combat-properties)).
- Netherite weapons (and those made of fire-resistant modded materials: warden, fiery, ignitium) float in lava and do not burn, like vanilla netherite.

- Every weapon can be crafted (see [Crafting recipes](#crafting-recipes)). Netherite versions are smithing-table upgrades of the diamond ones, like vanilla tools.

### Shields

Shields exist in every material (`wooden_shield`, `iron_shield`, `netherite_shield`...). They block like the vanilla shield, take banners, and accept the same enchantments.

- Durability: that of the material's tools, except wood (118) and gold (64).
- Recipe: the vanilla shape, with the material instead of the iron ingot and planks everywhere else. Netherite and other upgrade materials use the smithing table.
- Texture: generated from the template `textures/templates/entity/shield/shield.png` and the material's palette (see [Weapon textures](#weapon-textures)). A file `textures/entity/shield/<material>_shield.png` (64×64) takes priority.
- **Perfect parry**: a hit taken just after the shield is up does not wear it and cannot disable it. It pushes melee attackers back and sends arrows back to their shooter, and your next hit deals 20% more damage. See `gear.json`.

The vanilla shield is replaced everywhere it can be: its recipe is off, it is gone from the creative tab, loot and villagers give the mod's shields (better materials are rarer), armorers sell better ones as they level up, and vanilla shields in a player's inventory or ender chest become wooden shields at login. Turn off `replace_vanilla_gear` in `gear.json` for loot, trades and the login conversion; the vanilla recipes stay off either way, because the mod's versions use the same shapes.

A recipe from the game or from another mod that asks for a vanilla shield, bow or crossbow accepts the mod's versions, in any material.

### Bows and crossbows

Bows and crossbows exist in every material (`iron_bow`, `iron_crossbow`...), with the vanilla animations, and they accept the vanilla enchantments.

- The material only changes the **projectile speed**: arrow damage depends on speed, so a better material hits harder and flies straighter. Raw damage and rate of fire stay vanilla. Multipliers: wood 0.62, stone 1 (the vanilla bow), iron 1.12, diamond 1.45, netherite 1.62 (see `gear.json`), which gives 4, 6, 7, 9 and 10 damage for a fully drawn bow.
- **Gold** is the exception: it draws or loads twice as fast, enchants very well, but shoots at ×0.45 (3 damage) and breaks quickly.
- Durability: bow 384, crossbow 465 for wood, stone and iron; gold 49 / 60; diamond 2398 / 2903; netherite 3120 / 3778; modded materials follow the ratio of their tools to iron tools.
- Recipes: the vanilla shapes with the material instead of sticks (the wooden versions keep the sticks, and the wooden crossbow takes stone instead of the iron ingot), netherite at the smithing table.
- Textures: generated from the templates `textures/templates/item/bow.png`, `bow_pulling_0` to `2`, `crossbow.png`, `crossbow_pulling_0` to `2`, `crossbow_arrow` and `crossbow_firework` (see [Weapon textures](#weapon-textures)). The same names prefixed by `<material>_` in `textures/item/` take priority.

The vanilla bow and crossbow are replaced like the vanilla shield. Skeletons, strays, bogged, illusioners, pillagers and piglins get the mod's versions when they spawn (stone by default, which shoots like the vanilla weapon, with a 9.5% chance per step to move up a material, more on Hard). Their aim adapts to the speed of the arrow. Fletchers and armorers sell better materials as they level up, up to diamond: villagers never sell netherite. Recipes that ask for a bow accept any bow of the mod, like the dispenser, and the Ol' Betsy advancement accepts any crossbow.

### Shurikens

`<material>_shuriken`: thrown with right click, one every 0.6 seconds. Durability is that of the material's tools divided by 3 (wood 19, stone 43, iron 83, gold 10, diamond 520, netherite 677).

- Each throw costs 1 durability, picking the shuriken up gives it back. After a hit, it falls to the ground so you can get it back.
- Throwing the last point destroys the item; picking up that shuriken recreates it with 1 durability and its enchantments.
- A shuriken can only be picked up by its thrower, and only if the item still exists or was destroyed by a throw. A free throw (Unbreaking) gives nothing back.
- Enchantments: Power, Punch, Flame, Piercing or Multishot, Unbreaking, Mending, and **Returning** (flies back like a Loyalty trident, incompatible with Mending). Infinity and Quick Charge are refused.
- Textures: generated from `textures/templates/item/shuriken.png` (see [Weapon textures](#weapon-textures)); `textures/item/<material>_shuriken.png` takes priority. The ones provided are placeholders.

### Mobs

A mob holding one of the mod's weapons hits as hard as with a vanilla sword of the same material, whatever the weapon, because mobs attack at their own pace. The number of armed mobs does not change. A hostile mob that gets a vanilla weapon or tool at spawn has a 50% chance to get a random weapon of the mod instead, in the same material or better, and 5% of those hold one weapon in each hand. The off-hand weapon is never dropped. The best material is iron in the Overworld, diamond in the Nether and the End, and one step more on Hard (not in the End).

Mobs ignore `two_handed` and combos. They do respect the reach of their weapon, alternate hands when dual wielding, and can raise a guard with a shield or a parrying weapon; a guard reduces hits like a player's parry and an axe breaks it. 5% of skeletons throw shurikens, and patrol and raid captains carry a shield. All of this is off with `combat_changes_enabled: false`, Better Combat or Epic Fight, except for the weapons themselves.

### Enchantments

| Enchantment | Effect | Goes on | How to get it |
|---|---|---|---|
| Reflective I-III | On a perfect parry, sends 10/20/30% of a melee hit back, with its effects (Fire Aspect, on-hit effects, knockback, a cave spider's poison...). The item then goes on cooldown (5 s). Incompatible with Riposte | Shields and weapons that parry | End cities |
| Vampirism I-III | Heals `2 × ln(1 + damage × (10/20/30%) / 2)` per melee hit, at most every 0.5 s. Incompatible with Bleeding | Melee weapons | Nether chests |
| Riposte I-III | After a perfect parry, the next hit deals 25/50/75% more. Incompatible with Reflective | Weapons that parry | Enchanting table, End cities |
| Momentum I-III | Dash cooldown −15/30/45% | Weapons that dash | Enchanting table, End cities |
| Execution I-III | +10/20/30% damage on targets below 30% health | Melee weapons | Enchanting table, Nether chests |
| Bleeding I-III | Charged hits make the target bleed 0.5 HP per second for 2/3/4 s (skeletons are immune, incompatible with Fire Aspect and Vampirism) | Sickles, scythes, katanas | Enchanting table, Nether chests |

Reflective, Riposte and Momentum follow the weapon's configured `ability`: giving `parry` to a weapon in the editor makes it accept them. Descriptions are provided for the Enchantment Descriptions mod. With Better Combat or Epic Fight, Reflective only goes on shields and works on any block; Riposte and Momentum no longer apply.

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

Weapon stats and global rules are **JSON files**, which you can change **in game** with the editor or by hand. Each player's display preferences are in a small client TOML.

### In-game editor and Codex

Open it with `/betterweaponry`, or with the **Config** button of the mod list (also from the main menu).

- On the left: global rules, the 13 weapons, and your display preferences. A `*` marks a file with unsaved changes.
- On the right: every field of the file, grouped in sections (stats, on-hit effects, sweep, parry, dash, combo). The parry, dash and combo sections are folded when the weapon does not use them; click a title to fold or unfold it.
- Hover a field's name to read its help, range and default value. A **yellow** name differs from the mod's default value (the `↺` button puts it back); a **red** name is an invalid value, which cannot be saved.
- Lists (on-hit effects, combo hits) have buttons to add, remove and reorder their entries.
- Above the fields: the weapon's DPS with iron, compared to the vanilla iron sword, updated as you type.
- **Save** writes only the values that differ from the lower layers, then applies them right away without a full `/reload` (players receive the new values). Comments in a rewritten file are lost.
- **Target** (in game): *All worlds* writes to `config/better_weaponry/` (on a server, the server's folder); *This world* writes to the data pack `<world>/datapacks/better_weaponry_editor/`, which has priority over everything else. If a value you saved for all worlds is overridden by a world setting, the editor tells you which fields.
- **Copy JSON** copies what differs from the mod's defaults (to share a setup or paste it into a file); **Paste JSON** applies JSON from the clipboard (invalid fields are ignored); **Reset** puts every value back to the mod's defaults (until you save).
- Only operators (and the host of a single-player world) can save. Other players get the **Codex**: the same screen, read only (`/betterweaponry codex`).
- From the main menu, the editor changes the files of `config/better_weaponry/` directly; they apply when a world is loaded.

Commands:

| Command | Who | What |
|---|---|---|
| `/betterweaponry` | everyone | editor for operators, Codex for the others |
| `/betterweaponry editor` | operators | the editor |
| `/betterweaponry codex` | everyone | the Codex (read only) |
| `/betterweaponry dps` | everyone | DPS of every weapon with iron and netherite, compared to the vanilla iron sword |
| `/betterweaponry reload` | operators | reloads only the weapon data (faster than `/reload`) |

### Where the files are

```
config/better_weaponry/             ← created at first launch, applies to every world
├── README.txt                      ← how to use this folder, written in the game's language
├── weapon.schema.json              ← autocompletion and help on hover in VS Code
├── rules.schema.json
├── rules.json                      ← your global rules (only what you change)
├── weapons/<weapon>.json           ← your weapon changes (only what you change)
└── defaults/                       ← the mod's default values, rewritten at every launch (read only)

config/better_weaponry/gear.json    ← your shield and loot settings (only what you change)
config/better_weaponry-client.json  ← display preferences (tooltips, on-screen elements)
```

Layers, from lowest to highest priority (the last one wins):

1. the mod's defaults, `data/better_weaponry/better_weaponry/` in the mod jar;
2. `config/better_weaponry/` (every world);
3. the world's data packs (`<world>/datapacks/`), with the same path as the defaults, for per-world settings;
4. the data pack written by the in-game editor for "this world" (`<world>/datapacks/better_weaponry_editor/`), always last.

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
| `dual_wielded` | `true` / `false` | Can be used with another dual-wielded weapon in the other hand (ignored if two-handed) |
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
| `parry.durability_per_damage` | 0 – 10 | Durability lost per point of damage of the parried hit, rounded down (0 = no wear; default 0.5) |
| `parry.perfect_window_seconds` | 0 – 2 | Start of the parry during which a hit is blocked entirely (default 0.25, 0 = never) |
| `parry.perfect_cooldown_seconds` | 0 – 10 | Delay before raising the guard can open a new perfect window (default 1) |
| `parry.guard_break_seconds` | 0 – 10 | How long the weapon cannot parry after a guard break (default 1.5, 0 = never broken) |
| `parry.perfect_damage_bonus` | 0 – 500 | Extra damage on the next melee hit after a perfect parry, in % (default 20) |
| `parry.perfect_bonus_seconds` | 0.5 – 30 | How long that bonus stays available (default 3) |
| `dash.distance` | 0.5 – 16 | Approximate length of the dash on flat ground, in blocks (default 4) |
| `dash.cooldown_seconds` | 0.1 – 60 | Time before the next dash, shared by every material of the type (default 2) |
| `dash.direction` | `"look"` / `"input"` | Where you look, or where your movement keys point (default `"look"`) |
| `dash.allow_airborne` | `true` / `false` | `true`: also dashes while jumping or falling (default `false`) |
| `dash.hunger_cost` | 0 – 20 | Food points used per dash, 1 = half a drumstick (default 0.1) |
| `combo.enabled` | `true` / `false` | This weapon has a combo (default `false`) |
| `combo.reset_after_seconds` | 0.5 – 10 | The combo starts over after this long without landing a hit (default 2) |
| `combo.min_charge` | 0 – 100 | A hit charged below this % resets the combo (default 90) |
| `combo.steps` | up to 8 | The hits, in order. `{}` = normal hit. Fields of a hit below |
| `combo.steps[].both_hands` | `true` / `false` | Strike with both weapons at once (same type in each hand) |
| `combo.steps[].radius` | 0 – 8 | 0 = normal hit; above 0, area hit of this radius in blocks |
| `combo.steps[].angle` | 10 – 360 | Width of the area in front of you, in degrees (default 360) |
| `combo.steps[].damage_multiplier` | 0 – 10 | Damage of this hit × this value (default 1) |
| `combo.steps[].knockback` | 0 – 5 | Extra knockback, in Knockback enchantment levels (default 0) |
| `combo.steps[].cooldown_multiplier` | 0.1 – 5 | Cooldown of the next hit × this value (default 1) |

All values are the **final** values the player sees: the mod subtracts the player's base values for you (1 damage, 4 speed, 3 reach). Objects such as `sweep`, `parry`, `dash` and `combo` merge field by field across layers (`{ "sweep": { "radius": 2 } }` keeps the other sweep values); lists such as `on_hit_effects` and `combo.steps` are replaced as a whole. Misspelled fields are reported even inside `sweep`, `parry`, `dash`, `combo` or `combo.steps`.

Example: give the spear a parry that blocks 30% of the damage.

```json
{
  "ability": "parry",
  "parry": { "damage_reduction": 30 }
}
```

### Applying a weapon's settings to other items

The `items` field of a weapon file lists other items that behave like this weapon, by id or by tag:

```json
{ "items": ["minecraft:diamond_sword", "#minecraft:axes"] }
```

They get its parry or dash, combo, on-hit effects, sweep, armor penetration, shield disabling, two-handed and dual wield settings, but **keep their own damage, attack speed and reach**. An exact id wins over a tag, items of a missing mod are ignored, the mod's own weapons are never changed, and an item claimed by two files is reported to operators.

### rules.json

| Field | Range | Default | Meaning |
|---|---|---|---|
| `combat_changes_enabled` | `true` / `false` | `true` | `false`: vanilla combat, every system below is off at once. Weapons keep their own damage and attack speed |
| `dual_wield_speed_multiplier` | 1 – 5 | 1.5 | Attack speed multiplier while holding a weapon in each hand |
| `dual_wield_invulnerability_seconds` | 0 – 1 | 0.25 | How long a target ignores new hits after a dual-wield hit (vanilla: 0.5) |
| `balance_abilities` | `true` / `false` | `false` | Weapons with abilities deal slightly less damage, 12% less at most (see [Default stats](#default-stats)) |
| `pvp_max_hit_percent` | 1 – 100 | 60 | Most damage a single melee hit can deal to another player, after armor, in % of their maximum health. 100: no limit |
| `reach_enabled` | `true` / `false` | `true` | `false`: every weapon has the normal reach of a player |
| `sweep_enabled` | `true` / `false` | `true` | `false`: weapons sweep like a vanilla sword |
| `armor_penetration_enabled` | `true` / `false` | `true` | `false`: no weapon ignores armor |
| `on_hit_effects_enabled` | `true` / `false` | `true` | `false`: weapons give no effect to their target |
| `shield_disabling_enabled` | `true` / `false` | `true` | `false`: the mod's weapons no longer disable shields |
| `two_handed_enabled` | `true` / `false` | `true` | `false`: no weapon locks the off hand |
| `dual_wield_enabled` | `true` / `false` | `true` | `false`: only the main hand attacks |
| `parry_enabled` | `true` / `false` | `true` | `false`: no weapon can parry, whatever its file says |
| `perfect_parry_enabled` | `true` / `false` | `true` | `false`: no perfect parry |
| `guard_break_enabled` | `true` / `false` | `true` | `false`: axes no longer interrupt a parry |
| `dash_enabled` | `true` / `false` | `true` | `false`: no weapon can dash, whatever its file says |
| `combos_enabled` | `true` / `false` | `true` | `false`: no weapon has a combo, whatever its file says |

### gear.json

| Field | Range | Default | Meaning |
|---|---|---|---|
| `shield_perfect_window_seconds` | 0 – 2 | 0.25 | Once the shield is up, how long a hit counts as a perfect parry (0 = never) |
| `shield_perfect_cooldown_seconds` | 0 – 10 | 1 | Delay before raising the shield can open a new window |
| `shield_perfect_damage_bonus` | 0 – 500 | 20 | Extra damage on the next melee hit, in % |
| `shield_perfect_bonus_seconds` | 0.5 – 30 | 3 | How long that bonus stays available |
| `shield_perfect_knockback` | 0 – 5 | 0.4 | How far a melee attacker is pushed back |
| `reflect_arrows` | `true` / `false` | `true` | A perfect parry with a shield sends arrows back |
| `replace_vanilla_gear` | `true` / `false` | `true` | Loot, trades and the login conversion use the mod's versions |
| `loot_upgrade_chance` | 0 – 100 | 20 | In loot, chance to move up one material, rolled at each step (wood, gold, stone, iron, diamond, netherite) |
| `enchanted_book_chance` | 0 – 100 | 20 | Chance for a Nether or End chest to hold a book with one of the mod's enchantments |
| `reflective_cooldown_seconds` | 0 – 60 | 5 | Cooldown of the item after Reflective sends damage back (0 = none) |
| `projectile_speed_multipliers` | 0.1 – 5 each | see above | Projectile speed by material id; other materials use their damage multiplier minus 0.34 |
| `projectile_speed_cap` | 0.1 – 5 | 2 | Highest projectile speed multiplier, whatever the material |
| `golden_draw_time` | 0.1 – 1 | 0.5 | Draw or load time of gold compared to vanilla |
| `shurikens` | object | see the editor | `throw_speed`, `base_damage`, `cooldown_seconds` |
| `mobs` | object | see the editor | `upgrade_chance`, `modded_weapon_chance`, `dual_wield_chance`, `ninja_skeleton_chance`, `captain_shield`, `guard_chance` |

### Upgrading from 1.x

1.x used `config/better_weaponry-server.toml`. At first launch, 2.0 copies the values you had changed into `config/better_weaponry/` and renames the old file to `better_weaponry-server.toml.old`. The tooltip options are now in the client config. A 1.x file in `<world>/serverconfig/` or `defaultconfigs/` is not converted; a warning is written to the log.

### Default stats

Each weapon is calibrated against the vanilla sword of the same material. Its netherite damage per second (8 damage × 1.6 attacks/s = 12.8 DPS) is multiplied by:

- **Hands**: ×1 for a one-handed weapon, ×1.4 for a two-handed weapon (the off hand is locked), ×1.2 for a pair of dual-wielded weapons. A single dual-wielded weapon is about ×0.8, so crafting the second one pays off.
- **Reach**: `1 - 0.04 × (reach - 3)`. Reach counts, but only a little.

At netherite, every weapon lands within 2.5% of its target, with attack speeds of one decimal at most and damage in half points. DPS is measured on the real rhythm of the game, in whole ticks. The other materials use the same formula, so the balance holds at every tier. Abilities, effects, armor penetration and area damage do not count in this default balance.

| Weapon | Hands | Reach | Speed | Wood | Stone | Iron | Diamond | Netherite | Style |
|---|---|---|---|---|---|---|---|---|---|
| Dagger | 1 | 2.0 | 2.0 | 3 | 3.5 | 4 | 5 | 5.5 | fastest, point-blank, made for pairs |
| Sai | 1 | 2.5 | 1.8 | 3 | 3.5 | 4 | 5 | 5.5 | quick close-range, parries in pairs |
| Sickle | 1 | 2.5 | 1.4 | 4 | 5 | 6 | 7 | 8 | heavy hooked cuts up close |
| *Vanilla sword* | 1 | 3.0 | 1.6 | 4 | 5 | 6 | 7 | 8 | *reference* |
| Rapier | 1 | 3.0 | 1.6 | 3 | 4 | 4.5 | 5.5 | 6 | light thrusts that pierce armor |
| Chakram | 1 | 3.0 | 1.0 | 5 | 6.5 | 7.5 | 9 | 10 | slow, every hit counts |
| Katana | 2 | 3.5 | 1.6 | 5.5 | 6.5 | 8 | 9 | 10.5 | fast two-hander |
| Broadsword | 1 | 4.0 | 1.2 | 5.5 | 6.5 | 8 | 9 | 10.5 | long one-hander, pairs with a shield |
| Claymore | 2 | 4.0 | 1.1 | 8 | 9.5 | 11.5 | 13.5 | 15.5 | most agile heavy weapon |
| Battleaxe | 2 | 4.0 | 0.9 | 9.5 | 12 | 14.5 | 16.5 | 19 | heavy and slow |
| Hammer | 2 | 4.0 | 0.7 | 12.5 | 15.5 | 19 | 22 | 25 | slowest, crushing hits (best vs armor) |
| Glaive | 2 | 4.5 | 1.0 | 8.5 | 10.5 | 13 | 15 | 17 | balanced polearm |
| Spear | 1 | 5.0 | 1.2 | 5 | 6.5 | 7.5 | 9 | 10 | longest reach, keeps the shield |
| Scythe | 2 | 5.0 | 0.8 | 10.5 | 13 | 15.5 | 18 | 20.5 | huge reach, slow wide swings |

Golden = wood damage, like vanilla. Material damage uses the vanilla sword scale (×1 / ×1.25 / ×1.5 / ×1.75 / ×2), so the balance holds at every tier. Modded materials use `(4 + damage bonus) / 4`. No weapon goes above 2.0 attacks/s: after a hit, a mob is invulnerable for 0.5 s, so anything faster would be wasted.

### Combat properties

| Weapon | Armor penetration | On hit | Sweep | Disables shields | Right click | Combo |
|---|---|---|---|---|---|---|
| Broadsword | — | — | vanilla | — | parry | — |
| Spear | 15% | — | none (thrust) | — | — | 3rd hit 30° thrust |
| Dagger | — | — | none | — | dash (dodge, ~3.5 blocks, 1.5 s) | two daggers: 4th hit with both |
| Scythe | — | — | radius 2, 50% damage | — | — | 3rd hit 360° |
| Rapier | 20% | — | none (thrust) | — | dash (lunge, ~4.5 blocks, 2.5 s) | — |
| Sickle | — | Slowness I, 1 s | radius 0.75 | — | — | two sickles: 3rd hit with both |
| Katana | 10% | — | vanilla | — | parry | 3rd hit 90° |
| Battleaxe | 15% | — | vanilla | yes | — | 2nd hit 180° |
| Glaive | 10% | — | radius 1.5 | — | parry | — |
| Hammer | 30% | Slowness II, 1.5 s | none | yes | — | 2nd hit 360° shockwave |
| Claymore | 5% | — | radius 1.25 | — | parry | 3rd hit 180° |
| Chakram | — | — | vanilla | — | — | two chakrams: 3rd hit with both, 360° |
| Sai | — | — | none | — | parry, even with a weapon in each hand | — |

Every parry blocks 50% of the damage and costs half of the hit's damage in durability. Every dash costs 0.1 food point.

Combo finishers deal as much extra damage as they add recovery time, so a combo costs no DPS on a single target. In PvP, one melee hit never removes more than `pvp_max_hit_percent` of the target's maximum health (60% by default), so a player at full health always survives the first hit.

Machines and other fake players (Create deployers...) use these weapons as plain weapons: only the weapon's damage applies.

Set `balance_abilities` to `true` in `rules.json` to also count these properties: weapons with a parry or dash (-4%), an area combo (-3%), on-hit effects (-2%), shield disabling (-2%) or armor penetration (-1% per 10%) deal slightly less damage, 12% less at most.

---

## Other mods

- **Apothic Attributes**: when it is installed, weapons use its Armor Shred attribute instead of the mod's armor penetration, and its armor calculation applies. Tooltips show "Armor Shred" in the same place.
- **Apotheosis**: melee weapons, bows, crossbows and shields get affixes like vanilla gear. Shurikens do not.
- **Iris and Sodium**: the tutorials and the large weapon models work with shaders on. Oculus does not exist for NeoForge 1.21.1: use Iris.
- **Machines and fake players** (Create deployers...): the weapon only applies its damage, without combos, parry, dash or effects.

---

## Other combat mods

Better Weaponry detects these mods and adapts. It never needs them.

| Mod | What changes |
|---|---|
| Combat Reforged | Weapons get their combos, parries and abilities. Stats stay those of Better Weaponry 2.0. Reach is handled by Combat Reforged |
| Better Combat | Every weapon has a Better Combat preset and its own range. Dual-wielded weapons get a damage profile adapted to its attack speed |
| Epic Fight | Every weapon has an Epic Fight type and hit area. Damage and speed offsets are set in the generated files |

The weapons' own reach turns off when a mod listed in `reach_disabled_by_mods` (`config/better_weaponry/compat.json`) is loaded. By default: `bettercombat`, `combat_reforged`, `epicfight`.

Damage values follow a profile chosen from the loaded mods, in `data/better_weaponry/better_weaponry/balance/`. Your own values in `config/better_weaponry/weapons/` always win.

---

## Development

### Requirements

- JDK 25 or newer to run Gradle (the Fabric build tool needs it); the mod itself is compiled with Java 21, which Gradle downloads if needed
- An IDE with Gradle support (IntelliJ IDEA recommended)

`gradle.properties` sets `org.gradle.jvmargs=-Xmx4G`. If your machine has less RAM, lower this value.

### Common tasks

```powershell
./gradlew :neoforge:runClient     # launch a dev client (or :fabric:runClient)
./gradlew :neoforge:runServer     # launch a dev dedicated server (nogui)
./gradlew runGameTestServer  # run the automated combat tests on every loader, then exit (fails if a test fails)
./gradlew :fabric:runProductionClient  # launch the real Fabric jar, as players install it
./gradlew :neoforge:runData       # regenerate item models, translations, tags and recipes into common/src/generated/resources
./gradlew build         # build the mod jar -> neoforge/build/libs/better_weaponry-<version>+1.21.1-neoforge.jar
```

If dependencies get out of sync: `./gradlew --refresh-dependencies`, or `./gradlew clean`, then build again.

### Project layout

The project has one Gradle module per mod loader. `common/` holds everything that does not depend on a loader and only talks to it through `platform/Platform.java`, including the JEI and EMI plugins. `neoforge/` holds the entry points, the event bridges, data generation and loot modifiers. `fabric/` holds the entry points and the mixins that replace NeoForge's events. The GameTests are in `common/src/gametest/` and run on each loader.

```
common/src/main/java/com/betterweaponry/
├── BetterWeaponry.java          # Shared core: declares the content, config folder
├── platform/                    # What differs between loaders (Platform, Registered, IncomingDamage)
├── client/                     # 1.8 parry pose, on-screen elements (CombatHud), tooltip layout, attack zones debug view, key bindings
│   └── editor/                 # Editor / Codex screen: document list, field rows built from the FieldSets, model, HUD placement screen
├── command/BetterWeaponryCommand.java  # /betterweaponry (editor, codex, dps, reload)
├── editor/                     # Server side of the editor: documents, save (differences only), targets, client bridge
├── combat/
│   ├── CombatEvents.java        # Armor penetration, sweep on/off, dual-wield invulnerability time
│   ├── ParrySystem.java         # When a right click parries, frontal damage reduction, wear and sound
│   ├── DashAbility.java         # When a right click dashes, impulse (client), cooldown and hunger (server)
│   ├── DualWieldSystem.java     # Rules for dual wielding + cooldown of the hand whose turn it is
│   ├── WeaponProfiles.java      # "Which weapon is this item?": the mod's weapons and the items adopted with "items"
│   ├── AdoptedItemEvents.java   # Right click and attributes of adopted items
│   ├── AbilityCooldowns.java, ParryMaterial.java  # Shared cooldowns, sound and particles of a perfect parry per material
│   ├── ShieldSystem.java        # Perfect parry with a shield, arrows sent back
│   ├── ModEnchantments.java, EnchantmentEvents.java  # Effects of the mod's enchantments
│   ├── HandSwap.java            # Swaps both hands (items + attributes) during an off-hand attack
│   ├── TwoHandedSystem.java     # Rules for when the off hand is locked
│   ├── ComboSystem.java         # Combo progress, special hits (area, both weapons), cooldown after a special hit
│   ├── PlayerCombatState.java   # Per-player state: dual-wield turn, combo, two-arm animation
│   └── ICombatPlayer.java       # Duck interface mixed into Player to hold its PlayerCombatState
├── compat/CombatCompat.java      # Detects Better Combat and Epic Fight (turns off our systems)
├── config/
│   ├── ClientConfig.java        # Display preferences (client TOML), TooltipCategory, TooltipPlacement, HudElement, ComboHudStyle
│   ├── ConfigFolder.java        # config/better_weaponry/: defaults copy, user files, schemas, README
│   ├── Docs.java, ReadmeWriter.java, SchemaWriter.java  # Documentation generated from the field declarations
│   └── LegacyConfigMigration.java  # Converts the 1.x TOML
├── data/
│   ├── WeaponDefinition.java    # Fields of a weapon file
│   ├── OnHitEffect.java, SweepDefinition.java, ParryDefinition.java, DashDefinition.java,
│   │   ComboDefinition.java, ComboStep.java  # Sub-objects of a weapon file
│   ├── WeaponAbility.java, DashDirection.java  # Values of the "ability" and "dash.direction" fields
│   ├── RulesDefinition.java     # Fields of rules.json
│   ├── GearDefinition.java      # Fields of gear.json
│   ├── DataField.java, FieldSet.java  # Field declarations, validation, merging of the layers
│   ├── WeaponDataLoader.java    # Loads the files when a world loads and on /reload
│   ├── WeaponData.java          # Data in effect (loaded by the server, or synced from it)
│   └── WeaponDataEvents.java    # Reload listener, sync after /reload, error messages for operators
├── network/                     # Weapon data (login, /reload), combo correction, two-arm swing, editor (open, save, result)
├── registry/
│   ├── ModItems.java            # Registers one item per WeaponType × active WeaponMaterial
│   ├── ModAttributes.java       # Armor penetration attribute (on every living entity)
│   ├── ModSounds.java           # Parry, dash and combo-ready sounds (files and subtitles in assets/better_weaponry/sounds.json)
│   └── ModTags.java             # better_weaponry:dual_wieldable and better_weaponry:melee_weapons
├── gametest/CombatGameTests.java  # Automated tests (gradlew :neoforge:runGameTestServer)
├── item/
│   ├── WeaponType.java          # The 13 weapon types (+ base model and crafting pattern)
│   ├── WeaponMaterial.java      # One material: Tier, default stats, how it is crafted, required mod
│   ├── WeaponMaterials.java     # List of all materials (vanilla + mod compatibilities)
│   ├── WeaponRecipe.java        # Crafting pattern with M (material) and S (stick)
│   ├── WeaponItem.java          # Single item class; stats read live from the weapon data
│   ├── WeaponShieldItem.java, GearType.java, ModShieldDecorationRecipe.java  # Shields per material
│   ├── WeaponBowItem.java, WeaponCrossbowItem.java, RangedWeapon.java  # Bows and crossbows per material
│   └── VanillaGear.java         # Replaces vanilla gear (loot, inventories)
├── loot/                        # Global loot modifiers (vanilla gear replaced, enchanted books)
├── event/                       # Two-handed restrictions, tooltips, first-person hand hiding
├── mixin/                       # Dual-wield and combo attacks, two-arm swing (first and third person), rendering hooks
└── datagen/                     # Item model, item tag, recipe and Better Combat providers
    └── lang/                    # Builds the lang files from common/src/main/lang/ and checks they are complete

common/src/main/lang/                   # Translation sources, one per language (not packaged, read by runData)

common/src/main/resources/
├── assets/better_weaponry/      # Textures, palettes and templates, hand-written models, sounds.json
├── assets/minecraft/atlases/    # Adds the weapon palette source to the vanilla atlases
├── data/better_weaponry/        # Default weapon stats and rules (better_weaponry/), empty GameTest structure
└── betterweaponry.mixins.json   # Mixin config

common/src/generated/resources/         # Output of runData (do not edit by hand)

fabric/src/main/
├── java/com/betterweaponry/fabric/    # Fabric entry points, network and mixins
└── resources/fabric.mod.json          # Mod metadata (filled in from gradle.properties)

neoforge/src/main/
├── java/com/betterweaponry/neoforge/  # NeoForge entry points and event bridges
└── resources/META-INF/neoforge.mods.toml  # Mod metadata (filled in from gradle.properties)
```

### Adding a new weapon type

1. Add one line to `item/WeaponType.java`, for example `CUTLASS("cutlass", null)`. The second argument is the crafting pattern (`null` = no recipe). The items, models, tags and recipes all come from this enum. Then add its stats file `data/better_weaponry/better_weaponry/weapons/cutlass.json` (copy an existing one).
2. Optional, to set its size and how it is held: add `assets/better_weaponry/models/item/cutlass_base.json`. Without it, the weapon uses vanilla `item/handheld`.
3. Add its template `assets/better_weaponry/textures/templates/item/cutlass.png` (see [Weapon textures](#weapon-textures)). In each translation source `common/src/main/lang/<language>.json`, add the weapon's name in `names.types` (with its grammatical gender for German, Dutch, Portuguese and Russian) and its plural in `strings` (`better_weaponry.configuration.<type>`). The 50 item names per language are built automatically (see [Translations](#translations)).
4. Run `./gradlew :neoforge:runData` to generate the models, translations, tags and recipes.
5. Add the weapon's Better Combat preset in `datagen/BetterCombatAttributesProvider.java` (`runData` fails without one).

### Weapon textures

Weapon textures are built when resources load, from one **template** per weapon and one **palette** per material. All paths are under `assets/better_weaponry/textures/`.

| File | Role |
| --- | --- |
| `palettes/key.png` | The reference colors, one per pixel (7×1, dark to light). |
| `palettes/<material>.png` | The material's colors, in the same order and at the same size as `key.png`. |
| `templates/item/<name>.png` | A weapon painted with the `key.png` colors. Generates `item/<material>_<name>`. |
| `templates/entity/shield/shield.png` | Optional shield template (64×64). |
| `item/<material>_<name>.png` | A hand-drawn texture for this exact weapon. **It always wins over the generated one.** |

- Colors that are not in `key.png` stay as painted, which is how handles keep their color on every material.
- Palettes and templates are read when resources reload: press F3+T in game to see a change, no `runData` needed. A resource pack can replace any of these files.
- A missing or wrongly sized palette does not stop the game. The log lists the materials concerned and their weapons appear in purple and black.

Models follow the same priority, from strongest to weakest:

1. `models/item/<material>_<name>.json`, written by hand for this exact weapon. `runData` stops generating that file.
2. `models/item/<name>_base.json`, shared by every material of the weapon (size, position in hand).
3. Vanilla `item/handheld`.

A base model can be 3D (Blockbench) and still follow the palettes: name its texture variable `layer0` (Blockbench calls it `0` by default) and add `"particle": "#layer0"`.

### Translations

The lang files in `common/src/generated/resources/assets/better_weaponry/lang/` are **generated**: edit the sources in `common/src/main/lang/<language>.json`, then run `./gradlew :neoforge:runData`.

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

Each weapon type has a pattern in `WeaponType.java`. `M` is the weapon's material and `S` is a stick (`c:rods/wooden`). `./gradlew :neoforge:runData` writes one recipe per material: `M` becomes planks for the wooden version, `c:ingots/gold` for the golden one, the material's tag for modded materials, and so on. Netherite and Warden do not use the pattern. They are smithing-table upgrades (diamond → netherite → warden).

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
- Add the material's name to `names.materials` in each translation source `common/src/main/lang/<language>.json` (one entry per language, not 13 item names; see [Translations](#translations)), and its palette `textures/palettes/<material>.png` (see [Weapon textures](#weapon-textures)). A texture that is neither drawn nor generated does not stop `runData`: the model is still generated and the console lists the missing textures.
- Modded weapons are added to `minecraft:swords` as optional entries, so the tag keeps working when the mod is absent.

---

## License

All Rights Reserved. © WoroxIV

Mojang mapping names follow the [Mojang mappings license](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).
