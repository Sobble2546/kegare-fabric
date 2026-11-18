package com.cnethuka.kegare.sixeyes;

import net.minecraft.world.entity.LivingEntity;

/**
 * 26.x: no eyewear slot yet. Trinkets 4.x (the 26.x line) has a different API and slot data format from the 3.x one the 1.21.11 file
 * uses, and that port is not done; {@link #SUPPORTED} keeps {@link Eyewear#slotAvailable()} false so nothing offers the slot.
 */
final class EyewearSlots {
    private EyewearSlots() {}

    static final boolean SUPPORTED = false;

    static Eyewear worn(LivingEntity entity) {
        return Eyewear.NONE;
    }
}
