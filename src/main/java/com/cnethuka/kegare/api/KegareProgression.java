package com.cnethuka.kegare.api;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.progression.Attribution;
import com.cnethuka.kegare.progression.Landings;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Refinement and Assessment for an addon's abilities. Most of it is automatic: while an ability runs (its execute, tick and
 * end), anything it hurts, pushes or puts an effect on counts as that ability landing, and so does anything an entity it
 * spawned does later. Call these only for the cases that can't be seen from a hit. Server only.
 */
public final class KegareProgression {
    private KegareProgression() {}

    /**
     * {@code ability} landed on {@code target} without hurting it (a swap, a tether, a mark). Ignored unless the target is real
     * (not the caster, not their own) and the ability belongs to the caster's current technique.
     */
    public static void landed(ServerPlayer caster, LivingEntity target, Ability ability) {
        Landings.land(caster, target, ability);
    }

    /**
     * Something the caster owns landed this on its own (a summon picking its own fight). It's on the fight's record, but it
     * only earns Refinement while the caster is fighting too. See {@link com.cnethuka.kegare.api.progression.OwnedEntity}.
     */
    public static void landedOnItsOwn(ServerPlayer owner, LivingEntity target, Ability ability) {
        Landings.landOnItsOwn(owner, target, ability);
    }

    /** Runs a delayed effect (a timer, a scheduled burst) as part of the ability's cast, so what it hits counts for it. */
    public static void during(ServerPlayer caster, Ability ability, Runnable effect) {
        Landings.during(caster, ability, effect);
    }

    /**
     * Tells Kegare which ability is behind damage it can't trace on its own (one that isn't dealt inside a cast or by an
     * entity a cast spawned). Return null for damage that isn't yours.
     */
    public static void addAttributor(Attributor attributor) {
        Attribution.addAttributor(attributor::attribute);
    }

    /** Names the ability behind a hit, or null. */
    @FunctionalInterface
    public interface Attributor {
        @Nullable
        Ability attribute(ServerPlayer attacker, DamageSource source);
    }
}
