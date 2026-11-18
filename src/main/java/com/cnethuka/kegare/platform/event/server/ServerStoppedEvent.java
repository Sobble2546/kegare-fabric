package com.cnethuka.kegare.platform.event.server;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.server.MinecraftServer;

/** The server finished stopping. */
public final class ServerStoppedEvent extends Event {
    private final MinecraftServer server;

    public ServerStoppedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
