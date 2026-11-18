package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;

/** Raw input, before the game acts on it. Fired even while a screen is open; check {@code Minecraft.screen} where it matters. */
public abstract class InputEvent extends Event {
    private InputEvent() {}

    /** A keyboard key went down, up or repeated. {@code action} is GLFW's (press 1, release 0, repeat 2). */
    public static final class Key extends InputEvent {
        private final int key;
        private final int scanCode;
        private final int action;
        private final int modifiers;

        public Key(int key, int scanCode, int action, int modifiers) {
            this.key = key;
            this.scanCode = scanCode;
            this.action = action;
            this.modifiers = modifiers;
        }

        public int getKey() {
            return key;
        }

        public int getScanCode() {
            return scanCode;
        }

        public int getAction() {
            return action;
        }

        public int getModifiers() {
            return modifiers;
        }
    }

    /** A mouse button changed. {@link Pre} runs before the game handles it, {@link Post} after. */
    public abstract static class MouseButton extends InputEvent {
        private final int button;
        private final int action;
        private final int modifiers;

        private MouseButton(int button, int action, int modifiers) {
            this.button = button;
            this.action = action;
            this.modifiers = modifiers;
        }

        public int getButton() {
            return button;
        }

        public int getAction() {
            return action;
        }

        public int getModifiers() {
            return modifiers;
        }

        public static final class Pre extends MouseButton {
            public Pre(int button, int action, int modifiers) {
                super(button, action, modifiers);
            }
        }

        public static final class Post extends MouseButton {
            public Post(int button, int action, int modifiers) {
                super(button, action, modifiers);
            }
        }
    }

    /** What an interaction key is about to do. */
    public enum Interaction {
        ATTACK, USE_ITEM, PICK_BLOCK
    }

    /**
     * Attack, use or pick-block is about to run. Cancel to stop it; a canceled one still swings the hand unless
     * {@link #setSwingHand} says no.
     */
    public static final class InteractionKeyMappingTriggered extends InputEvent implements ICancellableEvent {
        private final Interaction kind;
        private final InteractionHand hand;
        private final KeyMapping keyMapping;
        private boolean swingHand = true;

        public InteractionKeyMappingTriggered(Interaction kind, InteractionHand hand, KeyMapping keyMapping) {
            this.kind = kind;
            this.hand = hand;
            this.keyMapping = keyMapping;
        }

        public Interaction getInteraction() {
            return kind;
        }

        public boolean isAttack() {
            return kind == Interaction.ATTACK;
        }

        public boolean isUseItem() {
            return kind == Interaction.USE_ITEM;
        }

        public boolean isPickBlock() {
            return kind == Interaction.PICK_BLOCK;
        }

        public InteractionHand getHand() {
            return hand;
        }

        public KeyMapping getKeyMapping() {
            return keyMapping;
        }

        public boolean shouldSwingHand() {
            return swingHand;
        }

        public void setSwingHand(boolean swingHand) {
            this.swingHand = swingHand;
        }
    }
}
