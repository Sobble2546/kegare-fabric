package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.client.DeltaTracker;

/** Once a frame: {@link Pre} before anything is drawn, {@link Post} after the whole frame. */
public abstract class RenderFrameEvent extends Event {
    private final DeltaTracker partialTick;

    private RenderFrameEvent(DeltaTracker partialTick) {
        this.partialTick = partialTick;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    public static final class Pre extends RenderFrameEvent {
        public Pre(DeltaTracker partialTick) {
            super(partialTick);
        }
    }

    public static final class Post extends RenderFrameEvent {
        public Post(DeltaTracker partialTick) {
            super(partialTick);
        }
    }
}
