package com.cobbleverse.retypedgyms.config;

import com.cobbleverse.retypedgyms.RetypedGyms;
import com.cobbleverse.retypedgyms.gym.GymLeaderDef;
import com.cobbleverse.retypedgyms.gym.GymTeamBuilder;
import com.cobbleverse.retypedgyms.gym.TypedPokemonPool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads all config from config/cobbleverse-retyped-gyms/
 *
 *   settings.json   – bans, datapack options
 *   leaders.json    – which trainers exist + level/items/team ref
 *   teams/*.json    – fixed Pokémon teams
 *   pools/*.json    – type pools + mega options
 *
 * Each file is loaded independently so one bad JSON cannot wipe everything.
 */
public final class ModConfig {
    public static final String CONFIG_DIR_NAME = "cobbleverse-retyped-gyms";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);

    public boolean generateDatapackOnStart = true;
    public boolean debugLogging = false;
    public String datapackFolderName = "zz-cobbleverse-retyped-gyms";
    public Set<String> bannedLabels = new LinkedHashSet<>();
    public Set<String> bannedSpeciesFallback = new LinkedHashSet<>();
    public Set<String> customBannedSpecies = new LinkedHashSet<>();

    public List<GymLeaderDef> leaders = new ArrayList<>();
    public Map<String, List<GymTeamBuilder.FixedPokemonSpec>> teams = new LinkedHashMap<>();
    public Map<String, List<TypedPokemonPool.PokemonEntry>> pools = new LinkedHashMap<>();
    public Map<String, List<TypedPokemonPool.MegaEntry>> megas = new LinkedHashMap<>();

    private static ModConfig INSTANCE;

    public static ModConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    /** Force re-read from disk (e.g. after /reload). */
    public static ModConfig reload() {
        INSTANCE = load();
        return INSTANCE;
    }

    public static ModConfig load() {
        ensureDefaultsCopiedQuiet();
        ModConfig cfg = new ModConfig();

        try { cfg.loadSettings(); }
        catch (Exception e) {
            RetypedGyms.LOGGER.error("[RetypedGyms] settings.json failed to load — bans/datapack flags unavailable. Fix JSON syntax (no trailing commas!).", e);
        }
        try { cfg.loadTeams(); }
        catch (Exception e) {
            RetypedGyms.LOGGER.error("[RetypedGyms] teams/ folder failed to load", e);
        }
        try { cfg.loadLeaders(); }
        catch (Exception e) {
            RetypedGyms.LOGGER.error("[RetypedGyms] leaders.json failed to load", e);
        }
        try { cfg.loadPools(); }
        catch (Exception e) {
            RetypedGyms.LOGGER.error("[RetypedGyms] pools/ folder failed to load", e);
        }

        RetypedGyms.LOGGER.info("[RetypedGyms] Config loaded from {} ({} leaders, {} teams, {} pools)",
                CONFIG_ROOT, cfg.leaders.size(), cfg.teams.size(), cfg.pools.size());
        RetypedGyms.LOGGER.info("[RetypedGyms] Ban lists: {} labels, {} fallback species, {} custom ({})",
                cfg.bannedLabels.size(), cfg.bannedSpeciesFallback.size(),
                cfg.customBannedSpecies.size(), cfg.customBannedSpecies);

        if (cfg.leaders.isEmpty()) {
            RetypedGyms.LOGGER.warn("[RetypedGyms] 0 leaders loaded — datapack will NOT be overwritten. Check leaders.json + teams/ for JSON errors.");
        }
        return cfg;
    }

    private static void ensureDefaultsCopiedQuiet() {
        try {
            ensureDefaultsCopied();
        } catch (Exception e) {
            RetypedGyms.LOGGER.warn("[RetypedGyms] Could not copy default config: {}", e.getMessage());
        }
    }

    private static void ensureDefaultsCopied() throws IOException {
        Files.createDirectories(CONFIG_ROOT);
        Path settings = CONFIG_ROOT.resolve("settings.json");
        if (!Files.exists(settings)) {
            copyResource("config/cobbleverse-retyped-gyms/settings.json", settings);
        }
        Path leaders = CONFIG_ROOT.resolve("leaders.json");
        if (!Files.exists(leaders)) {
            copyResource("config/cobbleverse-retyped-gyms/leaders.json", leaders);
        }
        copyResourceDir("config/cobbleverse-retyped-gyms/teams", CONFIG_ROOT.resolve("teams"));
        copyResourceDir("config/cobbleverse-retyped-gyms/pools", CONFIG_ROOT.resolve("pools"));
    }

    private static void copyResource(String resourcePath, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        try (InputStream in = ModConfig.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                RetypedGyms.LOGGER.warn("[RetypedGyms] Missing bundled resource: {}", resourcePath);
                return;
            }
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyResourceDir(String resourceDir, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        FabricLoader.getInstance().getModContainer("cobbleverse-retyped-gyms").ifPresent(mod -> {
            try {
                var opt = mod.findPath(resourceDir);
                if (opt.isEmpty()) return;
                Path srcRoot = opt.get();
                Files.walkFileTree(srcRoot, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path rel = srcRoot.relativize(file);
                        Path target = destDir.resolve(rel.toString());
                        if (!Files.exists(target)) {
                            Files.createDirectories(target.getParent());
                            Files.copy(file, target);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                RetypedGyms.LOGGER.warn("[RetypedGyms] Could not copy resource dir {}: {}", resourceDir, e.getMessage());
            }
        });
    }

    /** Strip trailing commas before } or ] so slightly-invalid editor JSON still parses. */
    private static String sanitizeJson(String raw) {
        // remove trailing commas: ,\s*}  or  ,\s*]
        return raw.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
    }

    private static JsonElement parseFile(Path p) throws IOException {
        String raw = Files.readString(p);
        String cleaned = sanitizeJson(raw);
        try {
            return JsonParser.parseString(cleaned);
        } catch (JsonSyntaxException e) {
            RetypedGyms.LOGGER.error("[RetypedGyms] Invalid JSON in {}: {}", p.getFileName(), e.getMessage());
            throw e;
        }
    }

    private void loadSettings() throws IOException {
        Path p = CONFIG_ROOT.resolve("settings.json");
        if (!Files.exists(p)) {
            RetypedGyms.LOGGER.warn("[RetypedGyms] settings.json missing at {}", p);
            return;
        }
        JsonObject o = parseFile(p).getAsJsonObject();
        if (o.has("generateDatapackOnStart")) generateDatapackOnStart = o.get("generateDatapackOnStart").getAsBoolean();
        if (o.has("debugLogging")) debugLogging = o.get("debugLogging").getAsBoolean();
        if (o.has("datapackFolderName")) datapackFolderName = o.get("datapackFolderName").getAsString();
        bannedLabels = readStringSet(o, "bannedLabels");
        bannedSpeciesFallback = readStringSet(o, "bannedSpeciesFallback");
        customBannedSpecies = readStringSet(o, "customBannedSpecies");
    }

    private static Set<String> readStringSet(JsonObject o, String key) {
        Set<String> set = new LinkedHashSet<>();
        if (!o.has(key) || !o.get(key).isJsonArray()) return set;
        for (JsonElement e : o.getAsJsonArray(key)) {
            if (e.isJsonPrimitive()) set.add(e.getAsString().trim().toLowerCase());
        }
        return set;
    }

    private void loadTeams() throws IOException {
        Path dir = CONFIG_ROOT.resolve("teams");
        if (!Files.isDirectory(dir)) {
            RetypedGyms.LOGGER.warn("[RetypedGyms] teams/ missing at {}", dir);
            return;
        }
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        try (var stream = Files.list(dir)) {
            for (Path file : (Iterable<Path>) stream.filter(f -> f.toString().endsWith(".json"))::iterator) {
                String ref = file.getFileName().toString().replace(".json", "");
                try {
                    String raw = sanitizeJson(Files.readString(file));
                    List<Map<String, Object>> parsed = GSON.fromJson(raw, listType);
                    List<GymTeamBuilder.FixedPokemonSpec> specs = new ArrayList<>();
                    if (parsed != null) {
                        for (Map<String, Object> m : parsed) {
                            specs.add(mapToSpec(m));
                        }
                    }
                    teams.put(ref, specs);
                } catch (Exception e) {
                    RetypedGyms.LOGGER.error("[RetypedGyms] Skipping bad team file {}: {}", file.getFileName(), e.getMessage());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static GymTeamBuilder.FixedPokemonSpec mapToSpec(Map<String, Object> m) {
        String species = str(m, "species");
        String gender = str(m, "gender");
        String nature = str(m, "nature");
        String ability = str(m, "ability");
        List<String> moves = (List<String>) m.getOrDefault("moves", List.of());
        String held = str(m, "heldItem");
        List<String> aspects = (List<String>) m.getOrDefault("aspects", List.of());
        boolean shiny = bool(m, "shiny");
        boolean mega = bool(m, "mega");
        boolean raid = bool(m, "raidBoss");
        if (raid) {
            return GymTeamBuilder.FixedPokemonSpec.raidBoss(species, gender, nature, ability, moves, held);
        }
        return new GymTeamBuilder.FixedPokemonSpec(species, gender, nature, ability, moves, held, aspects, shiny, mega);
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }

    private static boolean bool(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Boolean b && b;
    }

    private void loadLeaders() throws IOException {
        Path p = CONFIG_ROOT.resolve("leaders.json");
        if (!Files.exists(p)) {
            RetypedGyms.LOGGER.warn("[RetypedGyms] leaders.json missing at {}", p);
            return;
        }
        JsonArray arr = parseFile(p).getAsJsonArray();
        for (JsonElement el : arr) {
            try {
                JsonObject o = el.getAsJsonObject();
                String fileId = o.get("fileId").getAsString();
                String displayName = o.get("displayName").getAsString();
                int level = o.get("level").getAsInt();
                int itemCount = o.get("itemCount").getAsInt();
                String teamRef = o.get("team").getAsString();
                List<GymTeamBuilder.FixedPokemonSpec> team = teams.getOrDefault(teamRef, List.of());
                if (team.isEmpty()) {
                    RetypedGyms.LOGGER.warn("[RetypedGyms] Leader {} references missing team '{}'", fileId, teamRef);
                }
                leaders.add(new GymLeaderDef(fileId, displayName, "fixed", Math.max(team.size(), 1), level, level, itemCount, null, team));
            } catch (Exception e) {
                RetypedGyms.LOGGER.error("[RetypedGyms] Skipping bad leader entry: {}", e.getMessage());
            }
        }
    }

    private void loadPools() throws IOException {
        Path dir = CONFIG_ROOT.resolve("pools");
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.list(dir)) {
            for (Path file : (Iterable<Path>) stream.filter(f -> f.toString().endsWith(".json"))::iterator) {
                try {
                    JsonObject o = JsonParser.parseString(sanitizeJson(Files.readString(file))).getAsJsonObject();
                    String type = o.has("type") ? o.get("type").getAsString() : file.getFileName().toString().replace(".json", "");
                    List<TypedPokemonPool.PokemonEntry> entries = new ArrayList<>();
                    if (o.has("pokemon")) {
                        for (JsonElement e : o.getAsJsonArray("pokemon")) {
                            JsonObject poke = e.getAsJsonObject();
                            List<String> moves = new ArrayList<>();
                            if (poke.has("movePool")) {
                                for (JsonElement m : poke.getAsJsonArray("movePool")) moves.add(m.getAsString());
                            }
                            entries.add(new TypedPokemonPool.PokemonEntry(
                                    poke.get("species").getAsString(),
                                    poke.get("gender").getAsString(),
                                    poke.get("nature").getAsString(),
                                    poke.get("ability").getAsString(),
                                    moves
                            ));
                        }
                    }
                    List<TypedPokemonPool.MegaEntry> megaList = new ArrayList<>();
                    if (o.has("megas")) {
                        for (JsonElement e : o.getAsJsonArray("megas")) {
                            JsonObject mg = e.getAsJsonObject();
                            megaList.add(new TypedPokemonPool.MegaEntry(
                                    mg.get("species").getAsString(),
                                    mg.get("megaStone").getAsString()
                            ));
                        }
                    }
                    pools.put(type.toLowerCase(), entries);
                    megas.put(type.toLowerCase(), megaList);
                } catch (Exception e) {
                    RetypedGyms.LOGGER.error("[RetypedGyms] Skipping bad pool file {}: {}", file.getFileName(), e.getMessage());
                }
            }
        }
    }

    public Path getConfigRoot() {
        return CONFIG_ROOT;
    }
}
