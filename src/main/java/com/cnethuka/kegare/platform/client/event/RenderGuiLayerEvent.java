package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/** A vanilla HUD layer is about to be drawn ({@link Pre}, cancel to skip it) or was drawn ({@link Post}). */
public abstract class RenderGuiLayerEvent extends Event {
    private final GuiGraphics guiGraphics;
    private final DeltaTracker partialTick;
    private final Identifier name;

    private RenderGuiLayerEvent(GuiGraphics guiGraphics, DeltaTracker partialTick, Identifier name) {
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
        this.name = name;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    /** The layer, one of {@code VanillaGuiLayers}. */
    public Identifier getName() {
        return name;
    }

    public static final class Pre extends RenderGuiLayerEvent implements ICancellableEvent {
        public Pre(GuiGraphics guiGraphics, DeltaTracker partialTick, Identifier name) {
            super(guiGraphics, partialTick, name);
        }
    }

    /** Not fired for a layer a Pre listener cancelled. */
    public static final class Post extends RenderGuiLayerEvent {
        public Post(GuiGraphics guiGraphics, DeltaTracker partialTick, Identifier name) {
            super(guiGraphics, partialTick, name);
        }
    }
}
