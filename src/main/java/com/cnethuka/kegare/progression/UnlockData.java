package com.cnethuka.kegare.progression;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** What a player has earned from the {@link Unlocks} framework. Kept on death; {@link Unlocks} decides what a reroll takes. */
public final class UnlockData {
    public static final Codec<UnlockData> CODEC = Identifier.CODEC.listOf().xmap(UnlockData::new, d -> new ArrayList<>(d.unlocked));

    final Set<Identifier> unlocked = new HashSet<>();

    public UnlockData() {}

    private UnlockData(List<Identifier> unlocked) {
        this.unlocked.addAll(unlocked);
    }
}
