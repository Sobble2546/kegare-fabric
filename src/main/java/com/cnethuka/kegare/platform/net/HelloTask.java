package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.network.KegareNetwork;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.slf4j.Logger;

/**
 * The server's half of the hello (DESIGN §3.5): sends the version and id table, and holds the connection in the configuration phase until
 * the client answers with the same version. A client that never answers is let go after {@link #TIMEOUT_SECONDS}.
 */
final class HelloTask implements ConfigurationTask {
    static final Type TYPE = new Type("kegare:hello");
    static final int TIMEOUT_SECONDS = 30;
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Listeners with a hello out and no answer yet, so a stray answer is ignored instead of finishing someone else's task. */
    private static final Set<ServerConfigurationPacketListenerImpl> AWAITING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final ServerConfigurationPacketListenerImpl listener;
    private final MinecraftServer server;

    HelloTask(ServerConfigurationPacketListenerImpl listener, MinecraftServer server) {
        this.listener = listener;
        this.server = server;
    }

    @Override
    public Type type() {
        return TYPE;
    }

    @Override
    public void start(Consumer<Packet<?>> sender) {
        AWAITING.add(listener);
        sender.accept(VersionCompat.createConfigS2CPacket(new KegareHello.S2C(KegareNetwork.VERSION, HelloTable.entries())));
        CompletableFuture.delayedExecutor(TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> server.execute(() -> {
            if (AWAITING.remove(listener)) {
                LOGGER.warn("{} did not answer Kegare's hello in {} s", listener.getOwner().name(), TIMEOUT_SECONDS);
                listener.disconnect(Component.translatableWithFallback(PlatformNet.TIMEOUT_KEY, "The Kegare handshake timed out."));
            }
        }));
    }

    /** The connection ended (or was refused) while the hello was out: nothing to wait for. */
    static void forget(ServerConfigurationPacketListenerImpl listener) {
        AWAITING.remove(listener);
    }

    /** The client's answer, on the main thread. */
    static void answered(KegareHello.C2S answer, ServerConfigurationPacketListenerImpl listener) {
        if (!AWAITING.remove(listener)) return;
        if (!KegareNetwork.VERSION.equals(answer.version())) {
            listener.disconnect(Component.translatableWithFallback(PlatformNet.VERSION_KEY,
                    "Kegare network version mismatch: the server uses %s, your client uses %s. Use the same Kegare version as the server.",
                    KegareNetwork.VERSION, answer.version()));
            return;
        }
        KegareClients.add(KegareClients.connectionOf(listener));
        VersionCompat.completeConfigTask(listener, TYPE);
    }
}
