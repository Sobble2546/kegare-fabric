package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;

/**
 * The local player's movement input was just read from the keys, before the game uses it. Change {@link #getInput()}'s fields and
 * the change is written back (1.21.11 keeps its input in an immutable record plus a move vector; this is the mutable view
 * NeoForge's {@code Input} was).
 */
public final class MovementInputUpdateEvent extends Event {
    private final Input input;

    public MovementInputUpdateEvent(Input input) {
        this.input = input;
    }

    public Input getInput() {
        return input;
    }

    /** The movement keys and the two impulses they make. {@code up} is forward, {@code down} backward. */
    public static final class Input {
        public float forwardImpulse;
        public float leftImpulse;
        public boolean up;
        public boolean down;
        public boolean left;
        public boolean right;
        public boolean jumping;
        public boolean shiftKeyDown;
        public boolean sprinting;

        public Input() {}

        public Input(boolean up, boolean down, boolean left, boolean right, boolean jumping, boolean shiftKeyDown, boolean sprinting,
                     float forwardImpulse, float leftImpulse) {
            this.up = up;
            this.down = down;
            this.left = left;
            this.right = right;
            this.jumping = jumping;
            this.shiftKeyDown = shiftKeyDown;
            this.sprinting = sprinting;
            this.forwardImpulse = forwardImpulse;
            this.leftImpulse = leftImpulse;
        }
    }
}
