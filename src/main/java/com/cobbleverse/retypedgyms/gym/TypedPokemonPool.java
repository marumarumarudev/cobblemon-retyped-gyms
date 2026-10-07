package com.cobbleverse.retypedgyms.gym;

import com.cobbleverse.retypedgyms.config.ModConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Type pools are loaded from config/cobbleverse-retyped-gyms/pools/*.json
 */
public final class TypedPokemonPool {
    private TypedPokemonPool() {}

    public static List<PokemonEntry> getShuffledPool(String type) {
        List<PokemonEntry> pool = ModConfig.get().pools.getOrDefault(type.toLowerCase(Locale.ROOT), List.of());
        ArrayList<PokemonEntry> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    public static List<MegaEntry> getMegaOptions(String type) {
        return ModConfig.get().megas.getOrDefault(type.toLowerCase(Locale.ROOT), List.of());
    }

    public record MegaEntry(String species, String megaStone) {}

    public record PokemonEntry(String species, String gender, String nature, String ability, List<String> movePool) {}
}
