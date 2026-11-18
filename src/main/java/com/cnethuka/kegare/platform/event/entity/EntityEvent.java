package com.cnethuka.kegare.platform.event.entity;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Something that happened to an entity. */
public abstract class EntityEvent extends Event {
    private final Entity entity;

    protected EntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public Level getLevel() {
        return entity.level();
    }
}
