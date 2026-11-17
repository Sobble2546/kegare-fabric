package com.cnethuka.kegare.api.progression;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * An entity that belongs to a player (a puppet, a possessed player's empty body). Its hits are its owner's, and its
 * owner gets nothing for hitting it. Anything a cast spawns while it runs is already traced back to it; implement this
 * on entities made outside a cast, or ones that must stay their owner's however they were made.
 */
public interface OwnedEntity {
    @Nullable
    UUID ownerId();

    /** It picks its own fights (a puppet), so a hit of its own isn't its owner doing anything: see {@link com.cnethuka.kegare.api.KegareProgression#landedOnItsOwn}. */
    default boolean actsOnItsOwn() {
        return false;
    }
}
