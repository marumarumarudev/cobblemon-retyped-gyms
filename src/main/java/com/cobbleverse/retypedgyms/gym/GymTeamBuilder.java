/*
 * Decompiled with CFR 0.152.
 */
package com.cobbleverse.retypedgyms.gym;

import com.cobbleverse.retypedgyms.gym.GymLeaderDef;
import com.cobbleverse.retypedgyms.gym.TypedPokemonPool;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class GymTeamBuilder {
    private static final Random RNG = new Random();
    private static final List<String> SAFE_HELD_ITEMS = List.of("leftovers", "lum_berry", "sitrus_berry", "rocky_helmet", "focus_sash", "life_orb", "choice_scarf", "choice_band", "choice_specs", "assault_vest", "expert_belt", "air_balloon");
    private static final List<String> TERA_HELD_ITEMS = List.of("life_orb", "focus_sash", "choice_band", "choice_specs", "assault_vest", "leftovers", "expert_belt");

    private GymTeamBuilder() {
    }

    public static String normalizeHeldItem(String item) {
        if (item == null || item.isBlank()) {
            return "";
        }
        return switch (item.toLowerCase().trim()) {
            case "assaultvest" -> "assault_vest";
            case "airballoon" -> "air_balloon";
            case "blacksludge" -> "black_sludge";
            case "booster_energy" -> "mega_showdown:booster_energy";
            case "rusted_sword" -> "mega_showdown:rusted_sword";
            case "rusted_shield" -> "mega_showdown:rusted_shield";
            default -> item;
        };
    }

    public static String buildFixedTrainerJson(GymLeaderDef leader) {
        List<FixedPokemonSpec> team = leader.fixedTeam();
        int level = leader.maxLevel();
        int itemCount = leader.itemCount();
        String name = leader.displayName();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": {\n");
        sb.append("    \"literal\": \"").append(name).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"ai\": {\n");
        sb.append("    \"type\": \"rct\",\n");
        sb.append("    \"data\": {\n");
        sb.append("      \"moveBias\": 1,\n");
        sb.append("      \"switchBias\": 0.5,\n");
        sb.append("      \"statMoveBias\": 1,\n");
        sb.append("      \"itemBias\": 0.8,\n");
        sb.append("      \"maxSelectMargin\": 0.1\n");
        sb.append("    }\n");
        sb.append("  },\n");
        sb.append("  \"battleRules\": {\n");
        sb.append("    \"maxItemUses\": ").append(itemCount).append("\n");
        sb.append("  },\n");
        sb.append("  \"bag\": [\n");
        sb.append("    {\n");
        sb.append("      \"item\": \"cobblemon:full_restore\",\n");
        sb.append("      \"quantity\": ").append(itemCount).append("\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"team\": [\n");
        for (int i = 0; i < team.size(); ++i) {
            FixedPokemonSpec spec = team.get(i);
            boolean isLast = i == team.size() - 1;
            sb.append("    {\n");
            List<String> moves = spec.moves().subList(0, Math.min(4, spec.moves().size()));
            GymTeamBuilder.appendMoveSet(sb, moves);
            GymTeamBuilder.appendIvs(sb);
            if (spec.raidBoss()) {
                sb.append("      \"evs\": {\n");
                sb.append("        \"hp\": 252,\n");
                sb.append("        \"def\": 128,\n");
                sb.append("        \"spd\": 128\n");
                sb.append("      },\n");
            } else {
                boolean isSpecial = "modest".equalsIgnoreCase(spec.nature()) || "timid".equalsIgnoreCase(spec.nature()) || "calm".equalsIgnoreCase(spec.nature()) || "quiet".equalsIgnoreCase(spec.nature()) || "bold".equalsIgnoreCase(spec.nature());
                sb.append("      \"evs\": {\n");
                if (isSpecial) {
                    sb.append("        \"spe\": 252,\n");
                    sb.append("        \"spa\": 252,\n");
                    sb.append("        \"hp\": 4\n");
                } else {
                    sb.append("        \"spe\": 252,\n");
                    sb.append("        \"atk\": 252,\n");
                    sb.append("        \"hp\": 4\n");
                }
                sb.append("      },\n");
            }
            sb.append("      \"heldItem\": [\n");
            sb.append("        \"").append(GymTeamBuilder.normalizeHeldItem(spec.heldItem())).append("\"\n");
            sb.append("      ],\n");
            List<String> aspects = spec.aspects();
            if (spec.raidBoss()) {
                aspects = new ArrayList<String>(aspects);
                aspects.add("raid");
            }
            if (!aspects.isEmpty()) {
                sb.append("      \"aspects\": [\n");
                for (int a = 0; a < aspects.size(); ++a) {
                    sb.append("        \"").append(aspects.get(a)).append("\"");
                    if (a < aspects.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append("      ],\n");
            }
            if (spec.shiny() || spec.raidBoss()) {
                sb.append("      \"shiny\": true,\n");
            }
            sb.append("      \"species\": \"").append(spec.species()).append("\",\n");
            sb.append("      \"gender\": \"").append(spec.gender()).append("\",\n");
            int effectiveLevel = level;
            sb.append("      \"level\": ").append(effectiveLevel).append(",\n");
            sb.append("      \"nature\": \"").append(spec.nature()).append("\",\n");
            sb.append("      \"ability\": \"").append(spec.ability()).append("\"");
            if (spec.mega()) {
                sb.append(",\n      \"mega\": true\n");
            } else {
                sb.append("\n");
            }
            sb.append("    }");
            if (!isLast) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"battleFormat\": \"GEN_9_SINGLES\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static String buildTrainerJson(GymLeaderDef leader) {
        List<TypedPokemonPool.PokemonEntry> pool = TypedPokemonPool.getShuffledPool(leader.type());
        if (pool.isEmpty()) {
            throw new IllegalStateException("No Pok\u00e9mon pool found for type: " + leader.type());
        }
        int teamSize = leader.teamSize();
        List<TypedPokemonPool.MegaEntry> megaOptions = TypedPokemonPool.getMegaOptions(leader.type());
        boolean isAceMega = leader.signatureMega() != null || !megaOptions.isEmpty() && RNG.nextBoolean();
        TypedPokemonPool.PokemonEntry acePokemon = null;
        TypedPokemonPool.MegaEntry aceMega = null;
        ArrayList<TypedPokemonPool.PokemonEntry> team = new ArrayList<TypedPokemonPool.PokemonEntry>();
        if (isAceMega) {
            if (leader.signatureMega() != null) {
                for (TypedPokemonPool.MegaEntry megaEntry : megaOptions) {
                    if (!megaEntry.species().equalsIgnoreCase(leader.signatureMega())) continue;
                    aceMega = megaEntry;
                    break;
                }
            }
            if (aceMega == null && !megaOptions.isEmpty()) {
                aceMega = megaOptions.get(RNG.nextInt(megaOptions.size()));
            }
            if (aceMega != null) {
                String aceSpecies = aceMega.species();
                for (TypedPokemonPool.PokemonEntry p : pool) {
                    if (!p.species().equalsIgnoreCase(aceSpecies)) continue;
                    acePokemon = p;
                    break;
                }
            }
            if (acePokemon == null) {
                acePokemon = pool.get(0);
                isAceMega = false;
            }
        }
        int neededNonAce = teamSize - 1;
        for (TypedPokemonPool.PokemonEntry p : pool) {
            if (team.size() >= neededNonAce) break;
            if (acePokemon != null && p.species().equalsIgnoreCase(acePokemon.species())) continue;
            team.add(p);
        }
        for (TypedPokemonPool.PokemonEntry p : pool) {
            if (team.size() >= neededNonAce) break;
            team.add(p);
        }
        if (!isAceMega) {
            for (TypedPokemonPool.PokemonEntry p : pool) {
                if (team.contains(p)) continue;
                acePokemon = p;
                break;
            }
            if (acePokemon == null) {
                acePokemon = pool.get(RNG.nextInt(pool.size()));
            }
        }
        team.add(acePokemon);
        int[] nArray = GymTeamBuilder.spreadLevels(leader.minLevel(), leader.maxLevel(), team.size());
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": {\n");
        sb.append("    \"literal\": \"").append(leader.displayName()).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"ai\": {\n");
        sb.append("    \"type\": \"rct\",\n");
        sb.append("    \"data\": {\n");
        sb.append("      \"moveBias\": 1,\n");
        sb.append("      \"switchBias\": 0.5,\n");
        sb.append("      \"statMoveBias\": 1,\n");
        sb.append("      \"itemBias\": 0.8,\n");
        sb.append("      \"maxSelectMargin\": 0.15\n");
        sb.append("    }\n");
        sb.append("  },\n");
        sb.append("  \"battleRules\": {\n");
        sb.append("    \"maxItemUses\": ").append(leader.itemCount()).append("\n");
        sb.append("  },\n");
        sb.append("  \"bag\": [\n");
        sb.append("    {\n");
        sb.append("      \"item\": \"cobblemon:full_restore\",\n");
        sb.append("      \"quantity\": ").append(leader.itemCount()).append("\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"team\": [\n");
        for (int i = 0; i < team.size(); ++i) {
            String rawSpecies;
            TypedPokemonPool.PokemonEntry entry = (TypedPokemonPool.PokemonEntry)team.get(i);
            List<String> chosenMoves = GymTeamBuilder.pickMoves(entry.movePool(), 4);
            boolean isLast = i == team.size() - 1;
            sb.append("    {\n");
            GymTeamBuilder.appendMoveSet(sb, chosenMoves);
            GymTeamBuilder.appendIvs(sb);
            GymTeamBuilder.appendEvs(sb, entry);
            if (isLast) {
                if (isAceMega && aceMega != null) {
                    sb.append("      \"heldItem\": [\n");
                    sb.append("        \"").append(aceMega.megaStone()).append("\"\n");
                    sb.append("      ],\n");
                } else {
                    String teraItem = TERA_HELD_ITEMS.get(RNG.nextInt(TERA_HELD_ITEMS.size()));
                    sb.append("      \"heldItem\": [\n");
                    sb.append("        \"").append(teraItem).append("\"\n");
                    sb.append("      ],\n");
                    sb.append("      \"gimmicks\": {\n");
                    sb.append("        \"tera\": \"").append(leader.type()).append("\"\n");
                    sb.append("      },\n");
                }
            } else {
                String heldItem = SAFE_HELD_ITEMS.get(RNG.nextInt(SAFE_HELD_ITEMS.size()));
                sb.append("      \"heldItem\": [\n");
                sb.append("        \"").append(heldItem).append("\"\n");
                sb.append("      ],\n");
            }
            String speciesName = rawSpecies = entry.species();
            String aspect = null;
            if (rawSpecies.contains(" ")) {
                String[] parts = rawSpecies.split(" ", 2);
                speciesName = parts[0];
                aspect = parts[1];
            }
            if (aspect != null) {
                sb.append("      \"aspects\": [\n");
                sb.append("        \"").append(aspect).append("\"\n");
                sb.append("      ],\n");
            }
            sb.append("      \"species\": \"").append(speciesName).append("\",\n");
            sb.append("      \"gender\": \"").append(entry.gender()).append("\",\n");
            sb.append("      \"level\": ").append(nArray[i]).append(",\n");
            sb.append("      \"nature\": \"").append(entry.nature()).append("\",\n");
            sb.append("      \"ability\": \"").append(entry.ability()).append("\"");
            if (isLast && isAceMega) {
                sb.append(",\n      \"mega\": true\n");
            } else {
                sb.append("\n");
            }
            sb.append("    }");
            if (!isLast) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"battleFormat\": \"GEN_9_SINGLES\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static List<String> pickMoves(List<String> pool, int count) {
        ArrayList<String> shuffled = new ArrayList<String>(pool);
        Collections.shuffle(shuffled, RNG);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private static int[] spreadLevels(int minLvl, int maxLvl, int count) {
        int[] levels = new int[count];
        if (count == 1) {
            levels[0] = maxLvl;
            return levels;
        }
        for (int i = 0; i < count; ++i) {
            levels[i] = minLvl + Math.round((float)(maxLvl - minLvl) * (float)i / (float)(count - 1));
        }
        return levels;
    }

    private static void appendIvs(StringBuilder sb) {
        sb.append("      \"ivs\": {\n");
        sb.append("        \"spe\": 31,\n");
        sb.append("        \"spd\": 31,\n");
        sb.append("        \"spa\": 31,\n");
        sb.append("        \"def\": 31,\n");
        sb.append("        \"atk\": 31,\n");
        sb.append("        \"hp\": 31\n");
        sb.append("      },\n");
    }

    private static void appendEvs(StringBuilder sb, TypedPokemonPool.PokemonEntry entry) {
        boolean isSpecial = "modest".equalsIgnoreCase(entry.nature()) || "timid".equalsIgnoreCase(entry.nature()) || "calm".equalsIgnoreCase(entry.nature()) || "quiet".equalsIgnoreCase(entry.nature());
        sb.append("      \"evs\": {\n");
        if (isSpecial) {
            sb.append("        \"spe\": 252,\n");
            sb.append("        \"spa\": 252,\n");
            sb.append("        \"hp\": 4\n");
        } else {
            sb.append("        \"spe\": 252,\n");
            sb.append("        \"atk\": 252,\n");
            sb.append("        \"hp\": 4\n");
        }
        sb.append("      },\n");
    }

    private static void appendMoveSet(StringBuilder sb, List<String> moves) {
        sb.append("      \"moveset\": [\n");
        for (int m = 0; m < moves.size(); ++m) {
            sb.append("        \"").append(moves.get(m)).append("\"");
            if (m < moves.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("      ],\n");
    }

    public static void main(String[] args) {
        Path targetDir = args.length > 0 ? Path.of(args[0], new String[0]) : Path.of("cobble-gymleaders/src/main/resources", new String[0]);
        Path trainersDir = targetDir.resolve("data").resolve("rctmod").resolve("trainers");
        try {
            Files.createDirectories(trainersDir, new FileAttribute[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        List<GymLeaderDef> leaders = List.of(new GymLeaderDef("kanto_brock", "Brock", "steel", 6, 20, 36, 2), new GymLeaderDef("kanto_misty", "Misty", "poison", 6, 36, 44, 2), new GymLeaderDef("kanto_ltsurge", "Lt. Surge", "ground", 6, 44, 52, 3), new GymLeaderDef("kanto_erika", "Erika", "fairy", 6, 52, 60, 3), new GymLeaderDef("kanto_koga", "Koga", "flying", 6, 60, 68, 3), new GymLeaderDef("kanto_sabrina", "Sabrina", "normal", 6, 68, 75, 4), new GymLeaderDef("kanto_blaine", "Blaine", "psychic", 6, 75, 82, 4), new GymLeaderDef("kanto_giovanni", "Giovanni", "dark", 6, 82, 88, 4), new GymLeaderDef("team_rocket_giovanni", "Giovanni", "dark", 6, 85, 90, 4));
        for (GymLeaderDef leader : leaders) {
            try {
                String json = leader.fixedTeam() != null ? GymTeamBuilder.buildFixedTrainerJson(leader) : GymTeamBuilder.buildTrainerJson(leader);
                Files.writeString(trainersDir.resolve(leader.fileId() + ".json"), (CharSequence)json, new OpenOption[0]);
                System.out.println("Generated: " + leader.displayName() + " (" + leader.fileId() + ") - " + leader.type() + " [Lv." + leader.minLevel() + "-" + leader.maxLevel() + "]");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public record FixedPokemonSpec(String species, String gender, String nature, String ability, List<String> moves, String heldItem, List<String> aspects, boolean shiny, boolean mega, boolean raidBoss) {
        public FixedPokemonSpec(String species, String gender, String nature, String ability, List<String> moves, String heldItem) {
            this(species, gender, nature, ability, moves, heldItem, List.of(), false, false, false);
        }

        public FixedPokemonSpec(String species, String gender, String nature, String ability, List<String> moves, String heldItem, List<String> aspects) {
            this(species, gender, nature, ability, moves, heldItem, aspects, false, false, false);
        }

        public FixedPokemonSpec(String species, String gender, String nature, String ability, List<String> moves, String heldItem, List<String> aspects, boolean shiny, boolean mega) {
            this(species, gender, nature, ability, moves, heldItem, aspects, shiny, mega, false);
        }

        public static FixedPokemonSpec raidBoss(String species, String gender, String nature, String ability, List<String> moves, String heldItem) {
            return new FixedPokemonSpec(species, gender, nature, ability, moves, heldItem, List.of(), true, false, true);
        }
    }
}

