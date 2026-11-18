package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** The context a server-side handler gets: the sender. */
final class ServerPayloadContext implements IPayloadContext {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final ServerPlayer player;
    private final MinecraftServer server;
    private final boolean networkThread;

    ServerPayloadContext(ServerPlayer player, MinecraftServer server, boolean networkThread) {
        this.player = player;
        this.server = server;
        this.networkThread = networkThread;
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
        server.execute(() -> {
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
        PacketDistributor.sendToPlayer(player, payload);
    }
}
