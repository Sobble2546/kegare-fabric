package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

/** Posted on the mod bus for every connection that enters the configuration phase, after the hello task was added. */
public final class RegisterConfigurationTasksEvent extends Event {
    private final ServerConfigurationPacketListenerImpl listener;
    private final MinecraftServer server;

    public RegisterConfigurationTasksEvent(ServerConfigurationPacketListenerImpl listener, MinecraftServer server) {
        this.listener = listener;
        this.server = server;
    }

    public ServerConfigurationPacketListenerImpl getListener() {
        return listener;
    }

    public MinecraftServer getServer() {
        return server;
    }

    /** Adds a task to this connection's configuration; tasks run in the order they were added. */
    public void register(ConfigurationTask task) {
        VersionCompat.addConfigTask(listener, task);
    }
}
