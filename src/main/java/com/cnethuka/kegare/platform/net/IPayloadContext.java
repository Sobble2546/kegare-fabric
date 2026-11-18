package com.cnethuka.kegare.platform.net;

import java.util.concurrent.CompletableFuture;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * What a payload handler may use, shaped like NeoForge's context but with only the members Kegare calls.
 * Handlers run on the main thread, except the ones registered with {@link HandlerThread#NETWORK}.
 */
public interface IPayloadContext {
    /** The sender (server side: a ServerPlayer) or the local player (client side). */
    Player player();

    /**
     * Runs the work on the main thread. A handler that already is on it runs the work at once, so an exception goes to whoever called the
     * handler (Fabric logs it); a network-thread handler queues it for the main thread.
     */
    CompletableFuture<Void> enqueueWork(Runnable work);

    /** Answers the sender with a payload of the opposite direction. Safe from the network thread. */
    void reply(CustomPacketPayload payload);
}
