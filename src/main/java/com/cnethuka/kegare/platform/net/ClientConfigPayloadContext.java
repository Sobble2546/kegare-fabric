package com.cnethuka.kegare.platform.net;

import java.util.concurrent.CompletableFuture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/** The context a client-side configuration-phase handler gets: answers go back on the connection being configured. Client only. */
final class ClientConfigPayloadContext implements IPayloadContext {
    private final VersionCompatClient.Responder responder;

    ClientConfigPayloadContext(VersionCompatClient.Responder responder) {
        this.responder = responder;
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
        responder.send(payload);
    }

    @Override
    public void disconnect(Component reason) {
        responder.disconnect(reason);
    }
}
