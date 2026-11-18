package com.cnethuka.kegare.platform.event.entity.living;

import com.cnethuka.kegare.platform.event.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

/** Something that happened to a living entity. */
public abstract class LivingEvent extends EntityEvent {
    private final LivingEntity living;

    protected LivingEvent(LivingEntity entity) {
        super(entity);
        this.living = entity;
    }

    @Override
    public LivingEntity getEntity() {
        return living;
    }
}
