package com.cnethuka.kegare.platform.event.entity.living;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;

/** A living entity is about to be knocked back (cancel: it isn't). */
public final class LivingKnockBackEvent extends LivingEvent implements ICancellableEvent {
    private double strength, ratioX, ratioZ;

    public LivingKnockBackEvent(LivingEntity entity, double strength, double ratioX, double ratioZ) {
        super(entity);
        this.strength = strength;
        this.ratioX = ratioX;
        this.ratioZ = ratioZ;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public double getRatioX() {
        return ratioX;
    }

    public void setRatioX(double ratioX) {
        this.ratioX = ratioX;
    }

    public double getRatioZ() {
        return ratioZ;
    }

    public void setRatioZ(double ratioZ) {
        this.ratioZ = ratioZ;
    }
}
