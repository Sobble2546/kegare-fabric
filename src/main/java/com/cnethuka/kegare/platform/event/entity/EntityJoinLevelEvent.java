package com.cnethuka.kegare.platform.event.entity;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** An entity is about to be added to a level (cancel: it never joins). Fired on both sides. */
public final class EntityJoinLevelEvent extends EntityEvent implements ICancellableEvent {
    private final Level level;
    private final boolean loadedFromDisk;

    public EntityJoinLevelEvent(Entity entity, Level level) {
        this(entity, level, false);
    }

    public EntityJoinLevelEvent(Entity entity, Level level, boolean loadedFromDisk) {
        super(entity);
        this.level = level;
        this.loadedFromDisk = loadedFromDisk;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    public boolean loadedFromDisk() {
        return loadedFromDisk;
    }
}
