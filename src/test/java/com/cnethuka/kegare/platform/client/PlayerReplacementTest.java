package com.cnethuka.kegare.platform.client;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class PlayerReplacementTest {
    private final Object a = new Object();
    private final Object b = new Object();

    @Test
    void sameOrFirstPlayerIsNotAReplacement() {
        PlayerReplacement<Object> tracker = new PlayerReplacement<>();
        assertNull(tracker.poll(null));
        assertNull(tracker.poll(a)); // the first player of a connection
        assertNull(tracker.poll(a));
    }

    @Test
    void anotherPlayerOnTheSameConnectionReportsTheOldOne() {
        PlayerReplacement<Object> tracker = new PlayerReplacement<>();
        tracker.joined(a);
        assertNull(tracker.poll(a));
        assertSame(a, tracker.poll(b));
        assertNull(tracker.poll(b));
        assertSame(b, tracker.last());
    }

    @Test
    void leavingForgetsThePlayer() {
        PlayerReplacement<Object> tracker = new PlayerReplacement<>();
        tracker.joined(a);
        tracker.left();
        assertNull(tracker.last());
        assertNull(tracker.poll(b)); // a new world, not a replacement
    }

    @Test
    void aGapWithNoPlayerIsNotAReplacement() {
        PlayerReplacement<Object> tracker = new PlayerReplacement<>();
        tracker.joined(a);
        assertNull(tracker.poll(null)); // the packet that replaces the player drops it for a moment
        assertNull(tracker.poll(b));
    }
}
