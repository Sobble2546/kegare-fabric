package com.cnethuka.kegare.platform.event.entity.living;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import com.cnethuka.kegare.platform.damage.DamageContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * A hit has got past the early outs (already dead, fire resistance) and is about to go through the defences: change the amount, change
 * the hurt cooldown it starts, or cancel it. The first of the damage events.
 */
public final class LivingIncomingDamageEvent extends LivingEvent implements ICancellableEvent {
    private final DamageContainer container;

    public LivingIncomingDamageEvent(LivingEntity entity, DamageContainer container) {
        super(entity);
        this.container = container;
    }

    public DamageContainer getContainer() {
        return container;
    }

    public DamageSource getSource() {
        return container.getSource();
    }

    public float getAmount() {
        return container.getNewDamage();
    }

    public void setAmount(float amount) {
        container.setNewDamage(amount);
    }

    public void setInvulnerabilityTicks(int ticks) {
        container.setPostAttackInvulnerabilityTicks(ticks);
    }
}
