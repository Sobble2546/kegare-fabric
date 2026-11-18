package com.cnethuka.kegare.platform.net;

import java.util.Collection;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * NeoForge's PacketDistributor, on Fabric, with the presence gate on every send (DESIGN §3.5): a payload only goes to a player who
 * {@link Gate#ok passes it}, and a client only sends while the server said it runs Kegare ({@link ClientNet#isActive()}).
 */
public final class PacketDistributor {
    private PacketDistributor() {}

    public static void sendToPlayer(@Nullable ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... more) {
        if (player == null) return;
        send(player, payload);
        for (CustomPacketPayload extra : more) send(player, extra);
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... more) {
        send(payload);
        for (CustomPacketPayload extra : more) send(extra);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... more) {
        MinecraftServer server = PlatformNet.server();
        if (server != null) sendToAll(PlayerLookup.all(server), payload, more);
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... more) {
        sendToAll(level.players(), payload, more); // PlayerLookup.world is gone on 26.x
    }

    /** Players within {@code distance} of a point, except {@code excluded} (may be null). */
    public static void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z, double distance,
                                         CustomPacketPayload payload, CustomPacketPayload... more) {
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), distance)) {
            if (player == excluded) continue;
            send(player, payload);
            for (CustomPacketPayload extra : more) send(player, extra);
        }
    }

    /** Players who have the entity in their tracking range (not the entity itself, if it is a player). */
    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... more) {
        sendToAll(PlayerLookup.tracking(entity), payload, more);
    }

    /** Players tracking the entity, and the entity itself if it is a player. */
    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... more) {
        Collection<ServerPlayer> tracking = PlayerLookup.tracking(entity);
        sendToAll(tracking, payload, more);
        if (entity instanceof ServerPlayer self && !tracking.contains(self)) sendToPlayer(self, payload, more);
    }

    private static void sendToAll(Collection<ServerPlayer> players, CustomPacketPayload payload, CustomPacketPayload[] more) {
        for (ServerPlayer player : players) {
            send(player, payload);
            for (CustomPacketPayload extra : more) send(player, extra);
        }
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (Gate.ok(player, payload.type())) ServerPlayNetworking.send(player, payload);
    }

    private static void send(CustomPacketPayload payload) {
        if (ClientNet.isActive() && VersionCompatClient.canSendToServer(payload.type())) VersionCompatClient.sendToServer(payload);
    }
}
