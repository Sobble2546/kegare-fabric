package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import com.cnethuka.kegare.platform.client.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * InputEvent.InteractionKeyMappingTriggered: attack, use item, pick block. NeoForge fires it from exactly these methods.
 * Cancelling stops the action; vanilla's "stop breaking" path (continueAttack with the button up) is never touched.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftInteractionMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void kegare$attack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (ClientEventHooks.interaction(InputEvent.Interaction.ATTACK, InteractionHand.MAIN_HAND, mc.options.keyAttack)) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void kegare$continueAttack(boolean leftClick, CallbackInfo ci) {
        if (!leftClick) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (ClientEventHooks.interaction(InputEvent.Interaction.ATTACK, InteractionHand.MAIN_HAND, mc.options.keyAttack)) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void kegare$use(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (ClientEventHooks.interaction(InputEvent.Interaction.USE_ITEM, InteractionHand.MAIN_HAND, mc.options.keyUse)) ci.cancel();
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void kegare$pick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (ClientEventHooks.interaction(InputEvent.Interaction.PICK_BLOCK, InteractionHand.MAIN_HAND, mc.options.keyPickItem)) ci.cancel();
    }
}
