package com.cnethuka.kegare.platform;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
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

    /** True on the tick a swing starts. 26.x keeps the tick count private; the animation progress is still 0 on that tick. UNVERIFIED in game. */
    public static boolean swingStarted(LivingEntity entity) {
        return entity.isSwinging() && entity.getSwingAnimation(1f) <= 0f;
    }

    public static InteractionHand swingHand(LivingEntity entity) {
        LivingEntity.SwingDescription swing = entity.getCurrentSwing();
        return swing == null ? InteractionHand.MAIN_HAND : swing.hand();
    }

    public static void swing(LivingEntity entity, InteractionHand hand) {
        entity.swing(hand, SwingAnimation.DEFAULT, false);
    }

    /** Vanilla knockback. 26.x also wants the damage source and a float (passed on unchanged by vanilla); a generic source and 0. UNVERIFIED in game. */
    public static void knockback(LivingEntity entity, double strength, double x, double z) {
        entity.knockback(strength, x, z, entity.damageSources().generic(), 0f);
    }

    public static long dayTime(Level level) {
        return level.getDefaultClockTime();
    }

    /** Puts a stack back in a player inventory (dropping what does not fit), as the server. UNVERIFIED in game. */
    public static void giveBack(net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.item.ItemStack stack) {
        inventory.placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
    }
}
