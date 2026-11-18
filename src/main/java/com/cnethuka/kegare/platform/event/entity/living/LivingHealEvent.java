package com.cnethuka.kegare.platform.event.entity.living;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;

/** {@code LivingEntity.heal} was called (cancel: nothing is healed). */
public final class LivingHealEvent extends LivingEvent implements ICancellableEvent {
    private float amount;

    public LivingHealEvent(LivingEntity entity, float amount) {
        super(entity);
        this.amount = amount;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
