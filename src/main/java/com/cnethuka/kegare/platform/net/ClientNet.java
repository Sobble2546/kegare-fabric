package com.cnethuka.kegare.platform.net;

/**
 * Whether the server we are connected to runs Kegare on this client's network version: it said so in the configuration-phase hello
 * (DESIGN §3.5). Until then, and after every disconnect, the client mod is inert: nothing is sent and no Kegare screen or HUD should
 * draw. Client code that sends or draws asks {@link #isActive()}.
 * <p>
 * The state belongs to one connection (the one whose hello passed, the "owner"). Fabric's disconnect callbacks can run late, from the
 * netty thread, after a quick reconnect has already begun a new connection: such a callback names the connection it is for and is ignored
 * unless that is the owner, so it cannot make the client inert on a valid new server. The owner and the id table change together, under one
 * lock; reading {@link #isActive()} takes none.
 */
public final class ClientNet {
    private static final Object LOCK = new Object();
    private static volatile Object owner;

    private ClientNet() {}

    public static boolean isActive() {
        return owner != null;
    }

    /**
     * The hello of {@code connection} passed: runs {@code install} (the id table) and {@code answer} (the ack to the server) and only then makes
     * the connection the owner. If either throws, nothing is left installed and the client stays inert; the exception goes on to the caller.
     */
    static void activate(Object connection, Runnable install, Runnable answer) {
        synchronized (LOCK) {
            try {
                install.run();
                answer.run();
            } catch (RuntimeException | Error e) {
                owner = null;
                IdRemap.clear();
                com.cnethuka.kegare.network.EntityKindPayload.clear();
                throw e;
            }
            owner = connection;
        }
    }

    /** A connection is beginning (its configuration phase): whatever an earlier one left behind is gone. */
    static void reset() {
        synchronized (LOCK) {
            owner = null;
            IdRemap.clear();
                com.cnethuka.kegare.network.EntityKindPayload.clear();
        }
    }

    /** {@code connection} closed (possibly long ago): back to inert only if it is the one the state belongs to. */
    static void resetIfOwner(Object connection) {
        synchronized (LOCK) {
            if (connection != null && owner == connection) {
                owner = null;
                IdRemap.clear();
                com.cnethuka.kegare.network.EntityKindPayload.clear();
            }
        }
    }

    /** The connection the state belongs to, or null (for tests). */
    static Object owner() {
        return owner;
    }
}
