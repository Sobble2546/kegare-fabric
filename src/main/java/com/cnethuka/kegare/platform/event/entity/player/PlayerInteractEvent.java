package com.cnethuka.kegare.platform.event.entity.player;

import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** A player used or clicked something. Every one of these is cancellable. */
public abstract class PlayerInteractEvent extends PlayerEvent implements ICancellableEvent {
    private final InteractionHand hand;
    private final BlockPos pos;

    protected PlayerInteractEvent(Player player, InteractionHand hand, BlockPos pos) {
        super(player);
        this.hand = hand;
        this.pos = pos;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public BlockPos getPos() {
        return pos;
    }

    public net.minecraft.world.item.ItemStack getItemStack() {
        return getEntity().getItemInHand(hand);
    }

    public static final class RightClickBlock extends PlayerInteractEvent {
        private final BlockHitResult hit;

        public RightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hit) {
            super(player, hand, pos);
            this.hit = hit;
        }

        public BlockHitResult getHitVec() {
            return hit;
        }

        public Direction getFace() {
            return hit.getDirection();
        }
    }

    public static final class RightClickItem extends PlayerInteractEvent {
        public RightClickItem(Player player, InteractionHand hand) {
            super(player, hand, player.blockPosition());
        }
    }

    public static final class LeftClickBlock extends PlayerInteractEvent {
        private final Direction face;

        public LeftClickBlock(Player player, BlockPos pos, Direction face) {
            super(player, InteractionHand.MAIN_HAND, pos);
            this.face = face;
        }

        public Direction getFace() {
            return face;
        }
    }

    public static class EntityInteract extends PlayerInteractEvent {
        private final Entity target;

        public EntityInteract(Player player, InteractionHand hand, Entity target) {
            super(player, hand, target.blockPosition());
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    public static final class EntityInteractSpecific extends PlayerInteractEvent {
        private final Entity target;
        private final Vec3 localPos;

        public EntityInteractSpecific(Player player, InteractionHand hand, Entity target, Vec3 localPos) {
            super(player, hand, target.blockPosition());
            this.target = target;
            this.localPos = localPos;
        }

        public Entity getTarget() {
            return target;
        }

        public Vec3 getLocalPos() {
            return localPos;
        }
    }
}
