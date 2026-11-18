package com.cnethuka.kegare.sixeyes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** What the tests need of the Trinkets slot (the 26.x target has its own copy): put eyewear on someone, and ask if they have the slot. */
public final class TestEyewear {
    private TestEyewear() {}

    public static boolean hasSlot(LivingEntity entity) {
        return EyewearSlots.inventory(entity) != null;
    }

    public static void wear(LivingEntity entity, ItemStack stack) {
        EyewearSlots.inventory(entity).setItem(0, stack);
    }
}
