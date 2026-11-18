package com.cnethuka.kegare.platform.event.entity.living;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** A living entity is dying (cancel: it lives on). Fired once per death. */
public final class LivingDeathEvent extends LivingEvent implements ICancellableEvent {
    private final DamageSource source;

    public LivingDeathEvent(LivingEntity entity, DamageSource source) {
        super(entity);
        this.source = source;
    }

    public DamageSource getSource() {
        return source;
    }
}
