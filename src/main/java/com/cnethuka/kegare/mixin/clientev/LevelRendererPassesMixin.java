package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.LevelStages;
import com.cnethuka.kegare.platform.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RenderLevelStageEvent AFTER_PARTICLES and AFTER_WEATHER. 1.21.11 draws the level through a frame graph whose passes run later, so a
 * call-site hook would run at graph-build time. Instead a pass of our own is added to the graph right after vanilla's particles pass
 * and weather pass; it reads and writes the main target and posts the event when it runs (LevelStages.addPass). Also remembers the
 * frame's matrices for the event.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererPassesMixin {
    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void kegare$matrices(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
                                 Matrix4f modelView, Matrix4f projection, Matrix4f cullingProjection, GpuBufferSlice fog, Vector4f fogColor,
                                 boolean renderSky, CallbackInfo ci) {
        LevelStages.captureMatrices(modelView, projection);
    }

    @Inject(method = "addParticlesPass", at = @At("RETURN"))
    private void kegare$afterParticles(FrameGraphBuilder builder, GpuBufferSlice fog, CallbackInfo ci) {
        LevelStages.addPass(builder, targets, RenderLevelStageEvent.Stage.AFTER_PARTICLES);
    }

    @Inject(method = "addWeatherPass", at = @At("RETURN"))
    private void kegare$afterWeather(FrameGraphBuilder builder, GpuBufferSlice fog, CallbackInfo ci) {
        LevelStages.addPass(builder, targets, RenderLevelStageEvent.Stage.AFTER_WEATHER);
    }
}
