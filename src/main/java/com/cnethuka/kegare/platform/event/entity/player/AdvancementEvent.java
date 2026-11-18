package com.cnethuka.kegare.platform.event.entity.player;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.entity.player.Player;

/** Something about a player's advancements. */
public abstract class AdvancementEvent extends PlayerEvent {
    private final AdvancementHolder advancement;

    protected AdvancementEvent(Player player, AdvancementHolder advancement) {
        super(player);
        this.advancement = advancement;
    }

    public AdvancementHolder getAdvancement() {
        return advancement;
    }

    /** The player completed an advancement (the first time only). */
    public static final class AdvancementEarnEvent extends AdvancementEvent {
        public AdvancementEarnEvent(Player player, AdvancementHolder advancement) {
            super(player, advancement);
        }
    }
}
