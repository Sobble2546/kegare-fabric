package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.mixin.net.ServerCommonPacketListenerImplAccessor;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

/**
 * The connections that answered the configuration-phase hello with this server's network version (DESIGN §3.5). Kept by connection, not
 * by player id: a second login of the same player kicks the first, and the first one's late disconnect must not clear the second's entry.
 */
public final class KegareClients {
    private static final Set<Connection> READY = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private KegareClients() {}

    public static void add(Connection connection) {
        if (connection != null) READY.add(connection);
    }

    public static void remove(Connection connection) {
        if (connection != null) READY.remove(connection);
    }

    public static boolean has(Connection connection) {
        return connection != null && READY.contains(connection);
    }

    /** Has this player's connection passed the hello? */
    public static boolean has(ServerPlayer player) {
        return player != null && player.connection != null && has(connectionOf(player.connection));
    }

    /** The connection of a configuration or play listener ({@code connection} is protected). */
    static Connection connectionOf(ServerCommonPacketListenerImpl listener) {
        return ((ServerCommonPacketListenerImplAccessor) listener).kegare$connection();
    }

    public static void clear() {
        READY.clear();
    }

    static int size() {
        return READY.size();
    }
}
