package com.cnethuka.kegare.platform.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers payloads with Fabric. Server receivers drop anything from a connection that has not passed the hello (a modless proxy or a
 * hand-made client sending raw Kegare packets). Client receivers are only registered on a client, through {@link VersionCompatClient},
 * which a dedicated server never loads: Fabric advertises the client's registered receivers to the server, and that is what
 * {@link Gate} reads.
 */
final class FabricBackend implements PayloadRegistrar.Backend {
    private static final boolean CLIENT = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;

    @Override
    public <T extends CustomPacketPayload> void registerPlayS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        VersionCompat.registerPlayS2C(type, codec);
    }

    @Override
    public <T extends CustomPacketPayload> void registerPlayC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        VersionCompat.registerPlayC2S(type, codec);
    }

    @Override
    public <T extends CustomPacketPayload> void serverReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!KegareClients.has(player)) return;
            handler.handle(payload, new ServerPayloadContext(player, context.server(), false));
        });
    }

    @Override
    public <T extends CustomPacketPayload> void clientReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        if (CLIENT) VersionCompatClient.registerPlayReceiver(type, handler);
    }

    @Override
    public <T extends CustomPacketPayload> void registerConfigS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        VersionCompat.registerConfigS2C(type, codec);
    }

    @Override
    public <T extends CustomPacketPayload> void registerConfigC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        VersionCompat.registerConfigC2S(type, codec);
    }

    @Override
    public <T extends CustomPacketPayload> void configServerReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        VersionCompat.registerConfigReceiver(type, (payload, listener) -> {
            if (!KegareClients.has(KegareClients.connectionOf(listener))) return; // only after the hello
            handler.handle(payload, new ServerConfigPayloadContext(listener));
        });
    }

    @Override
    public <T extends CustomPacketPayload> void configClientReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        if (CLIENT) VersionCompatClient.registerConfigReceiver(type, (payload, responder) -> handler.handle(payload, new ClientConfigPayloadContext(responder)));
    }
}
