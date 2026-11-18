package com.cnethuka.kegare.platform.event.server;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.server.MinecraftServer;

/** The server finished starting; levels are loaded. */
public final class ServerStartedEvent extends Event {
    private final MinecraftServer server;

    public ServerStartedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
