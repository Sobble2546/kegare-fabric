package com.cnethuka.kegare.platform.net;

import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
    }

    static <T extends CustomPacketPayload> void registerPlayReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.handle(payload, new ClientPayloadContext(context.client(), context.player(), false, null)));
    }

    static void registerHelloReceiver(BiConsumer<KegareHello.S2C, Responder> receiver) {
        ClientConfigurationNetworking.registerGlobalReceiver(KegareHello.S2C.TYPE, (payload, context) -> receiver.accept(payload, new Responder() {
            @Override
            public void send(CustomPacketPayload answer) {
                context.responseSender().sendPacket(answer);
            }

            @Override
            public void disconnect(Component reason) {
                context.responseSender().disconnect(reason);
            }
        }));
    }

    /** Runs the callback whenever a connection starts configuring and whenever one ends (so the hello's effects never outlive a connection). */
    static void registerResetHooks(Runnable reset) {
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> reset.run());
        ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) -> reset.run());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset.run());
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
