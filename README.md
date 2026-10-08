# Cobbleverse Retyped Gyms

**Version 1.2.0** — data-driven gym teams, configurable bans, resilient config loading

Type-themed competitive teams for Kanto, Johto, and Hoenn Gym Leaders + Elite Four. Soft-depends on **Cobblemon** and **RCT (Radical Cobblemon Trainers)**.

---

## Authors

| Role | Name |
|------|------|
| Original mod | Antigravity |
| Decompile, restore, data-driven rewrite & 1.2.0 improvements | **zmoonmaru** |

License: MIT

---

## What's new in 1.2.0

- **Single datapack write** — if a global `datapacks/` folder exists, trainers are written only there (`datapackFolderName`); otherwise only the world datapacks folder. No more duplicate packs.
- **Resilient config loading** — one broken team/settings JSON no longer zeros the entire config. Bad files are skipped and logged; the rest still load.
- **Trailing-comma tolerance** — minor JSON editor mistakes (`,}` / `,]`) are stripped before parse.
- **Empty-pack guard** — if 0 leaders load, the mod **does not** overwrite an existing good datapack.
- **Stronger ban matching** — custom bans match bare names, namespaced ids (`mod:species`), and partial suffixes (e.g. `bloodsplitter`, `faismythicalmonstrosities:bloodsplitter`).
- Clearer startup logs (leader/team/pool counts + ban list summary).

### Still from 1.1.0

- Fully data-driven teams, pools, leaders, and bans (JSON, no recompile)
- Nested config folder under `config/cobbleverse-retyped-gyms/`
- Fabric Loom + Yarn mappings

---

## Config layout

On first run the mod copies defaults from the jar into:

```
config/cobbleverse-retyped-gyms/
├── settings.json       # bans, datapack toggle, folder name
├── leaders.json        # all trainers (id, name, level, items, team ref)
├── teams/
│   ├── brock_team.json
│   ├── misty_team.json
│   └── …               # 39 fixed teams
└── pools/
    ├── steel.json      # pokemon pool + mega options
    ├── poison.json
    └── …               # 16 types
```

Edit any of these → **restart the server** → changes apply. No rebuild needed.

> **Tip:** Keep JSON valid (no trailing commas if you can). Invalid files are logged as `Skipping bad team file …` / `Invalid JSON in settings.json`. Use a validator if bans or teams stop applying.

### `settings.json` keys

| Key | Default | Purpose |
|-----|---------|---------|
| `generateDatapackOnStart` | `true` | Write RCT trainer datapack on server start |
| `datapackFolderName` | `zz-cobbleverse-retyped-gyms` | Datapack folder name (global or world) |
| `debugLogging` | `false` | Extra log output |
| `bannedLabels` | legendary, mythical, … | Cobblemon labels banned in **gym / E4** battles |
| `bannedSpeciesFallback` | mewtwo, … | Species name blacklist |
| `customBannedSpecies` | shade, shedinja, annihilape | Extra custom bans (fakemon ids work here) |

Example custom bans:

```json
"customBannedSpecies": [
  "shade",
  "shedinja",
  "annihilape",
  "bloodsplitter"
]
```

Bans only apply in gym / Elite Four battles (not every wild or casual trainer fight).

### Team JSON (fixed teams)

```json
{
  "species": "lopunny",
  "gender": "MALE",
  "nature": "jolly",
  "ability": "limber",
  "moves": ["fakeout", "return", "highjumpkick", "icepunch"],
  "heldItem": "life_orb",
  "aspects": ["centonian"],
  "shiny": false,
  "mega": false,
  "raidBoss": false
}
```

- **species** — base species id only (`lopunny`), not `lopunny centonian`
- **aspects** — form / fakemon aspect ids (e.g. `["centonian"]`, `["hisuian"]`)
- **raidBoss** — special handling via `GymRaidBossManager`

### `leaders.json`

Each entry points at a team file by name (without `.json`):

```json
{
  "fileId": "kanto_brock",
  "displayName": "Brock",
  "level": 36,
  "itemCount": 2,
  "team": "brock_team"
}
```

---

## Datapack generation

On server start (if `generateDatapackOnStart` is true):

1. Prefer **global** `datapacks/<datapackFolderName>/` when that folder exists  
2. Otherwise write to the **world** datapacks folder under the same name  

Only **one** location is written. If leaders failed to load (0), write is skipped so an existing pack is not wiped.

Remove old duplicates if you still have them (e.g. world `cobbleverse-retyped-gyms` plus global `zz-cobbleverse-retyped-gyms`, or legacy `!zzRetypedGyms.zip`).

---

## Building

```bash
./gradlew build
```

Output: `build/libs/cobbleverse-retyped-gyms-1.2.0.jar`

Requires **Java 21**, Minecraft **1.21.1**, Fabric Loader ≥ 0.16, Fabric API. Soft-depends on Cobblemon + RCT.

---

## Runtime behaviour

1. Config folder is created on first run (defaults copied from the jar; existing files are not overwritten).
2. On server start, one RCT trainer datapack is written for every leader in `leaders.json`.
3. `GymBattleGuard` subscribes to Cobblemon battle events and cancels gym/E4 fights that include banned species or labels.
4. Raid-boss entries in team JSON are handled by `GymRaidBossManager`.

Healthy startup looks like:

```
[RetypedGyms] Config loaded from ... (70 leaders, 39 teams, 16 pools)
[RetypedGyms] Ban lists: 5 labels, … 4 custom ([shade, shedinja, annihilape, bloodsplitter])
[GymBattleGuard] Successfully registered! (BATTLE_STARTED_PRE=ACTIVE, …)
[RetypedGyms] Using global datapacks folder: .../zz-cobbleverse-retyped-gyms
[RetypedGyms] Done. Generated 70/70 gym leader teams into ...
```

If you see `Failed to load config, using empty defaults` or `Generated 0/0`, fix JSON under `config/cobbleverse-retyped-gyms/` (often a broken `teams/*.json`).

---

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| Bans do nothing | Config failed to load (empty ban lists) — check log for JSON errors |
| `Generated 0/0` | `leaders.json` / teams failed to parse |
| Still seeing Volcarona / old teams | Competing old datapack still enabled — remove duplicates |
| Fakemon form wrong | Use `"species": "base"` + `"aspects": ["form"]`, not a combined name |
| Edits ignored | Server not restarted, or editing a file the server isn’t reading |

---


## License

MIT — see `LICENSE`.

Original work © Antigravity.  
1.1.0–1.2.0 restore & improvements © zmoonmaru.
