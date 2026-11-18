package com.cnethuka.kegare.platform.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kegare payloads only go to players whose client can take them: the channel is registered there (Fabric's canSend) and the hello
 * matched (DESIGN §3.5). A client without the mod, or on another network version, is never sent anything.
 */
public final class Gate {
    private Gate() {}

    public static boolean ok(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return KegareClients.has(player) && ServerPlayNetworking.canSend(player, type);
    }
}
