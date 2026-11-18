package com.cnethuka.kegare.platform.event.entity;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** An entity is about to be moved to another dimension (cancel: it stays). */
public final class EntityTravelToDimensionEvent extends EntityEvent implements ICancellableEvent {
    private final ResourceKey<Level> dimension;

    public EntityTravelToDimensionEvent(Entity entity, ResourceKey<Level> dimension) {
        super(entity);
        this.dimension = dimension;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}
