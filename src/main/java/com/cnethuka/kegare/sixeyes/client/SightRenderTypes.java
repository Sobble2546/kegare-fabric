package com.cnethuka.kegare.sixeyes.client;

import com.cnethuka.kegare.client.visual.WorldTypes;
import com.mojang.blaze3d.platform.DepthTestFunction;
import java.util.function.Function;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** What the eyes draw in the world: seen through walls (no depth test), see-through, unlit, smooth ({@link WorldTypes#glow}). */
final class SightRenderTypes {
    private SightRenderTypes() {}

    static final Function<Identifier, RenderType> THROUGH_WALLS = texture -> WorldTypes.glow(texture, DepthTestFunction.NO_DEPTH_TEST, true);

    /** Drawn only where something stands in front of it: what's behind a wall, never over what's in plain view. */
    static final Function<Identifier, RenderType> BEHIND_WALLS = texture -> WorldTypes.glow(texture, DepthTestFunction.GREATER_DEPTH_TEST, true);

    /** Ordinary depth: in the open, round the body, under nothing. */
    static final Function<Identifier, RenderType> IN_THE_OPEN = texture -> WorldTypes.glow(texture, DepthTestFunction.LEQUAL_DEPTH_TEST, true);
}
