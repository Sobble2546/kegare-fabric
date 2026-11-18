package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.mixin.net.client.ClientCommonPacketListenerImplAccessor;
import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * The client's side of the hello. Client only: nothing outside {@link PlatformNet#init}'s client branch calls it, so a dedicated server
 * never loads this or {@link VersionCompatClient}. What the hello switches on ({@link ClientNet}, {@link IdRemap}) belongs to the connection
 * it came on, so a late disconnect of an older connection cannot switch it off.
 */
final class ClientHooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String FAILED_KEY = "kegare.network.hello_failed";

    private ClientHooks() {}

    /** The connection of a configuration or play listener ({@code connection} is protected). */
    static Connection connectionOf(ClientCommonPacketListenerImpl listener) {
        return ((ClientCommonPacketListenerImplAccessor) listener).kegare$connection();
    }

    static void init() {
        VersionCompatClient.registerResetHooks(connection -> ClientNet.reset(), ClientNet::resetIfOwner);
        VersionCompatClient.registerHelloReceiver(ClientHooks::onHello);
    }

    static void onHello(KegareHello.S2C hello, VersionCompatClient.Responder responder) {
        String mine = com.cnethuka.kegare.network.KegareNetwork.VERSION;
        if (!mine.equals(hello.version())) {
            responder.disconnect(Component.translatableWithFallback(PlatformNet.VERSION_KEY,
                    "Kegare network version mismatch: the server uses %s, your client uses %s. Use the same Kegare version as the server.",
                    hello.version(), mine));
            return;
        }
        try {
            // The table, the ack and (last) the switch-on are one step: if any of it fails nothing stays on, and the server hears nothing.
            ClientNet.activate(responder.connection(), () -> {
                IdRemap.install(hello.entries(), new LocalRegistries());
                if (IdRemap.isActive() && !IdRemap.isHooked()) {
                    throw new IllegalStateException("this server numbers Kegare's entries differently and the id-codec mixin did not apply, "
                            + "so its entities and items would not decode");
                }
            }, () -> responder.send(new KegareHello.C2S(mine)));
        } catch (RuntimeException e) {
            LOGGER.error("Kegare could not answer the server's hello", e);
            responder.disconnect(Component.translatableWithFallback(FAILED_KEY,
                    "Kegare could not join this server: %s (see the log).", String.valueOf(e.getMessage())));
            return;
        }
        LOGGER.info("Kegare server found (network version {}){}", hello.version(), IdRemap.isActive() ? ", remapping its ids" : "");
    }
}
