package com.cnethuka.kegare.platform.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** A player's attack is deciding whether it is a critical hit. */
public final class CriticalHitEvent extends PlayerEvent {
    private final Entity target;
    private final boolean vanillaCritical;
    private float damageMultiplier;
    private boolean criticalHit;

    public CriticalHitEvent(Player player, Entity target, float damageMultiplier, boolean vanillaCritical) {
        super(player);
        this.target = target;
        this.damageMultiplier = damageMultiplier;
        this.vanillaCritical = vanillaCritical;
        this.criticalHit = vanillaCritical;
    }

    public Entity getTarget() {
        return target;
    }

    /** Whether the game would have made it a critical. */
    public boolean isVanillaCritical() {
        return vanillaCritical;
    }

    /** Whether it will be one. */
    public boolean isCriticalHit() {
        return criticalHit;
    }

    public void setCriticalHit(boolean criticalHit) {
        this.criticalHit = criticalHit;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }
}
