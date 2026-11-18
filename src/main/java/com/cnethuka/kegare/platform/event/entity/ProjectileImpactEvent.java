package com.cnethuka.kegare.platform.event.entity;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/** A projectile hit something (cancel: it passes through). */
public final class ProjectileImpactEvent extends EntityEvent implements ICancellableEvent {
    private final Projectile projectile;
    private final HitResult hit;

    public ProjectileImpactEvent(Projectile projectile, HitResult hit) {
        super(projectile);
        this.projectile = projectile;
        this.hit = hit;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public HitResult getRayTraceResult() {
        return hit;
    }
}
