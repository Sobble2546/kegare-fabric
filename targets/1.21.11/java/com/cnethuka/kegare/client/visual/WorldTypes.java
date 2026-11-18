package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.Kegare;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The few render types Kegare's world drawing needs that vanilla's factories don't give (1.21.11 builds a {@link RenderType} from a
 * {@link RenderPipeline} and a {@link RenderSetup}; there are no render-state shards to subclass any more). {@link #glow} draws into the
 * main target, which is where the level passes of {@code LevelStages} are.
 * <ul>
 * <li>{@link #glow}: translucent, unlit (it's Malice, it glows), both faces, no depth written: vanilla's
 *     {@code entityTranslucentEmissive}, with a depth test of your choice and optionally the texture filtered smoothly (the art is
 *     high-resolution, not pixel art, DESIGN §3.16).</li>
 * <li>{@link #ghost}: a flat, lit, see-through copy of a body on the white texture ({@link FlatBodies}); vanilla's own item-entity type.</li>
 * </ul>
 */
public final class WorldTypes {
    public static final Identifier WHITE = Identifier.withDefaultNamespace("textures/misc/white.png");

    private static final Map<DepthTestFunction, RenderPipeline> GLOW = new HashMap<>();
    private static final Map<String, RenderType> TYPES = new HashMap<>();

    private WorldTypes() {}

    private static RenderPipeline glowPipeline(DepthTestFunction depth) {
        // Vanilla's pipeline/entity_translucent_emissive, with the depth test swapped.
        return GLOW.computeIfAbsent(depth, d -> RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                .withLocation(Kegare.id("pipeline/glow_" + d.name().toLowerCase(java.util.Locale.ROOT)))
                .withShaderDefine("ALPHA_CUTOUT", 0.1f)
                .withShaderDefine("PER_FACE_LIGHTING")
                .withSampler("Sampler1")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthWrite(false)
                .withDepthTestFunction(d)
                .build());
    }

    /** Translucent, unlit, both faces, nothing written to depth; {@code smooth} filters the texture linearly instead of pixel-sharp. */
    public static synchronized RenderType glow(Identifier texture, DepthTestFunction depth, boolean smooth) {
        return TYPES.computeIfAbsent("glow/" + depth + "/" + smooth + "/" + texture, k -> {
            RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(glowPipeline(depth));
            if (smooth) setup.withTexture("Sampler0", texture, () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            else setup.withTexture("Sampler0", texture);
            return RenderType.create("kegare_glow", setup.useOverlay().sortOnUpload().createRenderSetup());
        });
    }

    /**
     * A flat, lit, translucent, back-face-culled copy of a body ({@link FlatBodies}): vanilla's item-entity translucent type, which in
     * Fabulous graphics draws into the item-entity target (so it is depth-sorted against water and clouds like a dropped item). Draw it
     * from the level's entity stage, where that target is live.
     */
    public static RenderType ghost() {
        return RenderTypes.itemEntityTranslucentCull(WHITE);
    }

    /** Resource packs changed: a smooth sampler or a texture may differ, build the types again when next used. */
    public static synchronized void forget() {
        TYPES.clear();
    }
}
