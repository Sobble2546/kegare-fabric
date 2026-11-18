package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;

/**
 * A player's render state is being extracted ({@link Pre}) or was ({@link Post}). On 1.21.11 held items and armor are read at
 * extraction, so state swapped in Pre and undone in Post is what the player is drawn with. The model is NOT posed yet at Post: it
 * is posed from the render state when drawn.
 */
public abstract class RenderPlayerEvent extends Event {
    private final Player player;
    private final AvatarRenderer<?> renderer;
    private final AvatarRenderState renderState;
    private final float partialTick;

    private RenderPlayerEvent(Player player, AvatarRenderer<?> renderer, AvatarRenderState renderState, float partialTick) {
        this.player = player;
        this.renderer = renderer;
        this.renderState = renderState;
        this.partialTick = partialTick;
    }

    public Player getEntity() {
        return player;
    }

    public AvatarRenderer<?> getRenderer() {
        return renderer;
    }

    public AvatarRenderState getRenderState() {
        return renderState;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public static final class Pre extends RenderPlayerEvent {
        public Pre(Player player, AvatarRenderer<?> renderer, AvatarRenderState renderState, float partialTick) {
            super(player, renderer, renderState, partialTick);
        }
    }

    public static final class Post extends RenderPlayerEvent {
        public Post(Player player, AvatarRenderer<?> renderer, AvatarRenderState renderState, float partialTick) {
            super(player, renderer, renderState, partialTick);
        }
    }
}
