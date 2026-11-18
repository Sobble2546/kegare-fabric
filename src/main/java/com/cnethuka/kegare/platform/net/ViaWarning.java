package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * A dedicated server with ViaFabric (so older clients can join) needs ViaFabric's {@code ignore-registry-sync-errors: true}: otherwise
 * Fabric's registry sync, which lists every item, entity type and sound event, disconnects a client whose Minecraft version lacks some
 * of the server's vanilla entries (DESIGN §3.5). Warn at startup if it is not set.
 */
final class ViaWarning {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern FLAG = Pattern.compile("^\\s*ignore-registry-sync-errors\\s*:\\s*(\\S+)", Pattern.MULTILINE);

    private ViaWarning() {}

    static void check(FabricLoader loader) {
        if (!loader.isModLoaded("viafabric")) return;
        Path file = loader.getConfigDir().resolve("ViaFabric").resolve("viafabric.yml");
        String text = null;
        try {
            if (Files.isRegularFile(file)) text = Files.readString(file);
        } catch (IOException e) {
            LOGGER.debug("Could not read {}", file, e);
        }
        if (needsWarning(true, text)) {
            LOGGER.warn("ViaFabric is installed but 'ignore-registry-sync-errors' is not true in config/ViaFabric/viafabric.yml. Clients on another "
                    + "Minecraft version than this server will be disconnected during login (\"registry entries that are unknown to this client\"). "
                    + "Set it to true and restart. Kegare's own check of the client's version still applies.");
        }
    }

    /** True if ViaFabric is loaded and its config (null: absent, which means the default, false) does not turn the flag on. */
    static boolean needsWarning(boolean viaLoaded, String yaml) {
        if (!viaLoaded) return false;
        if (yaml == null) return true;
        Matcher m = FLAG.matcher(yaml);
        return !(m.find() && m.group(1).replace("\"", "").replace("'", "").equalsIgnoreCase("true"));
    }
}
