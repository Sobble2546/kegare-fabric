package com.cnethuka.kegare.platform.event.tick;

import com.cnethuka.kegare.platform.bus.Event;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.Entity;

/** Around every entity's tick, on either side. */
public abstract class EntityTickEvent extends Event {
    private final Entity entity;

    protected EntityTickEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    /** Cancel: the entity does not tick this time. */
    public static final class Pre extends EntityTickEvent implements ICancellableEvent {
        public Pre(Entity entity) {
            super(entity);
        }
    }

    public static final class Post extends EntityTickEvent {
        public Post(Entity entity) {
            super(entity);
        }
    }
}
