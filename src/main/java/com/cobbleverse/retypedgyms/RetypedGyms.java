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
        ModConfig cfg = ModConfig.get();
        GymBattleGuard.register();
        GymRaidBossManager.register();

        if (cfg.generateDatapackOnStart) {
            try {
                Path gameDir = FabricLoader.getInstance().getGameDir();
                Path globalDatapacksDir = gameDir.resolve("datapacks");
                if (Files.exists(globalDatapacksDir)) {
                    LOGGER.info("[RetypedGyms] Found global datapacks directory: {}", globalDatapacksDir);
                    writeDatapack(globalDatapacksDir.resolve(cfg.datapackFolderName));
                }
            } catch (Exception e) {
                LOGGER.warn("[RetypedGyms] Could not write global datapack: {}", e.getMessage());
            }
        }

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            GymBattleGuard.register();
            if (!ModConfig.get().generateDatapackOnStart) return;
            LOGGER.info("[RetypedGyms] Server starting — ensuring world datapack has latest teams...");
            Path worldDatapackDir = getWorldDatapackDir(server);
            if (worldDatapackDir != null) {
                writeDatapack(worldDatapackDir.resolve(MOD_ID));
            }
        });
    }

    public static void writeDatapack(Path datapackRoot) {
        try {
            Path trainersDir = datapackRoot.resolve("data/rctmod/trainers");
            Files.createDirectories(trainersDir);

            // pack.mcmeta
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

            List<GymLeaderDef> leaders = ModConfig.get().leaders;
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
            LOGGER.info("[RetypedGyms] Resolved datapack path via LevelResource: {}", datapacks);
            return datapacks;
        } catch (Exception e) {
            LOGGER.warn("[RetypedGyms] getSavePath failed: {}", e.getMessage());
        }
        // fallbacks
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
