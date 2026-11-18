package com.cnethuka.kegare.mixin.client;

import com.cnethuka.kegare.impact.client.ScreenFx;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Impact System's whole-screen step (DESIGN §3.23): once the world, your hand, the glowing outlines and vanilla's own post effect
 * are drawn, and before the HUD. Only reached while a world is drawn.
 * <p>
 * 1.21.11: {@code GameRenderer.render} draws the level, then the outline pass and vanilla's post effect, then ends the fog frame and
 * clears the depth for the HUD. The hook sits just before the fog frame ends, but that call is also reached when no level was drawn,
 * so a flag set after the level call says whether one was.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private boolean kegare$levelDrawn;

    @Inject(method = "render", at = @At("HEAD"))
    private void kegare$frameStart(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        kegare$levelDrawn = false;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void kegare$levelWasDrawn(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        kegare$levelDrawn = true;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void kegare$afterLevel(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (!kegare$levelDrawn) return;
        kegare$levelDrawn = false;
        ScreenFx.afterLevel(deltaTracker);
    }
}
