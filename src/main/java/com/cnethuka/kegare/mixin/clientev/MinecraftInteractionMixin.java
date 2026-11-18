package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import com.cnethuka.kegare.platform.client.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * InputEvent.InteractionKeyMappingTriggered: attack, use item, pick block, fired at the head of the vanilla methods.
 * Cancelling stops the action; vanilla's "stop breaking" path (continueAttack with the button up) is never touched.
 * <p>One click is one ATTACK: a tick that starts an attack also runs {@code continueAttack(true)} (which would start digging if the click was
 * cancelled), and that second call reuses the first one's answer instead of asking the listeners again. USE_ITEM is fired once, for the main
 * hand, before vanilla's own early-outs (a busy hand, a block being broken): a listener sees the key press, not whether vanilla would have acted.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftInteractionMixin {
    /** The client tick an ATTACK was last fired on, and whether it was cancelled. */
    @Unique
    private int kegare$attackTick = Integer.MIN_VALUE;
    @Unique
    private boolean kegare$attackCancelled;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void kegare$attack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        kegare$attackCancelled = ClientEventHooks.interaction(InputEvent.Interaction.ATTACK, InteractionHand.MAIN_HAND, mc.options.keyAttack);
        kegare$attackTick = ClientEventHooks.clientTicks();
        if (kegare$attackCancelled) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void kegare$continueAttack(boolean leftClick, CallbackInfo ci) {
        if (!leftClick) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (kegare$attackTick == ClientEventHooks.clientTicks()) { // startAttack already asked this tick
            if (kegare$attackCancelled) ci.cancel();
            return;
        }
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
