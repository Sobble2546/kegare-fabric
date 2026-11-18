package com.cnethuka.kegare.platform;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

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

    /** True on the tick a swing starts (the arm has not moved yet). */
    public static boolean swingStarted(LivingEntity entity) {
        return entity.swinging && entity.swingTime <= 0;
    }

    public static InteractionHand swingHand(LivingEntity entity) {
        return entity.swingingArm == null ? InteractionHand.MAIN_HAND : entity.swingingArm;
    }

    public static void swing(LivingEntity entity, InteractionHand hand) {
        entity.swing(hand);
    }

    /** Vanilla knockback: strength, then the direction the hit came FROM (x, z). */
    public static void knockback(LivingEntity entity, double strength, double x, double z) {
        entity.knockback(strength, x, z);
    }

    public static long dayTime(Level level) {
        return level.getDayTime();
    }

    /** Puts a stack back in a player inventory (dropping what does not fit), as the server. */
    public static void giveBack(net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.item.ItemStack stack) {
        inventory.placeItemBackInInventory(stack);
    }

    public static boolean isSwinging(LivingEntity entity) {
        return entity.swinging;
    }

    /** Ends a swing at once (tests reset an attacker between cases). */
    public static void clearSwing(LivingEntity entity) {
        entity.swinging = false;
        entity.swingTime = 0;
    }
}
