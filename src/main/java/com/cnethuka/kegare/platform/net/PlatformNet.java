package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.platform.bus.IEventBus;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.slf4j.Logger;

/**
 * Networking's one entry point (DESIGN §3.5). {@link #init} runs once from the mod initializer, after the mod bus has its listeners
 * (KegareNetwork::register, LoginTask::register): it registers the hello, the presence gate, the sound and id handling, posts
 * {@link RegisterPayloadHandlersEvent} so every payload gets registered, and hooks every connection's configuration phase.
 */
public final class PlatformNet {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String VERSION_KEY = "kegare.network.version";
    static final String TIMEOUT_KEY = "kegare.network.timeout";

    private static volatile MinecraftServer server;
    private static boolean initialised;

    private PlatformNet() {}

    /** The running server (a dedicated one, or the integrated one), once a player has begun to join it; null before that. */
    public static MinecraftServer server() {
        return server;
    }

    static void setServer(MinecraftServer value) {
        server = value;
    }

    /** Finishes a configuration task of a connection (vanilla's {@code finishCurrentTask}, which is private). Main thread only. */
    public static void completeTask(ServerConfigurationPacketListenerImpl listener, ConfigurationTask task) {
        VersionCompat.completeConfigTask(listener, task.type());
    }

    public static synchronized void init(IEventBus modBus) {
        if (initialised) throw new IllegalStateException("PlatformNet.init ran twice");
        initialised = true;
        // the hello, in the configuration phase
        VersionCompat.registerConfigS2C(KegareHello.S2C.TYPE, KegareHello.S2C.STREAM_CODEC);
        VersionCompat.registerConfigC2S(KegareHello.C2S.TYPE, KegareHello.C2S.STREAM_CODEC);
        VersionCompat.registerHelloAckReceiver(HelloTask::answered);
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, srv) -> {
            server = srv;
            // A client that cannot receive the hello has no Kegare. Fabric's registry sync refuses most of them, but a Via server turns it off
            // (ignore-registry-sync-errors), and such a client would break later on a Kegare item or entity it cannot decode: refuse it here.
            if (VersionCompat.canSendConfig(handler, KegareHello.S2C.TYPE)) {
                VersionCompat.addConfigTask(handler, new HelloTask(handler, srv));
            } else {
                handler.disconnect(net.minecraft.network.chat.Component.translatable("kegare.network.missing"));
                return;
            }
            modBus.post(new RegisterConfigurationTasksEvent(handler, srv));
        });
        ServerConfigurationConnectionEvents.DISCONNECT.register((handler, srv) -> {
            HelloTask.forget(handler);
            KegareClients.remove(KegareClients.connectionOf(handler));
        });
        ServerPlayConnectionEvents.INIT.register((handler, srv) -> server = srv);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(srv -> { if (server == srv) server = null; });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> KegareClients.remove(KegareClients.connectionOf(handler)));
        // every payload
        modBus.post(new RegisterPayloadHandlersEvent(new FabricBackend()));
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.getEnvironmentType() == EnvType.CLIENT) {
            ClientHooks.init();
        } else {
            ViaWarning.check(loader);
        }
    }
}
