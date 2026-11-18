package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.Kegare;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

/**
 * A whole-screen pass on 1.21.11's pipeline API, in the place of the removed {@code PostChain} / {@code EffectInstance}: one fragment
 * shader ({@code assets/<ns>/shaders/post/<name>.fsh}, GLSL 330, vanilla's {@code core/screenquad} vertex shader, so a full-screen
 * triangle with {@code texCoord}), a few named samplers and one std140 block {@code KegareFx { vec4 Params; vec4 Size; }}. Params is what
 * the caller passes (Amount and the like), Size is {@code (width, height, 1/width, 1/height)} of the target. Output goes to a render
 * target's colour attachment only (no depth), either replacing what's there or alpha-blended over it.
 * <p>
 * It cannot read the target it writes, so an effect that needs the picture copies it first ({@link #copy}).
 */
public final class FullscreenFx implements AutoCloseable {
    public static final String BLOCK = "KegareFx";

    private final String name;
    private final RenderPipeline pipeline;
    private final List<String> samplers;
    private GpuBuffer ubo;

    /**
     * @param fragment shader id: {@code kegare:post/invert} is {@code assets/kegare/shaders/post/invert.fsh}
     * @param samplers sampler uniform names in the shader, bound in {@link #draw}'s order
     * @param blend    alpha-blend over the target instead of replacing it
     */
    public FullscreenFx(String name, Identifier fragment, List<String> samplers, boolean blend) {
        this.name = name;
        this.samplers = List.copyOf(samplers);
        RenderPipeline.Builder b = RenderPipeline.builder()
                .withLocation(Kegare.id("pipeline/fx_" + name))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(fragment)
                .withUniform(BLOCK, UniformType.UNIFORM_BUFFER)
                .withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES);
        for (String s : this.samplers) b.withSampler(s);
        if (blend) b.withBlend(BlendFunction.TRANSLUCENT);
        else b.withoutBlend();
        this.pipeline = b.build();
    }

    /**
     * Runs the pass over all of {@code out}'s colour attachment. {@code inputs} are bound to the samplers in order (linear filtering when
     * {@code linear}). Throws if the shader doesn't compile: callers catch it and skip the effect.
     */
    public void draw(RenderTarget out, float[] params, boolean linear, GpuTextureView... inputs) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        writeBlock(encoder, params, out.width, out.height);
        FilterMode filter = linear ? FilterMode.LINEAR : FilterMode.NEAREST;
        try (RenderPass pass = encoder.createRenderPass(() -> "kegare " + name, out.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform(BLOCK, ubo);
            for (int i = 0; i < samplers.size() && i < inputs.length; i++) {
                pass.bindTexture(samplers.get(i), inputs[i], RenderSystem.getSamplerCache().getClampToEdge(filter));
            }
            pass.draw(0, 3);
        }
    }

    private void writeBlock(CommandEncoder encoder, float[] p, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder block = Std140Builder.onStack(stack, 32);
            block.putVec4(p.length > 0 ? p[0] : 0f, p.length > 1 ? p[1] : 0f, p.length > 2 ? p[2] : 0f, p.length > 3 ? p[3] : 0f);
            block.putVec4(width, height, 1f / Math.max(1, width), 1f / Math.max(1, height));
            ByteBuffer data = block.get();
            if (ubo == null) {
                ubo = RenderSystem.getDevice().createBuffer(() -> "kegare " + name, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32L);
            }
            encoder.writeToBuffer(ubo.slice(), data);
        }
    }

    /** Copies {@code from}'s colour into {@code to}'s (same size and format), so a pass can read the picture it is about to replace. */
    public static void copy(RenderTarget from, RenderTarget to) {
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(from.getColorTexture(), to.getColorTexture(), 0, 0, 0, 0, 0,
                Math.min(from.width, to.width), Math.min(from.height, to.height));
    }

    @Override
    public void close() {
        if (ubo != null) ubo.close();
        ubo = null;
    }
}
