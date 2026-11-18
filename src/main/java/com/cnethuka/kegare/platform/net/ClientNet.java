package com.cnethuka.kegare.platform.net;

/**
 * Whether the server we are connected to runs Kegare on this client's network version: it said so in the configuration-phase hello
 * (DESIGN §3.5). Until then, and after every disconnect, the client mod is inert: nothing is sent and no Kegare screen or HUD should
 * draw. Client code that sends or draws asks {@link #isActive()}.
 */
public final class ClientNet {
    private static volatile boolean active;

    private ClientNet() {}

    public static boolean isActive() {
        return active;
    }

    static void setActive(boolean value) {
        active = value;
    }

    /** Back to inert: no hello yet, nothing remapped. */
    static void reset() {
        active = false;
        IdRemap.clear();
    }
}
