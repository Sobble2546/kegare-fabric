package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.TriState;
import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * An entity's name tag was just worked out (while its render state is extracted, once a frame per entity that shows one).
 * Change the text with {@link #setContent}; {@code setCanRender(FALSE)} hides it. A name tag the game did not mean to show is not
 * offered, so {@code TRUE} cannot force one.
 */
public final class RenderNameTagEvent extends Event {
    private final Entity entity;
    private final float partialTick;
    private Component content;
    private TriState canRender = TriState.DEFAULT;

    public RenderNameTagEvent(Entity entity, Component content, float partialTick) {
        this.entity = entity;
        this.content = content;
        this.partialTick = partialTick;
    }

    public Entity getEntity() {
        return entity;
    }

    public Component getContent() {
        return content;
    }

    public void setContent(Component content) {
        this.content = content;
    }

    public TriState canRender() {
        return canRender;
    }

    public void setCanRender(TriState canRender) {
        this.canRender = canRender;
    }

    public float getPartialTick() {
        return partialTick;
    }
}
