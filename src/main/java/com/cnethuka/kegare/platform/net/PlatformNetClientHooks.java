package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.slf4j.Logger;

/** What the client-side packet mixin calls. Client only. */
public final class PlatformNetClientHooks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PlatformNetClientHooks() {}

    /** Runs a network-thread S2C payload's handler. Returns true if the payload is finished with; false to let it carry on. */
    public static boolean handleClientPayload(ClientCommonPacketListener listener, CustomPacketPayload payload) {
        if (!(listener instanceof ClientPacketListener play)) return false;
        Minecraft client = Minecraft.getInstance();
        try {
            NetworkThread.handleOnClient(payload, new ClientPayloadContext(client, client.player, true, play::send));
        } catch (Throwable t) {
            LOGGER.error("A {} payload failed", payload.type().id(), t);
        }
        return true;
    }
}
