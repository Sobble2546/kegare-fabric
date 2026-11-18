package com.cnethuka.kegare.platform;

import net.minecraft.world.entity.Entity;

/**
 * The entity fields 26.x hides or renames. This is the 1.21.11 file (plain fields); targets/26.3/java has the twin with the same
 * signatures. Kegare code that read {@code e.invulnerableTime} / {@code e.hurtMarked} calls these instead.
 */
public final class EntityCompat {
    private EntityCompat() {}

    /** The hurt cooldown: ticks left in which a second hit only counts for what it adds to the first. */
    public static int invulnerableTime(Entity entity) {
        return entity.invulnerableTime;
    }

    public static void setInvulnerableTime(Entity entity, int ticks) {
        entity.invulnerableTime = ticks;
    }

    /** Tells the client its own movement changed (a player moves itself): after setting a velocity on something that can be a player. */
    public static void markHurt(Entity entity) {
        entity.hurtMarked = true;
    }
}
