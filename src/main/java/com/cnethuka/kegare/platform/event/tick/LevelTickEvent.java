package com.cnethuka.kegare.platform.event.tick;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.world.level.Level;

/** Around every level's tick, on either side. */
public abstract class LevelTickEvent extends Event {
    private final Level level;

    protected LevelTickEvent(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public static final class Pre extends LevelTickEvent {
        public Pre(Level level) {
            super(level);
        }
    }

    public static final class Post extends LevelTickEvent {
        public Post(Level level) {
            super(level);
        }
    }
}
