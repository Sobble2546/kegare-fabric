package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import com.cnethuka.kegare.platform.client.event.MovementInputUpdateEvent;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MovementInputUpdateEvent: right after the player's input read the keys, before anything uses it. 1.21.11 keeps the keys in an
 * immutable {@code Input} record plus a move vector (x = left impulse, y = forward impulse); the listeners get a mutable view and
 * what they leave in it is written back.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerInputMixin {
    @Shadow
    public ClientInput input;

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void kegare$movementInput(CallbackInfo ci) {
        if (!com.cnethuka.kegare.platform.net.ClientNet.isActive()) return; // inert off a Kegare server
        Input keys = input.keyPresses;
        Vec2 move = input.getMoveVector();
        MovementInputUpdateEvent.Input view = ClientEventHooks.movementInput(new MovementInputUpdateEvent.Input(keys.forward(), keys.backward(),
                keys.left(), keys.right(), keys.jump(), keys.shift(), keys.sprint(), move.y, move.x));
        input.keyPresses = new Input(view.up, view.down, view.left, view.right, view.jumping, view.shiftKeyDown, view.sprinting);
        ((ClientInputAccessor) input).kegare$setMoveVector(new Vec2(view.leftImpulse, view.forwardImpulse));
    }
}
