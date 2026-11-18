package com.cnethuka.kegare.platform.event.entity.player;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** A player attacks (cancel: no attack). Fired at the start of {@code Player.attack}. */
public final class AttackEntityEvent extends PlayerEvent implements ICancellableEvent {
    private final Entity target;

    public AttackEntityEvent(Player player, Entity target) {
        super(player);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
