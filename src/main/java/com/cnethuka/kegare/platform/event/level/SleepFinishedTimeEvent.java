package com.cnethuka.kegare.platform.event.level;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.world.level.LevelAccessor;

/** Enough players slept: the night is skipped. Fired just before they are woken. */
public final class SleepFinishedTimeEvent extends Event {
    private final LevelAccessor level;
    private final long newTime;

    public SleepFinishedTimeEvent(LevelAccessor level, long newTime) {
        this.level = level;
        this.newTime = newTime;
    }

    public LevelAccessor getLevel() {
        return level;
    }

    public long getNewTime() {
        return newTime;
    }
}
