package com.cnethuka.kegare.platform.client;

import javax.annotation.Nullable;

/**
 * Notices that the local player object was replaced while the connection stayed up (a respawn or a dimension change), which
 * NeoForge reports as {@code ClientPlayerNetworkEvent.Clone} and Fabric does not report at all. Polled once per client tick.
 */
public final class PlayerReplacement<P> {
    @Nullable
    private P last;

    /** A connection became ready with this player (or none yet). */
    public void joined(@Nullable P player) {
        last = player;
    }

    /** The connection ended. */
    public void left() {
        last = null;
    }

    /** @return the previous player if {@code now} is a different one, otherwise null */
    @Nullable
    public P poll(@Nullable P now) {
        P old = last;
        last = now;
        return old != null && now != null && old != now ? old : null;
    }

    @Nullable
    public P last() {
        return last;
    }
}
