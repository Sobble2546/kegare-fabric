package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/** The context a client-side handler gets. Client only: never touched on a dedicated server. */
final class ClientPayloadContext implements IPayloadContext {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final Minecraft client;
    private final Player player;
    private final boolean networkThread;
    /** Sends a packet on this connection from any thread (null: use the client's current connection). */
    private final java.util.function.Consumer<Packet<?>> sender;

    ClientPayloadContext(Minecraft client, Player player, boolean networkThread, java.util.function.Consumer<Packet<?>> sender) {
        this.client = client;
        this.player = player;
        this.networkThread = networkThread;
        this.sender = sender;
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public CompletableFuture<Void> enqueueWork(Runnable work) {
        if (!networkThread) {
            work.run();
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> done = new CompletableFuture<>();
        client.execute(() -> {
            try {
                work.run();
                done.complete(null);
            } catch (Throwable t) {
                LOGGER.error("A payload's queued work failed", t);
                done.completeExceptionally(t);
            }
        });
        return done;
    }

    @Override
    public void reply(CustomPacketPayload payload) {
        Packet<?> packet = VersionCompatClient.createC2SPacket(payload);
        if (sender != null) sender.accept(packet);
        else if (client.getConnection() != null) client.getConnection().send(packet);
    }
}
