package com.cnethuka.kegare.platform.net;

import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationNetworkHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

/**
 * Every Fabric API / Minecraft spelling that differs between targets, for the server and common side. This file is the 1.21.11 one;
 * targets/26.3/java has the 26.x twin with the same signatures. Nothing else in platform.net names these APIs.
 */
final class VersionCompat {
    private VersionCompat() {}

    static <T extends CustomPacketPayload> void registerPlayS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playS2C().register(type, codec);
    }

    static <T extends CustomPacketPayload> void registerPlayC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
    }

    static <T extends CustomPacketPayload> void registerConfigS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.configurationS2C().register(type, codec);
    }

    static <T extends CustomPacketPayload> void registerConfigC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.configurationC2S().register(type, codec);
    }

    /** A packet carrying a configuration-phase payload to the client. */
    static Packet<ClientCommonPacketListener> createConfigS2CPacket(CustomPacketPayload payload) {
        return ServerConfigurationNetworking.createS2CPacket(payload);
    }

    /** Can the client behind this configuration listener receive the payload (is its channel registered)? */
    static boolean canSendConfig(ServerConfigurationPacketListenerImpl listener, CustomPacketPayload.Type<?> type) {
        return ServerConfigurationNetworking.canSend(listener, type);
    }

    static void addConfigTask(ServerConfigurationPacketListenerImpl listener, ConfigurationTask task) {
        ((FabricServerConfigurationNetworkHandler) (Object) listener).addTask(task);
    }

    /** Vanilla's {@code finishCurrentTask}, which is private. */
    static void completeConfigTask(ServerConfigurationPacketListenerImpl listener, ConfigurationTask.Type type) {
        ((FabricServerConfigurationNetworkHandler) (Object) listener).completeTask(type);
    }

    /** The server's receiver of the client's hello answer (main thread). */
    static void registerHelloAckReceiver(BiConsumer<KegareHello.C2S, ServerConfigurationPacketListenerImpl> receiver) {
        ServerConfigurationNetworking.registerGlobalReceiver(KegareHello.C2S.TYPE, (payload, context) -> receiver.accept(payload, context.networkHandler()));
    }
}
