package com.cnethuka.kegare.platform.event.server;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.server.MinecraftServer;

/** The server is about to start: it exists, its levels are not loaded yet (Fabric's SERVER_STARTING). */
public final class ServerAboutToStartEvent extends Event {
    private final MinecraftServer server;

    public ServerAboutToStartEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
