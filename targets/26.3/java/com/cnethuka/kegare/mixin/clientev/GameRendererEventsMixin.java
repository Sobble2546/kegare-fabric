package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** RenderFrameEvent.Pre / Post on 26.x (render's parameters differ between 26.2 and 26.3, so none are taken). The field of view is in CameraAnglesMixin. */
@Mixin(GameRenderer.class)
public abstract class GameRendererEventsMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void kegare$frameStart(CallbackInfo ci) {
        ClientEventHooks.frameStart(Minecraft.getInstance().getDeltaTracker());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void kegare$frameEnd(CallbackInfo ci) {
        ClientEventHooks.frameEnd(Minecraft.getInstance().getDeltaTracker());
    }
}
