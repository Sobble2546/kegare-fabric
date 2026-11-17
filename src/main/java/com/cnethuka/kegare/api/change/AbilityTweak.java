package com.cnethuka.kegare.api.change;

import com.cnethuka.kegare.api.ability.AbilityContext;
import com.cnethuka.kegare.api.ability.EndReason;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * A change to one existing ability's numbers or behaviour, registered with {@link TechniqueChanges#tweak}. Override only what you
 * change; each method gets what the ability (and any tweak registered before yours) decided and returns what it should be instead.
 * Everything runs on the logical server.
 * <p>
 * A tweak applies to the one ability it names. A special that shares its base ability's cooldown keeps the length it had: tweak the
 * special too to change it.
 */
public interface AbilityTweak {
    /** Its Malice cost: what pressing checks, what the cast pays, and what the HUD and the Sorcerer screen show. */
    default double cost(ServerPlayer player, double cost) {
        return cost;
    }

    /** Its cooldown in ticks, and what the Sorcerer screen shows. */
    default int cooldownTicks(ServerPlayer player, int ticks) {
        return ticks;
    }

    /** CHANNELED and TOGGLE: Malice per second while it runs. */
    default double upkeepPerSecond(ServerPlayer player, double upkeep) {
        return upkeep;
    }

    /** CHARGED: ticks before a release counts. */
    default int minChargeTicks(ServerPlayer player, int ticks) {
        return ticks;
    }

    /** CHARGED: ticks to full charge. */
    default int maxChargeTicks(ServerPlayer player, int ticks) {
        return ticks;
    }

    /** CHANNELED: its longest run in ticks (0: until released or out of Malice). */
    default int maxChannelTicks(ServerPlayer player, int ticks) {
        return ticks;
    }

    /**
     * Damage this ability deals to {@code target}: any hit Kegare traces back to it (dealt during its cast, or by an entity it
     * spawned), before armour and other defences.
     */
    default float damage(ServerPlayer caster, LivingEntity target, float amount) {
        return amount;
    }

    /**
     * Whether it may be cast now. Checked with Kegare's own checks, before anything is paid: false and the cast fizzles for free
     * with the ability's "can't cast" message.
     */
    default boolean canCast(AbilityContext ctx) {
        return true;
    }

    /** Runs just before the ability's own effect, inside its cast (anything you hit counts for that ability). */
    default void beforeExecute(AbilityContext ctx) {}

    /** Runs just after the ability's own effect, inside its cast. */
    default void afterExecute(AbilityContext ctx) {}

    /** Every tick while it charges, channels or is toggled on, after the ability's own tick. */
    default void tick(AbilityContext ctx) {}

    /** When a charge, channel or toggle ends, after the ability's own end. */
    default void onEnd(AbilityContext ctx, EndReason reason) {}
}
