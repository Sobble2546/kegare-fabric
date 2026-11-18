package com.cnethuka.kegare.platform.event.entity;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.LivingEntity;

/** An entity teleports itself. */
public abstract class EntityTeleportEvent extends EntityEvent implements ICancellableEvent {
    private final double targetX, targetY, targetZ;

    protected EntityTeleportEvent(net.minecraft.world.entity.Entity entity, double x, double y, double z) {
        super(entity);
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    /** An enderman (or another EnderMan) teleports. */
    public static final class EnderEntity extends EntityTeleportEvent {
        private final LivingEntity living;

        public EnderEntity(LivingEntity entity, double x, double y, double z) {
            super(entity, x, y, z);
            this.living = entity;
        }

        public LivingEntity getEntityLiving() {
            return living;
        }
    }
}
