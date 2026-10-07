/*
 * Decompiled with CFR 0.152.
 */
package com.cobbleverse.retypedgyms.gym;

import com.cobbleverse.retypedgyms.gym.GymTeamBuilder;
import java.util.List;

public record GymLeaderDef(String fileId, String displayName, String type, int teamSize, int minLevel, int maxLevel, int itemCount, String signatureMega, List<GymTeamBuilder.FixedPokemonSpec> fixedTeam) {
    public GymLeaderDef(String fileId, String displayName, String type, int teamSize, int minLevel, int maxLevel, int itemCount) {
        this(fileId, displayName, type, teamSize, minLevel, maxLevel, itemCount, null, null);
    }

    public GymLeaderDef(String fileId, String displayName, String type, int teamSize, int minLevel, int maxLevel, int itemCount, String signatureMega) {
        this(fileId, displayName, type, teamSize, minLevel, maxLevel, itemCount, signatureMega, null);
    }
}

