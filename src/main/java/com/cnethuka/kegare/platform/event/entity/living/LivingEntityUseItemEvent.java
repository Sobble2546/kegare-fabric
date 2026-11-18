package com.cnethuka.kegare.platform.event.entity.living;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Using an item (eating, drawing a bow, blocking), on either side. */
public abstract class LivingEntityUseItemEvent extends LivingEvent {
    private final ItemStack item;
    private int duration;

    protected LivingEntityUseItemEvent(LivingEntity entity, ItemStack item, int duration) {
        super(entity);
        this.item = item;
        this.duration = duration;
    }

    public ItemStack getItem() {
        return item;
    }

    /** Ticks of use left. */
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /** Someone starts using an item (cancel: they don't). */
    public static final class Start extends LivingEntityUseItemEvent implements ICancellableEvent {
        public Start(LivingEntity entity, ItemStack item, int duration) {
            super(entity, item, duration);
        }
    }

    /** Every tick of use; the listener may change the ticks left. */
    public static final class Tick extends LivingEntityUseItemEvent {
        public Tick(LivingEntity entity, ItemStack item, int duration) {
            super(entity, item, duration);
        }
    }
}
