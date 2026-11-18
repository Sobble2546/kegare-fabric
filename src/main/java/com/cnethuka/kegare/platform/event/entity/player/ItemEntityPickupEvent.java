package com.cnethuka.kegare.platform.event.entity.player;

import com.cnethuka.kegare.platform.TriState;
import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** A player touched a dropped item. */
public abstract class ItemEntityPickupEvent extends Event {
    private final Player player;
    private final ItemEntity item;

    protected ItemEntityPickupEvent(Player player, ItemEntity item) {
        this.player = player;
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemEntity getItemEntity() {
        return item;
    }

    /** Before the pickup: {@link TriState#FALSE} stops it. */
    public static final class Pre extends ItemEntityPickupEvent {
        private TriState canPickup = TriState.DEFAULT;

        public Pre(Player player, ItemEntity item) {
            super(player, item);
        }

        public TriState canPickup() {
            return canPickup;
        }

        public void setCanPickup(TriState canPickup) {
            this.canPickup = canPickup;
        }
    }
}
