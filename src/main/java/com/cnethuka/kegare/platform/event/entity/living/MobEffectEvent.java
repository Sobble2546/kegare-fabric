package com.cnethuka.kegare.platform.event.entity.living;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import javax.annotation.Nullable;

/** Something about a status effect. */
public abstract class MobEffectEvent extends LivingEvent {
    private final MobEffectInstance effect;

    protected MobEffectEvent(LivingEntity entity, MobEffectInstance effect) {
        super(entity);
        this.effect = effect;
    }

    public MobEffectInstance getEffectInstance() {
        return effect;
    }

    /** An effect was put on someone (fired just after it is applied). */
    public static final class Added extends MobEffectEvent {
        private final @Nullable MobEffectInstance oldEffect;
        private final @Nullable Entity source;

        public Added(LivingEntity entity, @Nullable MobEffectInstance oldEffect, MobEffectInstance newEffect, @Nullable Entity source) {
            super(entity, newEffect);
            this.oldEffect = oldEffect;
            this.source = source;
        }

        public @Nullable MobEffectInstance getOldEffectInstance() {
            return oldEffect;
        }

        /** Who put it there (the thrower of a potion, the drinker's attacker), if anyone. */
        public @Nullable Entity getEffectSource() {
            return source;
        }
    }
}
