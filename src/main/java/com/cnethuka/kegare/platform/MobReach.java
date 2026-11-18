package com.cnethuka.kegare.platform;

import com.cnethuka.kegare.mixin.MobAttackBoxInvoker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.AABB;

/** How far a mob's melee reaches in 1.21.11: its held weapon's attack range, or the mob default. The box is vanilla's own, overrides included. */
public final class MobReach {
    private MobReach() {}

    /** Vanilla's {@code Mob.DEFAULT_ATTACK_REACH} (private there): sqrt(2.04) - 0.6. */
    private static final double DEFAULT_REACH = Math.sqrt(2.04) - 0.6;

    public static double reach(Mob mob) {
        AttackRange range = mob.getMainHandItem().get(DataComponents.ATTACK_RANGE);
        return range == null ? DEFAULT_REACH : range.effectiveMaxRange(mob);
    }

    public static AABB attackBox(Mob mob) {
        return ((MobAttackBoxInvoker) mob).kegare$attackBox(reach(mob));
    }
}
