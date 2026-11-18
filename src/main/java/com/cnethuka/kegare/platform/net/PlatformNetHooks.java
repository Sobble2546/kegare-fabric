package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.mixin.net.ServerCommonPacketListenerImplAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.slf4j.Logger;

/** What the server-side packet mixin calls; kept out of the mixin class, which cannot hold ordinary helpers. */
public final class PlatformNetHooks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PlatformNetHooks() {}

    /**
     * Runs a network-thread C2S payload's handler for the play listener it arrived on. Returns true if the payload is finished with (handled,
     * or dropped because the sender did not pass the hello); false to let it carry on (a configuration-phase packet).
     */
    public static boolean handleServerPayload(ServerCommonPacketListener listener, CustomPacketPayload payload) {
        if (!(listener instanceof ServerGamePacketListenerImpl game)) return false;
        ServerPlayer player = game.getPlayer();
        if (!KegareClients.has(player)) return true;
        try {
            NetworkThread.handleOnServer(payload, new ServerPayloadContext(player, ((ServerCommonPacketListenerImplAccessor) game).kegare$server(), true));
        } catch (Throwable t) {
            LOGGER.error("A {} payload from {} failed", payload.type().id(), player.getGameProfile().name(), t);
        }
        return true;
    }
}
