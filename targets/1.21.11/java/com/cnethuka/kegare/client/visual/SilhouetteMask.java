package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.Kegare;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * An off-screen mask the size of the main target that position+colour quads are drawn into (the Severed senses' silhouettes): no depth
 * test and no blending, so a silhouette is the same through any wall. The 1.21.11 spelling (a render type whose output target is the
 * mask); a 26.x variant of this class, same members, replaces it.
 */
public final class SilhouetteMask {
    @Nullable private static TextureTarget mask;
    @Nullable private static MultiBufferSource.BufferSource buffers;

    private SilhouetteMask() {}

    /** Built on first use. */
    private static final class Type {
        static final RenderType TYPE = RenderType.create("kegare_senses_mask", RenderSetup.builder(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Kegare.id("pipeline/senses_mask"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .withoutBlend()
                .build()).setOutputTarget(new OutputTarget("kegare_senses_mask", () -> mask)).createRenderSetup());
    }

    /** Clears the mask (sized to {@code main}) and returns where to draw into it; call {@link #end} when done. */
    public static VertexConsumer begin(RenderTarget main) {
        if (mask == null) mask = new TextureTarget("kegare_senses_mask", main.width, main.height, false);
        else if (mask.width != main.width || mask.height != main.height) mask.resize(main.width, main.height);
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mask.getColorTexture(), 0);
        if (buffers == null) buffers = MultiBufferSource.immediate(new ByteBufferBuilder(1 << 18));
        return buffers.getBuffer(Type.TYPE);
    }

    /** Draws what was written since {@link #begin} into the mask. */
    public static void end() {
        if (buffers != null) buffers.endBatch();
    }

    /** The mask, to sample. */
    public static GpuTextureView view() {
        return mask.getColorTextureView();
    }
}
