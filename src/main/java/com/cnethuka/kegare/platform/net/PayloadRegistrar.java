package com.cnethuka.kegare.platform.net;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * NeoForge's registrar, on Fabric: {@code playToServer} / {@code playToClient} register the codec and the handler. Registering the same
 * payload type twice does nothing (KegareNetwork asks for two registrars); two different types on one channel id is a bug and throws.
 * <p>
 * A {@link HandlerThread#NETWORK} registrar also hands the handler to {@link NetworkThread}, which runs it as the packet arrives
 * (DESIGN §3.29). The ordinary receiver stays registered as well: it keeps the channel advertised (so the gate can see it) and is the
 * fallback, on the main thread, if the network-thread hook never fires.
 */
public final class PayloadRegistrar {
    /** What registering really does; Fabric's in {@link FabricBackend}, a recorder in tests. */
    public interface Backend {
        <T extends CustomPacketPayload> void registerPlayS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

        <T extends CustomPacketPayload> void registerPlayC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

        /** The main-thread receiver on the server. */
        <T extends CustomPacketPayload> void serverReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler);

        /** The main-thread receiver on the client. Does nothing on a dedicated server. */
        <T extends CustomPacketPayload> void clientReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler);

        <T extends CustomPacketPayload> void registerConfigS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec);

        <T extends CustomPacketPayload> void registerConfigC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec);

        /** The configuration-phase receiver on the server (main thread). */
        <T extends CustomPacketPayload> void configServerReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler);

        /** The configuration-phase receiver on the client. Does nothing on a dedicated server. */
        <T extends CustomPacketPayload> void configClientReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler);
    }

    private static final Map<String, CustomPacketPayload.Type<?>> CLAIMED = new HashMap<>();

    private final Backend backend;
    private final HandlerThread thread;

    PayloadRegistrar(Backend backend, HandlerThread thread) {
        this.backend = backend;
        this.thread = thread;
    }

    /** The same registrar, but its handlers run on the connection's thread. */
    public PayloadRegistrar executesOn(HandlerThread thread) {
        return new PayloadRegistrar(backend, thread);
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type,
                                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        if (!claim(type, "c2s")) return this;
        backend.registerPlayC2S(type, codec);
        if (thread == HandlerThread.NETWORK) {
            NetworkThread.registerServer(type, handler);
            backend.serverReceiver(type, NetworkThread.fallback(type, handler));
        } else {
            backend.serverReceiver(type, handler);
        }
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type,
                                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        if (!claim(type, "s2c")) return this;
        backend.registerPlayS2C(type, codec);
        if (thread == HandlerThread.NETWORK) {
            NetworkThread.registerClient(type, handler);
            backend.clientReceiver(type, NetworkThread.fallback(type, handler));
        } else {
            backend.clientReceiver(type, handler);
        }
        return this;
    }

    /**
     * A configuration-phase payload, server to client (what an {@link ICustomConfigurationTask} sends). Its handler runs on the main
     * thread whatever {@link #executesOn} said, and gets no player ({@link IPayloadContext#player} is null).
     */
    public <T extends CustomPacketPayload> PayloadRegistrar configurationToClient(CustomPacketPayload.Type<T> type,
                                                                                  StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        if (!claim(type, "config s2c")) return this;
        backend.registerConfigS2C(type, codec);
        backend.configClientReceiver(type, handler);
        return this;
    }

    /**
     * A configuration-phase payload, client to server. Its handler runs on the main thread, only for a connection that passed the hello,
     * and may {@link IPayloadContext#finishCurrentTask finish} the task or {@link IPayloadContext#disconnect disconnect}.
     */
    public <T extends CustomPacketPayload> PayloadRegistrar configurationToServer(CustomPacketPayload.Type<T> type,
                                                                                  StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        if (!claim(type, "config c2s")) return this;
        backend.registerConfigC2S(type, codec);
        backend.configServerReceiver(type, handler);
        return this;
    }

    private static synchronized boolean claim(CustomPacketPayload.Type<?> type, String direction) {
        String key = direction + " " + type.id();
        CustomPacketPayload.Type<?> before = CLAIMED.putIfAbsent(key, type);
        if (before == null) return true;
        if (before != type) throw new IllegalStateException("Two different payload types on the channel " + type.id() + " (" + direction + ")");
        return false;
    }

    /** For tests only: forgets that this payload type was registered, in both directions (and only this one). */
    static synchronized void forgetForTests(CustomPacketPayload.Type<?> type) {
        CLAIMED.remove("c2s " + type.id());
        CLAIMED.remove("s2c " + type.id());
        CLAIMED.remove("config c2s " + type.id());
        CLAIMED.remove("config s2c " + type.id());
        NetworkThread.forgetForTests(type);
    }
}
