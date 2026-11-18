package com.cnethuka.kegare.platform.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The client's Kegare state belongs to one connection: a late disconnect of an older one must not switch a newer one off. */
class ClientNetTest {
    private static final Identifier ITEM = Identifier.fromNamespaceAndPath("minecraft", "item");

    /** A hello whose ids differ from the client's, so the id table is not empty. */
    private static Runnable offsetTable() {
        return () -> IdRemap.install(List.of(new KegareHello.Entry(ITEM, Identifier.fromNamespaceAndPath("kegare", "token"), 1658)), new IdRemap.Local() {
            @Override
            public OptionalInt idOf(Identifier registry, Identifier name) {
                return OptionalInt.of(1505);
            }

            @Override
            public int foreignCount(Identifier registry) {
                return 150;
            }
        });
    }

    @AfterEach
    void reset() {
        ClientNet.reset();
    }

    @Test
    void aLateDisconnectOfAnOlderConnectionLeavesTheNewOneActive() {
        Object old = new Object();
        Object fresh = new Object();
        ClientNet.activate(old, offsetTable(), () -> {});
        assertTrue(ClientNet.isActive());

        // the player reconnects at once: the new connection begins configuring, then its hello passes ...
        ClientNet.reset();
        assertFalse(ClientNet.isActive(), "inert until the new hello");
        ClientNet.activate(fresh, offsetTable(), () -> {});
        // ... and only now does the old connection's close callback arrive
        ClientNet.resetIfOwner(old);

        assertTrue(ClientNet.isActive());
        assertSame(fresh, ClientNet.owner());
        assertTrue(IdRemap.isActive(), "the new connection's id table is still installed");
        assertEquals(1505, IdRemap.toLocal(ITEM, 1658));
    }

    @Test
    void theOwnersOwnDisconnectSwitchesItOff() {
        Object connection = new Object();
        ClientNet.activate(connection, offsetTable(), () -> {});
        ClientNet.resetIfOwner(connection);
        assertFalse(ClientNet.isActive());
        assertFalse(IdRemap.isActive());
        assertNull(ClientNet.owner());
    }

    @Test
    void aDisconnectBeforeAnyHelloChangesNothing() {
        ClientNet.resetIfOwner(new Object());
        ClientNet.resetIfOwner(null);
        assertFalse(ClientNet.isActive());
    }

    @Test
    void aFailedAnswerLeavesNothingOnAndIsReported() {
        Object connection = new Object();
        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                ClientNet.activate(connection, offsetTable(), () -> { throw new IllegalStateException("channel closed"); }));
        assertEquals("channel closed", failure.getMessage());
        assertFalse(ClientNet.isActive(), "activated only after the ack went out");
        assertFalse(IdRemap.isActive(), "the table installed for it is gone again");
    }

    @Test
    void theSwitchOnComesAfterTheAnswer() {
        Object connection = new Object();
        boolean[] activeWhenAnswering = {true};
        ClientNet.activate(connection, () -> {}, () -> activeWhenAnswering[0] = ClientNet.isActive());
        assertFalse(activeWhenAnswering[0]);
        assertTrue(ClientNet.isActive());
    }
}
