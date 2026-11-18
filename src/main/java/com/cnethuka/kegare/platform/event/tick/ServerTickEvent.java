package com.cnethuka.kegare.platform.event.tick;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.server.MinecraftServer;

/** Once at the start ({@link Pre}) and once at the end ({@link Post}) of every server tick. */
public abstract class ServerTickEvent extends Event {
    private final MinecraftServer server;

    protected ServerTickEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public static final class Pre extends ServerTickEvent {
        public Pre(MinecraftServer server) {
            super(server);
        }
    }

    public static final class Post extends ServerTickEvent {
        public Post(MinecraftServer server) {
            super(server);
        }
    }
}
