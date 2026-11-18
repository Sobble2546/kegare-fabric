package com.cnethuka.kegare.reinforcement.client;

import com.cnethuka.kegare.client.visual.WorldTypes;
import com.mojang.blaze3d.platform.DepthTestFunction;
import java.util.function.Function;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * The aura's render type: see-through, unlit (it's Malice, it glows), both faces, no depth written, and the texture filtered smoothly
 * (the aura is high-resolution art, not pixel art, DESIGN §3.16). Vanilla's translucent emissive entity pipeline with a linear sampler
 * ({@link WorldTypes#glow}).
 */
final class AuraRenderTypes {
    private AuraRenderTypes() {}

    static final Function<Identifier, RenderType> SMOOTH = texture -> WorldTypes.glow(texture, DepthTestFunction.LEQUAL_DEPTH_TEST, true);
}
