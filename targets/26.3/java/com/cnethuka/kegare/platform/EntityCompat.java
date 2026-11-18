package com.cnethuka.kegare.platform;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 26.x twin of the 1.21.11 file. UNVERIFIED against a 26.x compile: the map (cross-check item 3) found that the hit cooldown is
 * {@code LivingEntity.damageCooldownTime} there ({@code Entity.invulnerableTime} is private and unrelated), and that {@code hurtMarked}
 * became {@code syncVelocity}, set by the protected {@code markHurt()}.
 */
public final class EntityCompat {
    private EntityCompat() {}

    public static int invulnerableTime(Entity entity) {
        return entity instanceof LivingEntity living ? living.damageCooldownTime : 0;
    }

    public static void setInvulnerableTime(Entity entity, int ticks) {
        if (entity instanceof LivingEntity living) living.damageCooldownTime = ticks;
    }

    /** {@code Entity.markHurt()} is protected on 26.x; its whole body is this public flag (verified with javap on the 26.3 jar). */
    public static void markHurt(Entity entity) {
        entity.syncVelocity = true;
    }
}
