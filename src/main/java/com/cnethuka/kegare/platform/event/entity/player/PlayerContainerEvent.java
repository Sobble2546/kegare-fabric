package com.cnethuka.kegare.platform.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** A container menu opened or closed for a player. */
public abstract class PlayerContainerEvent extends PlayerEvent {
    private final AbstractContainerMenu container;

    protected PlayerContainerEvent(Player player, AbstractContainerMenu container) {
        super(player);
        this.container = container;
    }

    public AbstractContainerMenu getContainer() {
        return container;
    }

    /** The menu is open: {@code player.containerMenu} is already it. */
    public static final class Open extends PlayerContainerEvent {
        public Open(Player player, AbstractContainerMenu container) {
            super(player, container);
        }
    }
}
