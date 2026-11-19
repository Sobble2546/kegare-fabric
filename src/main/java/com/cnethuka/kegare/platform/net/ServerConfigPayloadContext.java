package com.cnethuka.kegare.platform.net;

import java.util.concurrent.CompletableFuture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.entity.player.Player;

/** The context a server-side configuration-phase handler gets: the connection, which has no player yet. Main thread. */
final class ServerConfigPayloadContext implements IPayloadContext {
    private final ServerConfigurationPacketListenerImpl listener;

    ServerConfigPayloadContext(ServerConfigurationPacketListenerImpl listener) {
        this.listener = listener;
    }

    @Override
    public Player player() {
        return null;
    }

    @Override
    public CompletableFuture<Void> enqueueWork(Runnable work) {
        work.run();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void reply(CustomPacketPayload payload) {
        listener.send(VersionCompat.createConfigS2CPacket(payload));
    }

    @Override
    public void disconnect(Component reason) {
        listener.disconnect(reason);
    }

    @Override
    public void finishCurrentTask(ConfigurationTask.Type type) {
        VersionCompat.completeConfigTask(listener, type);
    }
}
