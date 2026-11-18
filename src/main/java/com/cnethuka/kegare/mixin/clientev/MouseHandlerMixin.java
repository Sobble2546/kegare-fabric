package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** InputEvent.MouseButton.Pre / Post: a mouse button change, before and after the game handled it. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void kegare$before(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        ClientEventHooks.mouseButton(true, info.button(), action, info.modifiers());
    }

    @Inject(method = "onButton", at = @At("RETURN"))
    private void kegare$after(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        ClientEventHooks.mouseButton(false, info.button(), action, info.modifiers());
    }
}
