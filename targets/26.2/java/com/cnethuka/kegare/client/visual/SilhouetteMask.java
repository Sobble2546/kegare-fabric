package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.platform.client.KBuffers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.textures.GpuTextureView;

import javax.annotation.Nullable;

/** 26.x: NOT PORTED (see {@link FullscreenFx} for this target). Always invalid, so the Senses outline is skipped. */
public final class SilhouetteMask {
    private SilhouetteMask() {}

    public static boolean isValid() {
        return false;
    }

    public static VertexConsumer begin(RenderTarget main) {
        return new KBuffers.Recording(); // written to and thrown away
    }

    public static void end() {}

    public static void idle() {}

    public static void close() {}

    @Nullable
    public static GpuTextureView view() {
        return null;
    }
}
