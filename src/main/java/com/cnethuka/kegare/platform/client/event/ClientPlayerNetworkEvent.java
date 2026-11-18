package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

/** The local player or its connection coming, going or being replaced. */
public abstract class ClientPlayerNetworkEvent extends Event {
    @Nullable
    private final LocalPlayer player;
    @Nullable
    private final Connection connection;

    private ClientPlayerNetworkEvent(@Nullable LocalPlayer player, @Nullable Connection connection) {
        this.player = player;
        this.connection = connection;
    }

    /** The player this is about; null if the client had none (a connection that dropped before the player existed). */
    @Nullable
    public LocalPlayer getPlayer() {
        return player;
    }

    @Nullable
    public Connection getConnection() {
        return connection;
    }

    /** The client joined a world. */
    public static final class LoggingIn extends ClientPlayerNetworkEvent {
        public LoggingIn(@Nullable LocalPlayer player, @Nullable Connection connection) {
            super(player, connection);
        }
    }

    /** The client left a world (or lost the connection). Fired for every disconnect, whether or not the server had Kegare. */
    public static final class LoggingOut extends ClientPlayerNetworkEvent {
        public LoggingOut(@Nullable LocalPlayer player, @Nullable Connection connection) {
            super(player, connection);
        }
    }

    /** The local player object was replaced (respawn or dimension change); both are alive objects of the same connection. */
    public static final class Clone extends ClientPlayerNetworkEvent {
        private final LocalPlayer oldPlayer;

        public Clone(LocalPlayer oldPlayer, LocalPlayer newPlayer, @Nullable Connection connection) {
            super(newPlayer, connection);
            this.oldPlayer = oldPlayer;
        }

        public LocalPlayer getOldPlayer() {
            return oldPlayer;
        }

        public LocalPlayer getNewPlayer() {
            return getPlayer();
        }
    }
}
