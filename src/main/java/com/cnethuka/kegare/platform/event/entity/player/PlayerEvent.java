package com.cnethuka.kegare.platform.event.entity.player;

import com.cnethuka.kegare.platform.event.entity.living.LivingEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Something about a player. */
public abstract class PlayerEvent extends LivingEvent {
    private final Player player;

    protected PlayerEvent(Player player) {
        super(player);
        this.player = player;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    /** A player joined the server (they are in the world and have been sent their state). */
    public static final class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player player) {
            super(player);
        }
    }

    /** A player is leaving the server; they have not been removed from the world yet. */
    public static final class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player player) {
            super(player);
        }
    }

    /** A player respawned (after dying, or leaving the End). */
    public static final class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;

        public PlayerRespawnEvent(Player player, boolean endConquered) {
            super(player);
            this.endConquered = endConquered;
        }

        public boolean isEndConquered() {
            return endConquered;
        }
    }

    /** A player arrived in another dimension. */
    public static final class PlayerChangedDimensionEvent extends PlayerEvent {
        private final ResourceKey<Level> from, to;

        public PlayerChangedDimensionEvent(Player player, ResourceKey<Level> from, ResourceKey<Level> to) {
            super(player);
            this.from = from;
            this.to = to;
        }

        public ResourceKey<Level> getFrom() {
            return from;
        }

        public ResourceKey<Level> getTo() {
            return to;
        }
    }

    /** A player started to see {@code target}: {@link #getEntity} is the player, {@link #getTarget} the entity they now track. */
    public static final class StartTracking extends PlayerEvent {
        private final Entity target;

        public StartTracking(Player player, Entity target) {
            super(player);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    /** A player's data was copied into their new body (respawn, or returning from the End). */
    public static final class Clone extends PlayerEvent {
        private final Player original;
        private final boolean wasDeath;

        public Clone(Player newPlayer, Player original, boolean wasDeath) {
            super(newPlayer);
            this.original = original;
            this.wasDeath = wasDeath;
        }

        public Player getOriginal() {
            return original;
        }

        public boolean isWasDeath() {
            return wasDeath;
        }
    }
}
