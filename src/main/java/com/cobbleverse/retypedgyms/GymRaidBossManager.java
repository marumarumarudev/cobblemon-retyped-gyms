/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.minecraft.entity.boss.BossBar$net.minecraft.entity.boss.BossBar$Color
 *  net.minecraft.entity.boss.BossBar$net.minecraft.entity.boss.BossBar$Style
 *  net.minecraft.entity.Entity
 *  net.minecraft.particle.ParticleEffect
 *  net.minecraft.particle.ParticleTypes
 *  net.minecraft.text.Text
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.entity.boss.ServerBossBar
 *  net.minecraft.server.world.ServerWorld
 *  net.minecraft.server.network.ServerPlayerEntity
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.sound.SoundCategory
 *  net.minecraft.text.MutableText
 *  net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
 *  net.minecraft.network.packet.s2c.play.TitleS2CPacket
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleverse.retypedgyms;

import com.cobbleverse.retypedgyms.GymBattleGuard;
import com.cobbleverse.retypedgyms.RetypedGyms;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.network.packet.Packet;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;

public final class GymRaidBossManager {
    private static final Set<String> RAID_BOSS_SPECIES = Set.of("annihilape", "stalliava", "cradily", "metagross", "aegislash", "kingambit", "sandyshocks", "kommoo", "icicranid", "karoshicore", "lapras", "mechabang", "roaringmoon", "roserade", "sugareign", "neuveon", "equirin", "runerigus", "irontreads", "magnezone", "bisharp", "diggersby");
    private static final Map<UUID, PlayerRaidState> ACTIVE_BOSS_SESSIONS = new ConcurrentHashMap<UUID, PlayerRaidState>();
    private static final Set<UUID> DEFEATED_POKEMON_IDS = Collections.newSetFromMap(new ConcurrentHashMap());
    private static int tickCounter = 0;
    private static boolean registered = false;

    private GymRaidBossManager() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(GymRaidBossManager::onServerTick);
        registered = true;
        RetypedGyms.LOGGER.info("[GymRaidBossManager] Registered Gym Raid Boss Encounter Manager.");
    }

    private static void onServerTick(MinecraftServer server) {
        if (++tickCounter % 5 != 0) {
            return;
        }
        if (DEFEATED_POKEMON_IDS.size() > 1000) {
            DEFEATED_POKEMON_IDS.clear();
        }
        for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            Object battle = GymBattleGuard.getActiveBattleForPlayer(player);
            if (battle == null || !GymBattleGuard.isGymOrEliteBattle(battle)) {
                GymRaidBossManager.cleanupSession(playerId);
                continue;
            }
            Object raidBattlePokemon = GymRaidBossManager.findActiveRaidBossPokemon(battle);
            if (raidBattlePokemon == null) {
                GymRaidBossManager.cleanupSession(playerId);
                continue;
            }
            GymRaidBossManager.processRaidBoss(player, battle, raidBattlePokemon);
        }
    }

    private static void processRaidBoss(net.minecraft.server.network.ServerPlayerEntity player, Object battle, Object battlePokemon) {
        float hpRatio;
        UUID playerId = player.getUuid();
        UUID bpId = GymRaidBossManager.getPokemonUuid(battlePokemon);
        if (bpId != null && DEFEATED_POKEMON_IDS.contains(bpId)) {
            return;
        }
        int currentHp = GymRaidBossManager.getInt(battlePokemon, "getHealth", 0);
        int maxHp = Math.max(1, GymRaidBossManager.getInt(battlePokemon, "getMaxHealth", 1));
        String species = GymRaidBossManager.getSpecies(battlePokemon);
        PlayerRaidState state = ACTIVE_BOSS_SESSIONS.computeIfAbsent(playerId, k -> new PlayerRaidState());
        if (currentHp <= 0) {
            if (!state.defeated) {
                state.defeated = true;
                if (bpId != null) {
                    DEFEATED_POKEMON_IDS.add(bpId);
                }
                String formattedName = GymRaidBossManager.formatSpeciesName(species);
                if (state.bossEvent != null) {
                    try {
                        state.bossEvent.clearPlayers();
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    state.bossEvent = null;
                }
                if (state.shieldActive) {
                    try {
                        player.playSoundToPlayer(net.minecraft.sound.SoundEvents.ITEM_SHIELD_BREAK, net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 0.9f);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a7c" + formattedName + " succumbed to the onslaught and broke its stance!")));
                }
                try {
                    player.playSoundToPlayer(net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sound.SoundCategory.MASTER, 1.0f, 1.0f);
                    player.networkHandler.sendPacket((net.minecraft.network.packet.Packet)new net.minecraft.network.packet.s2c.play.TitleS2CPacket((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"\u00a76\u00a7l\u2605 RAID BOSS DEFEATED! \u2605")));
                    player.networkHandler.sendPacket((net.minecraft.network.packet.Packet)new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a7eYou shattered the Gym Raid Boss: " + formattedName + "!"))));
                }
                catch (Exception exception) {
                    // empty catch block
                }
                player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a76\u00a7l\u2605 RAID BOSS DEFEATED! \u2605 \u00a7aYou successfully conquered " + formattedName + "!")));
                RetypedGyms.LOGGER.info("[GymRaidBossManager] Raid Boss ({}) defeated by {}", (Object)formattedName, (Object)player.getNameForScoreboard());
            }
            return;
        }
        if (!state.active) {
            state.active = true;
            state.defeated = false;
            state.shieldActive = false;
            state.species = species;
            state.pokemonUuid = bpId;
            String formattedName = GymRaidBossManager.formatSpeciesName(species);
            net.minecraft.text.MutableText bossTitle = net.minecraft.text.Text.literal((String)("\u00a7d\u00a7l[RAID BOSS] \u00a7f" + formattedName + " \u00a7e\u2605\u2605\u2605\u2605\u2605\u2605"));
            state.bossEvent = new net.minecraft.entity.boss.ServerBossBar((net.minecraft.text.Text)bossTitle, net.minecraft.entity.boss.BossBar.Color.PURPLE, net.minecraft.entity.boss.BossBar.Style.NOTCHED_10);
            state.bossEvent.setDarkenSky(true);
            state.bossEvent.setThickenFog(true);
            state.bossEvent.setPercent(Math.max(0.0f, Math.min(1.0f, (float)currentHp / (float)maxHp)));
            state.bossEvent.addPlayer(player);
            try {
                player.playSoundToPlayer(net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN, net.minecraft.sound.SoundCategory.HOSTILE, 0.9f, 0.7f);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                player.networkHandler.sendPacket((net.minecraft.network.packet.Packet)new net.minecraft.network.packet.s2c.play.TitleS2CPacket((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"\u00a74\u00a7l\u26a0 RAID BOSS \u26a0")));
                player.networkHandler.sendPacket((net.minecraft.network.packet.Packet)new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a7c" + formattedName + " awakened a terrifying Raid Aura!"))));
            }
            catch (Exception exception) {
                // empty catch block
            }
            player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a7d\u00a7l[RAID BOSS] \u00a7c" + formattedName + " entered the arena surrounded by an ominous Raid Aura!")));
            player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"\u00a77  \u2022 Defense & Sp. Def sharply rose (+2)!"));
            player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"\u00a77  \u2022 Attack & Sp. Atk rose (+1)!"));
            GymRaidBossManager.applyRaidAuraStatBoost(battlePokemon);
            RetypedGyms.LOGGER.info("[GymRaidBossManager] Triggered Raid Boss encounter ({}) for player {}", (Object)formattedName, (Object)player.getNameForScoreboard());
        }
        if ((hpRatio = (float)currentHp / (float)maxHp) <= 0.5f && !state.shieldActive && !state.defeated) {
            state.shieldActive = true;
            String formattedName = GymRaidBossManager.formatSpeciesName(species);
            if (state.bossEvent != null) {
                state.bossEvent.setName((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a7d\u00a7l[RAID BOSS] \u00a7f" + formattedName + " \u00a7b\u00a7l[\ud83d\udee1 SHIELD ACTIVE \ud83d\udee1] \u00a7e\u2605\u2605\u2605\u2605\u2605\u2605")));
                state.bossEvent.setColor(net.minecraft.entity.boss.BossBar.Color.BLUE);
            }
            try {
                player.playSoundToPlayer(net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE, net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 1.2f);
                player.playSoundToPlayer(net.minecraft.sound.SoundEvents.ITEM_SHIELD_BLOCK, net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 0.8f);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                player.networkHandler.sendPacket((net.minecraft.network.packet.Packet)new net.minecraft.network.packet.s2c.play.TitleS2CPacket((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"\u00a7b\u00a7l\ud83d\udee1 RAID SHIELD ACTIVE \ud83d\udee1")));
                player.networkHandler.sendPacket((net.minecraft.network.packet.Packet)new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a7eEnergy has begun to gather around " + formattedName + "!"))));
            }
            catch (Exception exception) {
                // empty catch block
            }
            player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)("\u00a7b\u00a7l[RAID SHIELD] \u00a7eEnergy has begun to gather around " + formattedName + "! Its defenses hardened (+1.5x)!")));
            GymRaidBossManager.applyRaidShieldStatBoost(battlePokemon);
            RetypedGyms.LOGGER.info("[GymRaidBossManager] Raid Shield activated at 50% HP for {} (player: {})", (Object)formattedName, (Object)player.getNameForScoreboard());
        }
        if (state.bossEvent != null) {
            float progress = Math.max(0.0f, Math.min(1.0f, hpRatio));
            state.bossEvent.setPercent(progress);
        }
        try {
            Object entityObj = GymBattleGuard.call(battlePokemon, "getEntity");
            if (entityObj instanceof net.minecraft.entity.Entity) {
                net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity)entityObj;
                net.minecraft.server.world.ServerWorld level = player.getServerWorld();
                level.spawnParticles((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.DRAGON_BREATH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 3, 0.4, 0.5, 0.4, 0.02);
                if (state.shieldActive) {
                    level.spawnParticles((net.minecraft.particle.ParticleEffect)net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 0.8, entity.getZ(), 4, 0.5, 0.6, 0.5, 0.04);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void applyRaidShieldStatBoost(Object battlePokemon) {
        try {
            Object statChangesObj = GymBattleGuard.call(battlePokemon, "getStatChanges");
            if (statChangesObj instanceof Map) {
                Map rawMap;
                Map statChanges = rawMap = (Map)statChangesObj;
                Class<?> statsClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stats");
                Object def = statsClass.getField("DEFENCE").get(null);
                Object spDef = statsClass.getField("SPECIAL_DEFENCE").get(null);
                int currentDef = ((Number)statChanges.getOrDefault(def, 0)).intValue();
                int currentSpD = ((Number)statChanges.getOrDefault(spDef, 0)).intValue();
                statChanges.put(def, Math.min(6, currentDef + 1));
                statChanges.put(spDef, Math.min(6, currentSpD + 1));
                GymBattleGuard.call(battlePokemon, "sendUpdate");
                RetypedGyms.LOGGER.info("[GymRaidBossManager] Successfully applied Raid Shield stat boost (+1 Def/SpD).");
            }
        }
        catch (Exception e) {
            RetypedGyms.LOGGER.warn("[GymRaidBossManager] Could not apply Raid Shield stat boost: {}", (Object)e.getMessage());
        }
    }

    private static void applyRaidAuraStatBoost(Object battlePokemon) {
        try {
            Object statChangesObj = GymBattleGuard.call(battlePokemon, "getStatChanges");
            if (statChangesObj instanceof Map) {
                Map rawMap;
                Map statChanges = rawMap = (Map)statChangesObj;
                Class<?> statsClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stats");
                Object def = statsClass.getField("DEFENCE").get(null);
                Object spDef = statsClass.getField("SPECIAL_DEFENCE").get(null);
                Object atk = statsClass.getField("ATTACK").get(null);
                Object spAtk = statsClass.getField("SPECIAL_ATTACK").get(null);
                statChanges.put(def, 2);
                statChanges.put(spDef, 2);
                statChanges.put(atk, 1);
                statChanges.put(spAtk, 1);
                GymBattleGuard.call(battlePokemon, "sendUpdate");
                RetypedGyms.LOGGER.info("[GymRaidBossManager] Successfully applied Raid Aura Omniboost to active Pokemon.");
            }
        }
        catch (Exception e) {
            RetypedGyms.LOGGER.warn("[GymRaidBossManager] Could not apply Raid Aura Omniboost: {}", (Object)e.getMessage());
        }
    }

    private static Object findActiveRaidBossPokemon(Object battle) {
        try {
            Object actorsObj = GymBattleGuard.call(battle, "getActors");
            if (!(actorsObj instanceof Iterable)) {
                return null;
            }
            Iterable iterable = (Iterable)actorsObj;
            for (Object actor : iterable) {
                Object pokemonListObj;
                String className = actor.getClass().getSimpleName();
                String typeStr = "";
                try {
                    Object type = GymBattleGuard.call(actor, "getType");
                    if (type != null) {
                        typeStr = type.toString();
                    }
                }
                catch (Exception type) {
                    // empty catch block
                }
                if (!"NPC".equalsIgnoreCase(typeStr) && !className.contains("Trainer") && !className.contains("NPC")) continue;
                Object activeListObj = GymBattleGuard.call(actor, "getActivePokemon");
                if (activeListObj instanceof Iterable) {
                    Iterable activeIterable = (Iterable)activeListObj;
                    for (Object activeSlot : activeIterable) {
                        UUID bpId;
                        Object bp = GymBattleGuard.call(activeSlot, "getBattlePokemon");
                        if (bp == null) {
                            bp = activeSlot;
                        }
                        if (!GymRaidBossManager.isRaidBoss(bp) || (bpId = GymRaidBossManager.getPokemonUuid(bp)) != null && DEFEATED_POKEMON_IDS.contains(bpId)) continue;
                        return bp;
                    }
                }
                if (!((pokemonListObj = GymBattleGuard.call(actor, "getPokemonList")) instanceof Iterable)) continue;
                Iterable pokemonList = (Iterable)pokemonListObj;
                for (Object bp : pokemonList) {
                    UUID bpId;
                    Boolean sentOut = (Boolean)GymBattleGuard.call(bp, "isSentOut");
                    if (!Boolean.TRUE.equals(sentOut) || !GymRaidBossManager.isRaidBoss(bp) || (bpId = GymRaidBossManager.getPokemonUuid(bp)) != null && DEFEATED_POKEMON_IDS.contains(bpId)) continue;
                    return bp;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static boolean isRaidBoss(Object battlePokemon) {
        if (battlePokemon == null) {
            return false;
        }
        try {
            Set aspects;
            Object aspectsObj;
            Object p = GymBattleGuard.call(battlePokemon, "getEffectedPokemon");
            if (p == null) {
                p = GymBattleGuard.call(battlePokemon, "getOriginalPokemon");
            }
            if (p != null && (aspectsObj = GymBattleGuard.call(p, "getAspects")) instanceof Set && (aspects = (Set)aspectsObj).contains("raid")) {
                return true;
            }
            String species = GymRaidBossManager.getSpecies(battlePokemon);
            if (species != null && RAID_BOSS_SPECIES.contains(species.toLowerCase())) {
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    private static UUID getPokemonUuid(Object battlePokemon) {
        if (battlePokemon == null) {
            return null;
        }
        try {
            Object pId;
            Object id = GymBattleGuard.call(battlePokemon, "getUuid");
            if (id instanceof UUID) {
                UUID u = (UUID)id;
                return u;
            }
            Object p = GymBattleGuard.call(battlePokemon, "getEffectedPokemon");
            if (p == null) {
                p = GymBattleGuard.call(battlePokemon, "getOriginalPokemon");
            }
            if (p != null && (pId = GymBattleGuard.call(p, "getUuid")) instanceof UUID) {
                UUID u = (UUID)pId;
                return u;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static String getSpecies(Object battlePokemon) {
        try {
            Object speciesObj;
            Object p = GymBattleGuard.call(battlePokemon, "getEffectedPokemon");
            if (p == null) {
                p = GymBattleGuard.call(battlePokemon, "getOriginalPokemon");
            }
            if (p != null && (speciesObj = GymBattleGuard.call(p, "getSpecies")) != null) {
                Object name = GymBattleGuard.call(speciesObj, "getName");
                if (name != null) {
                    return name.toString().toLowerCase();
                }
                return speciesObj.toString().toLowerCase();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "";
    }

    private static int getInt(Object target, String method, int def) {
        try {
            Object res = GymBattleGuard.call(target, method);
            if (res instanceof Number) {
                Number num = (Number)res;
                return num.intValue();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return def;
    }

    private static String formatSpeciesName(String species) {
        if (species == null || species.isBlank()) {
            return "Pokemon";
        }
        return Character.toUpperCase(species.charAt(0)) + species.substring(1).toLowerCase();
    }

    private static void cleanupSession(UUID playerId) {
        PlayerRaidState state = ACTIVE_BOSS_SESSIONS.remove(playerId);
        if (state != null && state.bossEvent != null) {
            try {
                state.bossEvent.clearPlayers();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private static final class PlayerRaidState {
        boolean active = false;
        boolean defeated = false;
        boolean shieldActive = false;
        UUID pokemonUuid = null;
        String species = "";
        net.minecraft.entity.boss.ServerBossBar bossEvent = null;

        private PlayerRaidState() {
        }
    }
}

