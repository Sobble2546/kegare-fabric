package com.cnethuka.kegare.sixeyes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 26.x: there is no eyewear slot yet (see sixeyes.EyewearSlots for this target), so the tests that need one fail at their setup check. */
public final class TestEyewear {
    private TestEyewear() {}

    public static boolean hasSlot(LivingEntity entity) {
        return false;
    }

    public static void wear(LivingEntity entity, ItemStack stack) {
        throw new IllegalStateException("no eyewear slot on this target");
    }
}
