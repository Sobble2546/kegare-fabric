package com.cnethuka.kegare.client.visual;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 26.x: NOT PORTED. The 1.21.11 file draws a full-screen triangle with its own pipeline and uniform block; 26.x replaced that GPU API
 * (bind-group layouts, compiled pipelines) and the port is not written. Every effect built on this reports itself invalid, which the
 * callers already treat as "shader failed to compile": the screen effects (grayscale, invert, hit-stop hold) and the Senses outline
 * are skipped on 26.x. See the port report.
 */
public final class FullscreenFx implements AutoCloseable {
    public static final String BLOCK = "KegareFx";

    public FullscreenFx(String name, Identifier fragment, List<String> samplers, boolean blend) {}

    public boolean isValid() {
        return false;
    }

    public static boolean valid(RenderPipeline pipeline) {
        return false;
    }

    public void draw(RenderTarget out, float[] params, boolean linear, GpuTextureView... inputs) {}

    public static void copy(RenderTarget from, RenderTarget to) {}

    @Override
    public void close() {}
}
