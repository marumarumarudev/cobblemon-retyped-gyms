# Cobbleverse Retyped Gyms

**Version 1.1.0** (decompiled + improved from the abandoned 1.0.0 jar)

Randomizes Kanto, Johto and Hoenn Gym Leaders + Elite Four with type-themed competitive teams every server start.  
Soft-depends on **Cobblemon** + **RCT (Radical Cobblemon Trainers)**.

## Improvements in 1.1.0

- **Editable config** (`config/cobbleverse-retyped-gyms.json`)
  - `bannedLabels` – Cobblemon labels that are banned in gym battles
  - `bannedSpeciesFallback` – species name blacklist
  - `customBannedSpecies` – extra custom bans (e.g. shedinja)
  - `generateDatapackOnStart` / `debugLogging`
- Cleaner project structure with Fabric Loom
- Updated `fabric.mod.json` (suggests Cobblemon + RCT)
- Source is now available for further improvement

## Building

```bash
./gradlew build
```

The jar will appear in `build/libs/`.

> **Note on mappings**  
> The original jar was built against intermediary. This project now uses standard Yarn mappings so Gradle configures cleanly.
> The Java sources still contain intermediary names (`class_XXX`, `method_XXX`) from decompilation.
> Compile will report unresolved symbols until those are replaced with Yarn names
> (or you work only on the pure-Java parts: config, team builder, pools, etc.).  
> For long-term maintenance you should re-map the Minecraft references to Yarn or official Mojmap and clean the names.

## Runtime behaviour

1. On mod init + every server start the mod writes a datapack  
   `zz-cobbleverse-retyped-gyms` / `cobbleverse-retyped-gyms` containing RCT trainer JSONs with the retyped teams.
2. `GymBattleGuard` uses reflection on Cobblemon events to cancel gym battles that contain banned Pokémon.
3. Raid-boss Pokémon (marked in the fixed teams) get special handling via `GymRaidBossManager`.

## Further recommended improvements

- Move the giant hardcoded `*_TEAM` lists out of `RetypedGyms.java` into JSON files under `data/`.
- Make type pools (`TypedPokemonPool`) data-driven.
- Replace intermediary names with Yarn/Mojmap.
- Add a `/retypedgyms reload` command.
- Support a persistent seed so the same randomisation can be shared across servers.

## Authors

| Role | Name |
|------|------|
| Original mod | Antigravity |
| Decompile, restore, improvements & data-driven rewrite | **zmoonmaru** |

## License

MIT – see `LICENSE`.

## Config layout (v1.1 data-driven)

All runtime config lives under:

```
config/cobbleverse-retyped-gyms/
├── settings.json      # bans, datapack on/off, folder name
├── leaders.json       # list of trainers (fileId, name, level, itemCount, team)
├── teams/
│   ├── brock_team.json
│   ├── misty_team.json
│   └── ...            # 39 fixed teams
└── pools/
    ├── steel.json     # pokemon + megas for that type
    ├── poison.json
    └── ...            # 16 types
```

Defaults are shipped inside the jar and copied to the config folder on first run.
Edit any JSON and restart the server — no recompile needed.
