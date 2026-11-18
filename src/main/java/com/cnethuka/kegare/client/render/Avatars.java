package com.cnethuka.kegare.client.render;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.11 draws a player from an {@link AvatarRenderState}, not the player: a layer that needs the player (their Visuals, their tick
 * count) finds it here. Layers are added with {@code client.visual.PlayerLayers}.
 */
public final class Avatars {
    private Avatars() {}

    /** The player a render state was extracted from, or null (a mannequin, or gone since). */
    @Nullable
    public static AbstractClientPlayer player(AvatarRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Entity entity = mc.level.getEntity(state.id);
        return entity instanceof AbstractClientPlayer player ? player : null;
    }

    /**
     * The partial tick a state was extracted at, for the entity it came from: its age counts the partial tick on top of the tick count.
     * (Not the frame's: a Standstill alters it for a frozen player.)
     */
    public static float partialTick(AvatarRenderState state, Entity entity) {
        return state.ageInTicks - entity.tickCount;
    }
}
