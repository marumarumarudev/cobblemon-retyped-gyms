/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.util.Formatting
 *  net.minecraft.entity.Entity
 *  net.minecraft.text.Text
 *  net.minecraft.server.network.ServerPlayerEntity
 *  net.minecraft.server.MinecraftServer
 */
package com.cobbleverse.retypedgyms;

import com.cobbleverse.retypedgyms.RetypedGyms;
import com.cobbleverse.retypedgyms.config.ModConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

public final class GymBattleGuard {
    private static final String COBBLEMON_MOD_ID = "cobblemon";
    // Banned lists now live in config/cobbleverse-retyped-gyms.json (editable at runtime)
    private static Set<String> bannedLabels() { return ModConfig.get().bannedLabels; }
    private static Set<String> bannedSpeciesFallback() { return ModConfig.get().bannedSpeciesFallback; }
    private static Set<String> customBannedSpecies() { return ModConfig.get().customBannedSpecies; }
    private static final Set<String> GYM_AND_ELITE_NAMES = Set.of("brock", "misty", "lt. surge", "lt surge", "surge", "erika", "koga", "sabrina", "blaine", "giovanni", "falkner", "valerio", "bugsy", "raffaello", "whitney", "chiara", "morty", "angelo", "chuck", "furio", "jasmine", "pryce", "alfredo", "clair", "sandra", "roxanne", "brawly", "wattson", "flannery", "norman", "winona", "tate", "liza", "tate&liza", "juan", "petra", "rudi", "walter", "fiammetta", "tell", "alice", "adriano", "roark", "gardenia", "maylene", "crasher wake", "wake", "fantina", "byron", "candice", "volkner", "cilan", "chili", "cress", "lenora", "burgh", "elesa", "clay", "skyla", "brycen", "drayden", "marlon", "roxie", "viola", "grant", "korrina", "ramos", "clemont", "valerie", "olympia", "wulfric", "ilima", "lana", "kiawe", "mallow", "sophocles", "acerola", "mina", "hala", "olivia", "nanu", "hapu", "milo", "nessa", "kabu", "bea", "allister", "opal", "gordie", "melony", "piers", "marnie", "raihan", "katy", "brassius", "iono", "kofu", "larry", "ryme", "tulip", "grusha", "lorelei", "bruno", "agatha", "siegfrid", "siegfried", "lance", "blue", "green", "red", "will", "karen", "sidney", "phoebe", "glacia", "drake", "steven", "wallace", "fosco", "ester", "frida", "rocco", "aaron", "bertha", "flint", "lucian", "cynthia", "shauntal", "marshal", "grimsley", "caitlin", "alder", "iris", "malva", "siebold", "wikstrom", "drasna", "diantha", "kahili", "molayne", "kukui", "hau", "leon", "rika", "poppy", "hassel", "geeta", "nemona", "kieran");
    private static final List<String> NORMAL_TRAINER_CLASSES = List.of("youngster", "bug catcher", "lass", "camper", "picnicker", "hiker", "fisherman", "sailor", "bird keeper", "black belt", "battle girl", "beauty", "biker", "cue ball", "burglar", "channeler", "collector", "ace trainer", "cooltrainer", "engineer", "gambler", "gentleman", "guitarist", "juggler", "lady", "medium", "ninja boy", "officer", "police", "painter", "pokefan", "pok\u00e9fan", "poke maniac", "pok\u00e9 maniac", "psychic", "rich boy", "roughneck", "ruin maniac", "schoolboy", "schoolkid", "scientist", "super nerd", "swimmer", "tamer", "tuber", "veteran", "waiter", "waitress", "worker");
    private static volatile boolean registered = false;
    private static int tickCounter = 0;
    private static final Map<UUID, Long> NOTIFICATION_COOLDOWNS = new ConcurrentHashMap<UUID, Long>();

    private GymBattleGuard() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded(COBBLEMON_MOD_ID)) {
            RetypedGyms.LOGGER.warn("[GymBattleGuard] Cobblemon not loaded \u2013 legendary gym ban inactive.");
            return;
        }
        RetypedGyms.LOGGER.info("[GymBattleGuard] Initializing Legendary Gym Battle Guard...");
        boolean preSubscribed = GymBattleGuard.registerCobblemonObservable("BATTLE_STARTED_PRE", GymBattleGuard::handleBattleStartedPre);
        boolean postSubscribed = GymBattleGuard.registerCobblemonObservable("BATTLE_STARTED_POST", GymBattleGuard::handleBattleStartedPost);
        ServerTickEvents.END_SERVER_TICK.register(GymBattleGuard::onServerTick);
        registered = true;
        RetypedGyms.LOGGER.info("[GymBattleGuard] Successfully registered! (BATTLE_STARTED_PRE={}, BATTLE_STARTED_POST={}, TickPoller=ACTIVE)", (Object)(preSubscribed ? "ACTIVE" : "FALLBACK"), (Object)(postSubscribed ? "ACTIVE" : "FALLBACK"));
    }

    private static boolean registerCobblemonObservable(String fieldName, Consumer<Object> consumer) {
        try {
            Class<?> eventsClass = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            Field field = eventsClass.getField(fieldName);
            Object observable = field.get(null);
            if (observable == null) {
                RetypedGyms.LOGGER.warn("[GymBattleGuard] CobblemonEvents.{} is null.", (Object)fieldName);
                return false;
            }
            try {
                Class<?> priorityClass = Class.forName("com.cobblemon.mod.common.api.Priority");
                Object highest = priorityClass.getField("HIGHEST").get(null);
                for (Method m : observable.getClass().getMethods()) {
                    Class<?>[] params;
                    if (!"subscribe".equals(m.getName()) || m.getParameterCount() != 2 || !(params = m.getParameterTypes())[0].isAssignableFrom(priorityClass) || !params[1].isAssignableFrom(Consumer.class)) continue;
                    m.invoke(observable, highest, consumer);
                    RetypedGyms.LOGGER.info("[GymBattleGuard] Subscribed to {} with HIGHEST priority.", (Object)fieldName);
                    return true;
                }
            }
            catch (Exception ex) {
                RetypedGyms.LOGGER.debug("[GymBattleGuard] Priority subscribe failed for {}: {}", (Object)fieldName, (Object)ex.getMessage());
            }
            for (Method m : observable.getClass().getMethods()) {
                if (!"subscribe".equals(m.getName()) || m.getParameterCount() != 1 || !m.getParameterTypes()[0].isAssignableFrom(Consumer.class)) continue;
                m.invoke(observable, consumer);
                RetypedGyms.LOGGER.info("[GymBattleGuard] Subscribed to {} via 1-param subscribe(Consumer).", (Object)fieldName);
                return true;
            }
            RetypedGyms.LOGGER.warn("[GymBattleGuard] Could not find subscribe(Consumer) on {}.", (Object)fieldName);
        }
        catch (Exception e) {
            RetypedGyms.LOGGER.error("[GymBattleGuard] Failed to subscribe to {}: {}", new Object[]{fieldName, e.getMessage(), e});
        }
        return false;
    }

    private static void handleBattleStartedPre(Object event) {
        try {
            Object battle = GymBattleGuard.call(event, "getBattle");
            if (battle == null) {
                return;
            }
            if (!GymBattleGuard.isGymOrEliteBattle(battle)) {
                return;
            }
            List<Object> playerActors = GymBattleGuard.getPlayerActors(battle);
            for (Object playerActor : playerActors) {
                net.minecraft.server.network.ServerPlayerEntity player;
                List<String> bannedPokemon = GymBattleGuard.findBannedPokemonForActor(playerActor, player = GymBattleGuard.resolvePlayer(playerActor), battle);
                if (bannedPokemon.isEmpty()) continue;
                try {
                    Method cancelMethod = event.getClass().getMethod("cancel", new Class[0]);
                    cancelMethod.invoke(event, new Object[0]);
                }
                catch (Exception ex) {
                    RetypedGyms.LOGGER.warn("[GymBattleGuard] cancel() invocation failed: {}", (Object)ex.getMessage());
                }
                if (player != null) {
                    GymBattleGuard.notifyPlayer(player, bannedPokemon);
                }
                RetypedGyms.LOGGER.info("[GymBattleGuard] \u2694 Cancelled gym battle for {} due to banned Pok\u00e9mon: {}", (Object)(player != null ? player.getNameForScoreboard() : "Unknown"), bannedPokemon);
                break;
            }
        }
        catch (Exception ex) {
            RetypedGyms.LOGGER.error("[GymBattleGuard] Error handling BATTLE_STARTED_PRE: {}", (Object)ex.getMessage(), (Object)ex);
        }
    }

    private static void handleBattleStartedPost(Object event) {
        try {
            Object battle = GymBattleGuard.call(event, "getBattle");
            if (battle == null) {
                return;
            }
            if (!GymBattleGuard.isGymOrEliteBattle(battle)) {
                return;
            }
            List<Object> playerActors = GymBattleGuard.getPlayerActors(battle);
            for (Object playerActor : playerActors) {
                net.minecraft.server.network.ServerPlayerEntity player;
                List<String> bannedPokemon = GymBattleGuard.findBannedPokemonForActor(playerActor, player = GymBattleGuard.resolvePlayer(playerActor), battle);
                if (bannedPokemon.isEmpty()) continue;
                GymBattleGuard.endBattle(battle, playerActor);
                if (player != null) {
                    GymBattleGuard.notifyPlayer(player, bannedPokemon);
                }
                RetypedGyms.LOGGER.info("[GymBattleGuard] \u2694 Aborted running gym battle for {} due to banned Pok\u00e9mon: {}", (Object)(player != null ? player.getNameForScoreboard() : "Unknown"), bannedPokemon);
                break;
            }
        }
        catch (Exception ex) {
            RetypedGyms.LOGGER.error("[GymBattleGuard] Error handling BATTLE_STARTED_POST: {}", (Object)ex.getMessage(), (Object)ex);
        }
    }

    private static void onServerTick(MinecraftServer server) {
        if (++tickCounter % 10 != 0) {
            return;
        }
        try {
            block2: for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Object battle = GymBattleGuard.getActiveBattleForPlayer(player);
                if (battle == null || !GymBattleGuard.isGymOrEliteBattle(battle)) continue;
                List<Object> playerActors = GymBattleGuard.getPlayerActors(battle);
                for (Object playerActor : playerActors) {
                    List<String> banned;
                    net.minecraft.server.network.ServerPlayerEntity p = GymBattleGuard.resolvePlayer(playerActor);
                    if (p != null && !p.getUuid().equals(player.getUuid()) || (banned = GymBattleGuard.findBannedPokemonForActor(playerActor, player, battle)).isEmpty()) continue;
                    GymBattleGuard.endBattle(battle, playerActor);
                    GymBattleGuard.notifyPlayer(player, banned);
                    RetypedGyms.LOGGER.info("[GymBattleGuard] \u2694 TickPoller closed gym battle for {} due to banned Pok\u00e9mon: {}", (Object)player.getNameForScoreboard(), banned);
                    continue block2;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    static Object getActiveBattleForPlayer(net.minecraft.server.network.ServerPlayerEntity player) {
        try {
            Class<?> brClass = Class.forName("com.cobblemon.mod.common.battles.BattleRegistry");
            for (Method m : brClass.getMethods()) {
                if (!"getBattleByParticipatingPlayerId".equals(m.getName()) || m.getParameterCount() != 1) continue;
                return m.invoke(null, player.getUuid());
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    public static boolean isGymOrEliteBattle(Object battle) {
        if (battle == null) {
            return false;
        }
        try {
            Object actorsObj = GymBattleGuard.call(battle, "getActors");
            if (!(actorsObj instanceof Iterable)) {
                return false;
            }
            Iterable iterable = (Iterable)actorsObj;
            for (Object actor : iterable) {
                boolean isNpc;
                if (actor == null) continue;
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
                String className = actor.getClass().getSimpleName();
                boolean bl = isNpc = "NPC".equalsIgnoreCase(typeStr) || className.contains("Trainer") || className.contains("NPC");
                if (!isNpc) continue;
                String name = "";
                try {
                    Object nameObj = GymBattleGuard.call(actor, "getName");
                    if (nameObj instanceof net.minecraft.text.Text) {
                        net.minecraft.text.Text comp = (net.minecraft.text.Text)nameObj;
                        name = comp.getString().trim();
                    } else if (nameObj != null) {
                        name = nameObj.toString().trim();
                    }
                }
                catch (Exception nameObj) {
                    // empty catch block
                }
                String trainerId = "";
                try {
                    Object tid = GymBattleGuard.call(actor, "getTrainerName");
                    if (tid != null) {
                        trainerId = tid.toString().trim();
                    }
                }
                catch (Exception tid) {
                    // empty catch block
                }
                if (GymBattleGuard.isGymOrEliteName(name) || GymBattleGuard.isGymOrEliteName(trainerId)) {
                    return true;
                }
                try {
                    Object entityObj = GymBattleGuard.call(actor, "getEntity");
                    if (entityObj == null) {
                        entityObj = GymBattleGuard.call(actor, "getNpc");
                    }
                    if (entityObj instanceof net.minecraft.entity.Entity) {
                        net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity)entityObj;
                        String entityCustomName = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
                        String entityScoreboard = entity.getNameForScoreboard();
                        if (GymBattleGuard.isGymOrEliteName(entityCustomName) || GymBattleGuard.isGymOrEliteName(entityScoreboard)) {
                            return true;
                        }
                        Set<?> tags = entity.getCommandTags();
                        if (tags != null) {
                            for (Object tagObj : tags) {
                                String lowerTag = tagObj.toString().toLowerCase(Locale.ROOT);
                                if (!lowerTag.contains("gym") && !lowerTag.contains("leader") && !lowerTag.contains("elite") && !lowerTag.contains("e4") && !lowerTag.contains("champion") && !lowerTag.contains("league")) continue;
                                return true;
                            }
                        }
                    }
                }
                catch (Exception entityObj) {
                    // empty catch block
                }
                try {
                    Object pList = GymBattleGuard.call(actor, "getPokemonList");
                    if (!(pList instanceof Iterable)) continue;
                    Iterable pokeList = (Iterable)pList;
                    for (Object battlePoke : pokeList) {
                        Set aspects;
                        Object aspectsObj;
                        Object poke = GymBattleGuard.call(battlePoke, "getEffectedPokemon");
                        if (poke == null) {
                            poke = GymBattleGuard.call(battlePoke, "getOriginalPokemon");
                        }
                        if (poke == null || !((aspectsObj = GymBattleGuard.call(poke, "getAspects")) instanceof Set) || !(aspects = (Set)aspectsObj).contains("raid")) continue;
                        return true;
                    }
                }
                catch (Exception exception) {
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    public static boolean isGymOrEliteName(String name) {
        String[] words;
        if (name == null || name.isBlank()) {
            return false;
        }
        String clean = name.toLowerCase(Locale.ROOT).trim();
        if (clean.contains("gym") || clean.contains("leader") || clean.contains("elite") || clean.contains("e4") || clean.contains("champion") || clean.contains("league")) {
            return true;
        }
        if (clean.contains("kanto_") || clean.contains("johto_") || clean.contains("hoenn_") || clean.contains("sinnoh_") || clean.contains("unova_") || clean.contains("kalos_") || clean.contains("alola_") || clean.contains("galar_") || clean.contains("paldea_") || clean.contains("team_rocket_")) {
            for (String known : GYM_AND_ELITE_NAMES) {
                if (!clean.contains(known)) continue;
                return true;
            }
        }
        if (GYM_AND_ELITE_NAMES.contains(clean)) {
            return true;
        }
        if (GymBattleGuard.isNormalTrainerClass(clean)) {
            return false;
        }
        String alphaOnly = clean.replaceAll("[^a-z0-9\\s]", " ").trim();
        for (String word : words = alphaOnly.split("\\s+")) {
            if (!GYM_AND_ELITE_NAMES.contains(word)) continue;
            return true;
        }
        return false;
    }

    private static boolean isNormalTrainerClass(String clean) {
        for (String normalClass : NORMAL_TRAINER_CLASSES) {
            if (!clean.contains(normalClass)) continue;
            return true;
        }
        return false;
    }

    static boolean isNpcBattle(Object battle) {
        try {
            Object actorsObj = GymBattleGuard.call(battle, "getActors");
            if (!(actorsObj instanceof Iterable)) {
                return false;
            }
            Iterable iterable = (Iterable)actorsObj;
            for (Object actor : iterable) {
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
                String className = actor.getClass().getSimpleName();
                if (!"NPC".equalsIgnoreCase(typeStr) && !className.contains("Trainer") && !className.contains("NPC")) continue;
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    private static List<Object> getPlayerActors(Object battle) {
        ArrayList<Object> result;
        block6: {
            result = new ArrayList<Object>();
            try {
                Object actorsObj = GymBattleGuard.call(battle, "getActors");
                if (!(actorsObj instanceof Iterable)) break block6;
                Iterable iterable = (Iterable)actorsObj;
                for (Object actor : iterable) {
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
                    String className = actor.getClass().getSimpleName();
                    if (!"PLAYER".equalsIgnoreCase(typeStr) && !className.contains("Player")) continue;
                    result.add(actor);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return result;
    }

    private static List<String> findBannedPokemonForActor(Object playerActor, net.minecraft.server.network.ServerPlayerEntity player, Object battle) {
        LinkedHashSet<String> bannedNames;
        block16: {
            bannedNames = new LinkedHashSet<String>();
            try {
                Object pList = GymBattleGuard.call(playerActor, "getPokemonList");
                if (pList instanceof Iterable) {
                    Iterable pokeList = (Iterable)pList;
                    for (Object battlePoke : pokeList) {
                        Object poke = GymBattleGuard.call(battlePoke, "getOriginalPokemon");
                        if (poke == null) {
                            poke = GymBattleGuard.call(battlePoke, "getEffectedPokemon");
                        }
                        if (poke == null) continue;
                        GymBattleGuard.checkPokemon(poke, bannedNames);
                    }
                }
            }
            catch (Exception pList) {
                // empty catch block
            }
            try {
                Object stores = GymBattleGuard.call(battle, "getBattlePartyStores");
                if (stores instanceof Iterable) {
                    Iterable storeList = (Iterable)stores;
                    for (Object store : storeList) {
                        UUID storeUuid = (UUID)GymBattleGuard.call(store, "getPlayerUUID");
                        if (player != null && storeUuid != null && !player.getUuid().equals(storeUuid) || !(store instanceof Iterable)) continue;
                        Iterable pokeIter = (Iterable)store;
                        for (Object poke : pokeIter) {
                            if (poke == null) continue;
                            GymBattleGuard.checkPokemon(poke, bannedNames);
                        }
                    }
                }
            }
            catch (Exception stores) {
                // empty catch block
            }
            if (bannedNames.isEmpty() && player != null) {
                try {
                    Class<?> cobClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
                    Object cobInst = cobClass.getField("INSTANCE").get(null);
                    Object storage = GymBattleGuard.call(cobInst, "getStorage");
                    if (storage == null) break block16;
                    for (Method m : storage.getClass().getMethods()) {
                        if (!"getParty".equals(m.getName()) || m.getParameterCount() != 1) continue;
                        Object party = m.invoke(storage, player);
                        if (party instanceof Iterable) {
                            Iterable partyIter = (Iterable)party;
                            for (Object poke : partyIter) {
                                if (poke == null) continue;
                                GymBattleGuard.checkPokemon(poke, bannedNames);
                            }
                        }
                        break;
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
        return new ArrayList<String>(bannedNames);
    }

    private static boolean isBannedSpeciesName(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        String clean = id.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
        // strip namespace if present (modid:species -> species)
        String bare = clean.contains(":") ? clean.substring(clean.lastIndexOf(':') + 1) : clean;

        if (bannedSpeciesFallback().contains(clean) || bannedSpeciesFallback().contains(bare)) {
            return true;
        }
        if (customBannedSpecies().contains(clean) || customBannedSpecies().contains(bare)) {
            return true;
        }
        for (String custom : customBannedSpecies()) {
            if (custom == null || custom.isEmpty()) continue;
            String c = custom.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
            String cBare = c.contains(":") ? c.substring(c.lastIndexOf(':') + 1) : c;
            if (clean.equals(c) || bare.equals(cBare)
                    || clean.endsWith(":" + cBare) || clean.endsWith("/" + cBare) || clean.endsWith("_" + cBare)
                    || bare.contains(cBare) || clean.contains(cBare)) {
                return true;
            }
        }
        for (String banned : bannedSpeciesFallback()) {
            if (banned == null || banned.isEmpty()) continue;
            String b = banned.toLowerCase(Locale.ROOT);
            if (bare.equals(b) || clean.endsWith(":" + b) || bare.contains(b)) {
                return true;
            }
        }
        return false;
    }

    private static void checkPokemon(Object poke, Set<String> bannedNames) {
        try {
            String lowerName;
            Object species = GymBattleGuard.call(poke, "getSpecies");
            if (species == null) {
                return;
            }
            String name = (String)GymBattleGuard.call(species, "getName");
            if (name == null) {
                name = "Unknown";
            }
            if (GymBattleGuard.isBannedSpeciesName(lowerName = name.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_"))) {
                bannedNames.add(name);
                return;
            }
            try {
                Object resId = GymBattleGuard.call(species, "getResourceIdentifier");
                if (resId != null) {
                    String path = (String)GymBattleGuard.call(resId, "getPath");
                    if (path != null && GymBattleGuard.isBannedSpeciesName(path)) {
                        bannedNames.add(name);
                        return;
                    }
                    if (GymBattleGuard.isBannedSpeciesName(resId.toString())) {
                        bannedNames.add(name);
                        return;
                    }
                }
            }
            catch (Exception resId) {
                // empty catch block
            }
            try {
                String showdownId = (String)GymBattleGuard.call(species, "getShowdownId");
                if (showdownId != null && GymBattleGuard.isBannedSpeciesName(showdownId)) {
                    bannedNames.add(name);
                    return;
                }
            }
            catch (Exception showdownId) {
                // empty catch block
            }
            Object labelsObj = GymBattleGuard.call(species, "getLabels");
            if (labelsObj instanceof Iterable) {
                Iterable it = (Iterable)labelsObj;
                for (Object lbl : it) {
                    String s;
                    if (lbl == null || !bannedLabels().contains(s = lbl.toString().toLowerCase(Locale.ROOT))) continue;
                    bannedNames.add(name);
                    return;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void notifyPlayer(net.minecraft.server.network.ServerPlayerEntity player, List<String> banned) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = NOTIFICATION_COOLDOWNS.get(player.getUuid());
        if (last != null && now - last < 3000L) {
            return;
        }
        NOTIFICATION_COOLDOWNS.put(player.getUuid(), now);
        player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"\u2694 Gym / League Battle Denied! ").formatted(new net.minecraft.util.Formatting[]{net.minecraft.util.Formatting.BOLD, net.minecraft.util.Formatting.RED}).append((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"Legendary, Mythical & Banned Pok\u00e9mon are not permitted in Gym & Elite Four battles.").styled(s -> s.withBold(Boolean.valueOf(false)).withColor(net.minecraft.util.Formatting.YELLOW))));
        player.sendMessage((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)"  Banned Pok\u00e9mon: ").formatted(net.minecraft.util.Formatting.GRAY).append((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)String.join((CharSequence)", ", banned)).formatted(new net.minecraft.util.Formatting[]{net.minecraft.util.Formatting.GOLD, net.minecraft.util.Formatting.BOLD})).append((net.minecraft.text.Text)net.minecraft.text.Text.literal((String)" \u2014 please deposit or swap them before challenging!").formatted(net.minecraft.util.Formatting.GRAY)));
    }

    private static void endBattle(Object battle, Object playerActor) {
        if (battle == null) {
            return;
        }
        try {
            Class<?> brClass = Class.forName("com.cobblemon.mod.common.battles.BattleRegistry");
            for (Method m : brClass.getMethods()) {
                if (!"closeBattle".equals(m.getName()) || m.getParameterCount() != 1) continue;
                m.invoke(null, battle);
                return;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        for (String name : List.of("end", "stop", "close")) {
            try {
                battle.getClass().getMethod(name, new Class[0]).invoke(battle, new Object[0]);
                return;
            }
            catch (Exception exception) {
            }
        }
        if (playerActor != null) {
            for (String name : List.of("surrender", "forfeit", "flee")) {
                for (Method m : battle.getClass().getMethods()) {
                    if (!m.getName().equals(name) || m.getParameterCount() != 1) continue;
                    try {
                        m.invoke(battle, playerActor);
                        return;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            }
        }
        RetypedGyms.LOGGER.warn("[GymBattleGuard] Could not end the battle programmatically.");
    }

    private static net.minecraft.server.network.ServerPlayerEntity resolvePlayer(Object playerActor) {
        if (playerActor == null) {
            return null;
        }
        for (String mname : List.of("getEntity", "getPlayer")) {
            try {
                Object r = playerActor.getClass().getMethod(mname, new Class[0]).invoke(playerActor, new Object[0]);
                if (!(r instanceof net.minecraft.server.network.ServerPlayerEntity)) continue;
                net.minecraft.server.network.ServerPlayerEntity sp = (net.minecraft.server.network.ServerPlayerEntity)r;
                return sp;
            }
            catch (Exception exception) {
            }
        }
        for (Method m : playerActor.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || !net.minecraft.server.network.ServerPlayerEntity.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object r = m.invoke(playerActor, new Object[0]);
                if (!(r instanceof net.minecraft.server.network.ServerPlayerEntity)) continue;
                net.minecraft.server.network.ServerPlayerEntity sp = (net.minecraft.server.network.ServerPlayerEntity)r;
                return sp;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return null;
    }

    static Object call(Object target, String method) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(method, new Class[0]).invoke(target, new Object[0]);
        }
        catch (Exception exception) {
            try {
                Method m = target.getClass().getDeclaredMethod(method, new Class[0]);
                m.setAccessible(true);
                return m.invoke(target, new Object[0]);
            }
            catch (Exception exception2) {
                return null;
            }
        }
    }
}

