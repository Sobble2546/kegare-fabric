package com.cnethuka.kegare.platform.event.tick;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.world.entity.player.Player;

/** Around every player's tick, on either side. */
public abstract class PlayerTickEvent extends Event {
    private final Player player;

    protected PlayerTickEvent(Player player) {
        this.player = player;
    }

    public Player getEntity() {
        return player;
    }

    public static final class Pre extends PlayerTickEvent {
        public Pre(Player player) {
            super(player);
        }
    }

    public static final class Post extends PlayerTickEvent {
        public Post(Player player) {
            super(player);
        }
    }
}
