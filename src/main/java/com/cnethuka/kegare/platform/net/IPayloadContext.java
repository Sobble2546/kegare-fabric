package com.cnethuka.kegare.platform.net;

import java.util.concurrent.CompletableFuture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.player.Player;

/**
 * What a payload handler may use, shaped like NeoForge's context but with only the members Kegare calls.
 * Handlers run on the main thread, except the ones registered with {@link HandlerThread#NETWORK}.
 */
public interface IPayloadContext {
    /** The sender (server side: a ServerPlayer) or the local player (client side). Null in the configuration phase. */
    Player player();

    /**
     * Runs the work on the main thread. A handler that already is on it runs the work at once, so an exception goes to whoever called the
     * handler (Fabric logs it); a network-thread handler queues it for the main thread.
     */
    CompletableFuture<Void> enqueueWork(Runnable work);

    /** Answers the sender with a payload of the opposite direction. Safe from the network thread. */
    void reply(CustomPacketPayload payload);

    /** Closes the connection with this reason. Configuration-phase handlers only (Kegare calls it nowhere else). */
    default void disconnect(Component reason) {
        throw new UnsupportedOperationException("disconnect is only for configuration-phase handlers");
    }

    /** Finishes the server's configuration task of this type, so the connection moves on. Server-side configuration handlers only. */
    default void finishCurrentTask(ConfigurationTask.Type type) {
        throw new UnsupportedOperationException("finishCurrentTask is only for server-side configuration handlers");
    }
}
