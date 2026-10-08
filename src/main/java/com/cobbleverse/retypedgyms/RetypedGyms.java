package com.cobbleverse.retypedgyms;

import com.cobbleverse.retypedgyms.config.ModConfig;
import com.cobbleverse.retypedgyms.gym.GymLeaderDef;
import com.cobbleverse.retypedgyms.gym.GymTeamBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RetypedGyms implements ModInitializer {
    public static final String MOD_ID = "cobbleverse-retyped-gyms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[RetypedGyms] Initializing Cobbleverse Retyped Gyms mod v1.1.0 (data-driven)...");
        ModConfig.get(); // load config early
        GymBattleGuard.register();
        GymRaidBossManager.register();

        // Write datapack once, at server start — prefer global datapacks/ if it exists
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            GymBattleGuard.register();
            ModConfig cfg = ModConfig.get();
            if (!cfg.generateDatapackOnStart) {
                LOGGER.info("[RetypedGyms] generateDatapackOnStart=false — skipping datapack write.");
                return;
            }

            Path target = resolveSingleDatapackRoot(server, cfg);
            if (target != null) {
                writeDatapack(target);
            } else {
                LOGGER.warn("[RetypedGyms] Could not resolve a datapack directory — teams not written.");
            }
        });
    }

    /**
     * Prefer global gameDir/datapacks when it exists; otherwise use the world's datapacks folder.
     * Only one location is ever written.
     */
    private static Path resolveSingleDatapackRoot(MinecraftServer server, ModConfig cfg) {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path globalDatapacks = gameDir.resolve("datapacks");

        if (Files.isDirectory(globalDatapacks)) {
            Path dest = globalDatapacks.resolve(cfg.datapackFolderName);
            LOGGER.info("[RetypedGyms] Using global datapacks folder: {}", dest);
            return dest;
        }

        Path worldDir = getWorldDatapackDir(server);
        if (worldDir != null) {
            Path dest = worldDir.resolve(cfg.datapackFolderName);
            LOGGER.info("[RetypedGyms] No global datapacks/ — using world folder: {}", dest);
            return dest;
        }
        return null;
    }

    public static void writeDatapack(Path datapackRoot) {
        try {
            List<GymLeaderDef> leaders = ModConfig.get().leaders;
            if (leaders == null || leaders.isEmpty()) {
                LOGGER.warn("[RetypedGyms] Skipping datapack write to {} — 0 leaders loaded (config error?). Existing trainers left untouched.",
                        datapackRoot);
                return;
            }

            Path trainersDir = datapackRoot.resolve("data/rctmod/trainers");
            Files.createDirectories(trainersDir);

            Path meta = datapackRoot.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                Files.writeString(meta, """
                    {
                      "pack": {
                        "pack_format": 48,
                        "description": "Cobbleverse Retyped Gyms generated trainers"
                      }
                    }
                    """);
            }

            int success = 0;
            for (GymLeaderDef leader : leaders) {
                try {
                    String json = leader.fixedTeam() != null && !leader.fixedTeam().isEmpty()
                            ? GymTeamBuilder.buildFixedTrainerJson(leader)
                            : GymTeamBuilder.buildTrainerJson(leader);
                    Files.writeString(trainersDir.resolve(leader.fileId() + ".json"), json);
                    LOGGER.info("[RetypedGyms] ✓ {} ({}) — {} type, {} Pokémon (Lv.{}-{})",
                            leader.displayName(), leader.fileId(), leader.type(),
                            leader.teamSize(), leader.minLevel(), leader.maxLevel());
                    success++;
                } catch (Exception e) {
                    LOGGER.warn("[RetypedGyms] Failed to write {}: {}", leader.fileId(), e.getMessage());
                }
            }
            LOGGER.info("[RetypedGyms] Done. Generated {}/{} gym leader teams into {}",
                    success, leaders.size(), datapackRoot);
        } catch (Exception e) {
            LOGGER.error("[RetypedGyms] writeDatapack failed", e);
        }
    }

    public static Path getWorldDatapackDir(MinecraftServer server) {
        try {
            Path datapacks = server.getSavePath(WorldSavePath.DATAPACKS);
            LOGGER.info("[RetypedGyms] Resolved world datapack path: {}", datapacks);
            return datapacks;
        } catch (Exception e) {
            LOGGER.warn("[RetypedGyms] getSavePath failed: {}", e.getMessage());
        }
        try {
            Path runDir = FabricLoader.getInstance().getGameDir();
            for (String candidate : List.of("world/datapacks", "saves/world/datapacks")) {
                Path p = runDir.resolve(candidate);
                if (Files.exists(p.getParent())) {
                    LOGGER.info("[RetypedGyms] Using fallback path: {}", p);
                    return p;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
