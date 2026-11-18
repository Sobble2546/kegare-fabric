package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** RenderFrameEvent.Pre / Post (once a frame) and ViewportEvent.ComputeFov (the world's and the hand's field of view). */
@Mixin(GameRenderer.class)
public abstract class GameRendererEventsMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void kegare$frameStart(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ClientEventHooks.frameStart(deltaTracker);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void kegare$frameEnd(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ClientEventHooks.frameEnd(deltaTracker);
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float kegare$fov(float fov, Camera camera, float partialTick, boolean useConfiguredFov) {
        return ClientEventHooks.computeFov(camera, partialTick, fov, useConfiguredFov);
    }
}
