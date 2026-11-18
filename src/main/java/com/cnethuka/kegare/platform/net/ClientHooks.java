package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * The client's side of the hello. Client only: nothing outside {@link PlatformNet#init}'s client branch calls it, so a dedicated server
 * never loads this or {@link VersionCompatClient}.
 */
final class ClientHooks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientHooks() {}

    static void init() {
        VersionCompatClient.registerResetHooks(ClientNet::reset);
        VersionCompatClient.registerHelloReceiver((hello, responder) -> {
            String mine = com.cnethuka.kegare.network.KegareNetwork.VERSION;
            if (!mine.equals(hello.version())) {
                ClientNet.reset();
                responder.disconnect(Component.translatableWithFallback(PlatformNet.VERSION_KEY,
                        "Kegare network version mismatch: the server uses %s, your client uses %s. Use the same Kegare version as the server.",
                        hello.version(), mine));
                return;
            }
            IdRemap.install(hello.entries(), new LocalRegistries());
            ClientNet.setActive(true);
            LOGGER.info("Kegare server found (network version {}){}", hello.version(), IdRemap.isActive() ? ", remapping its ids" : "");
            responder.send(new KegareHello.C2S(mine));
        });
    }
}
