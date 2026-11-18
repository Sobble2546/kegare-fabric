package com.cnethuka.kegare.trait;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Set of trait IDs a player has (attachment {@code kegare:traits}, kept on death). */
public final class TraitData {
    public static final Codec<TraitData> CODEC = Identifier.CODEC.listOf()
            .xmap(list -> new TraitData(new LinkedHashSet<>(list)), data -> new ArrayList<>(data.traits));

    private final Set<Identifier> traits;

    public TraitData() {
        this(new LinkedHashSet<>());
    }

    private TraitData(Set<Identifier> traits) {
        this.traits = traits;
    }

    public boolean has(Identifier id) {
        return traits.contains(id);
    }

    public Set<Identifier> view() {
        return Collections.unmodifiableSet(traits);
    }

    // Mutators are package-private: go through TraitService (it enforces uniqueness).
    boolean add(Identifier id) {
        return traits.add(id);
    }

    boolean remove(Identifier id) {
        return traits.remove(id);
    }
}
