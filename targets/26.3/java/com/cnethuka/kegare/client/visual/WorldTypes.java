package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.Kegare;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
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
    public static final Identifier WHITE = Identifier.fromNamespaceAndPath("kegare", "textures/misc/white.png");

    private static final Map<CompareOp, RenderPipeline> GLOW = new HashMap<>();
    private record Key(Identifier texture, CompareOp depth, boolean smooth) {}

    private static final Map<Key, RenderType> TYPES = new HashMap<>();
    /** The per-texture memos of {@link #glowTypes}, emptied together with {@link #TYPES} on a reload. */
    private static final List<Map<Identifier, RenderType>> MEMOS = new ArrayList<>();

    private WorldTypes() {}

    private static RenderPipeline glowPipeline(CompareOp depth) {
        // Vanilla's pipeline/entity_translucent_emissive, with the depth test swapped.
        return GLOW.computeIfAbsent(depth, d -> RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                .withLocation(Kegare.id("pipeline/glow_" + d.name().toLowerCase(java.util.Locale.ROOT)))
                .withShaderDefine("ALPHA_CUTOUT", 0.1f)
                .withShaderDefine("PER_FACE_LIGHTING")
                .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER1) // the overlay texture the shader declares; without it the pipeline fails and 26.x crashes
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(d, false))
                .build());
    }

    /** Translucent, unlit, both faces, nothing written to depth; {@code smooth} filters the texture linearly instead of pixel-sharp. */
    public static synchronized RenderType glow(Identifier texture, CompareOp depth, boolean smooth) {
        return TYPES.computeIfAbsent(new Key(texture, depth, smooth), k -> {
            RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(glowPipeline(k.depth()));
            if (k.smooth()) setup.withTexture("Sampler0", k.texture(), () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            else setup.withTexture("Sampler0", k.texture());
            return RenderType.create("kegare_glow", setup.useOverlay().sortOnUpload().createRenderSetup());
        });
    }

    /**
     * {@link #glow} for a depth test and filtering fixed, as a function of the texture, for what is drawn every frame: a texture looked
     * up again finds its type in a map of its own (no key built, no lock), and {@link #forget} empties it with the rest.
     */
    public static Function<Identifier, RenderType> glowTypes(CompareOp depth, boolean smooth) {
        Map<Identifier, RenderType> memo = new ConcurrentHashMap<>();
        synchronized (WorldTypes.class) {
            MEMOS.add(memo);
        }
        return texture -> {
            RenderType type = memo.get(texture);
            if (type == null) {
                type = glow(texture, depth, smooth);
                memo.put(texture, type);
            }
            return type;
        };
    }

    /**
     * A flat, lit, translucent, back-face-culled copy of a body ({@link FlatBodies}): vanilla's item-entity translucent type, which in
     * Fabulous graphics draws into the item-entity target (so it is depth-sorted against water and clouds like a dropped item). Draw it
     * from the level's entity stage, where that target is live.
     */
    public static RenderType ghost() {
        return RenderTypes.itemTranslucent(WHITE);
    }

    /** Resource packs changed: a smooth sampler or a texture may differ, build the types again when next used. */
    public static synchronized void forget() {
        TYPES.clear();
        MEMOS.forEach(Map::clear);
    }
}
