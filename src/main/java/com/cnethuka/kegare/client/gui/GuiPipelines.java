package com.cnethuka.kegare.client.gui;

import com.cnethuka.kegare.Kegare;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Kegare's own GUI render pipelines. The 1.21.11 GUI has no blend-state toggles any more: the blend lives in the pipeline a draw call
 * names, so the reveal's glows (which add light to what is behind them) need a pipeline of their own.
 */
public final class GuiPipelines {
    private GuiPipelines() {}

    /** Touching the class registers the pipelines; call it during client initialisation, before the first resource reload. */
    public static void init() {}

    /** Vanilla's textured GUI pipeline with additive blending (source alpha x source color added to the screen). */
    public static final RenderPipeline GUI_ADDITIVE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Kegare.id("pipeline/gui_additive"))
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
            .build());
}
