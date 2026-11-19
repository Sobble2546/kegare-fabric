package com.cnethuka.kegare.platform.net;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The client-only twin of {@link VersionCompat}; 26.x spellings. Only ever loaded on a client, so it may name client classes.
 */
final class VersionCompatClient {
    private VersionCompatClient() {}

    /** What a configuration-phase handler can do to answer. */
    interface Responder {
        void send(CustomPacketPayload payload);

        void disconnect(Component reason);

        /** The connection the hello came on: what the client's Kegare state is scoped to. */
        Connection connection();
    }

    static <T extends CustomPacketPayload> void registerPlayReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.handle(payload, new ClientPayloadContext(context.client(), context.player(), false, null)));
    }

    static void registerHelloReceiver(BiConsumer<KegareHello.S2C, Responder> receiver) {
        registerConfigReceiver(KegareHello.S2C.TYPE, receiver);
    }

    /** A client receiver of a configuration-phase payload; answers go back on the connection being configured. */
    static <T extends CustomPacketPayload> void registerConfigReceiver(CustomPacketPayload.Type<T> type, BiConsumer<T, Responder> receiver) {
        ClientConfigurationNetworking.registerGlobalReceiver(type, (payload, context) -> receiver.accept(payload, new Responder() {
            @Override
            public void send(CustomPacketPayload answer) {
                context.responseSender().sendPacket(answer);
            }

            @Override
            public void disconnect(Component reason) {
                context.responseSender().disconnect(reason);
            }

            @Override
            public Connection connection() {
                return ClientHooks.connectionOf(context.packetListener());
            }
        }));
    }

    /**
     * {@code started} runs for the connection that is beginning to configure, {@code ended} for each one that closes. The close callbacks can
     * arrive late (Fabric fires them from the netty thread as well), after a newer connection has already started, which is why they name
     * their connection.
     */
    static void registerResetHooks(Consumer<Connection> started, Consumer<Connection> ended) {
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> started.accept(ClientHooks.connectionOf(handler)));
        ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) -> ended.accept(ClientHooks.connectionOf(handler)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ended.accept(handler.getConnection()));
    }

    static Packet<ServerCommonPacketListener> createC2SPacket(CustomPacketPayload payload) {
        return ClientPlayNetworking.createServerboundPacket(payload);
    }

    static boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        return ClientPlayNetworking.canSend(type);
    }

    static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
