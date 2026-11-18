package com.cnethuka.kegare.platform.event.entity.living;

import com.cnethuka.kegare.platform.damage.DamageContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** The two events around the damage a hit really does: {@link Pre} after armor and effects, {@link Post} after the health is taken. */
public abstract class LivingDamageEvent extends LivingEvent {
    private final DamageContainer container;

    protected LivingDamageEvent(LivingEntity entity, DamageContainer container) {
        super(entity);
        this.container = container;
    }

    public DamageContainer getContainer() {
        return container;
    }

    public DamageSource getSource() {
        return container.getSource();
    }

    public float getOriginalDamage() {
        return container.getOriginalDamage();
    }

    /** Armor, enchantments and effects have taken their share; absorption and the health bar haven't. Listeners may change the amount. */
    public static final class Pre extends LivingDamageEvent {
        public Pre(LivingEntity entity, DamageContainer container) {
            super(entity, container);
        }

        public float getNewDamage() {
            return getContainer().getNewDamage();
        }

        public void setNewDamage(float damage) {
            getContainer().setNewDamage(damage);
        }
    }

    /** The health has been taken. {@link #getNewDamage} is what was actually removed (after absorption); 0 when nothing got through. */
    public static final class Post extends LivingDamageEvent {
        private final float newDamage;

        public Post(LivingEntity entity, DamageContainer container) {
            this(entity, container, container.getNewDamage());
        }

        public Post(LivingEntity entity, DamageContainer container, float newDamage) {
            super(entity, container);
            this.newDamage = newDamage;
        }

        public float getNewDamage() {
            return newDamage;
        }
    }
}
