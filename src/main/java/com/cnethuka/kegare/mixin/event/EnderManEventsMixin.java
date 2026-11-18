package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.event.EventHooks;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An enderman teleporting itself. It dodges projectiles in hurtServer BEFORE the damage events, so cancelling the hit is not enough to
 * keep a frozen one in place (TimeStops.onEnderTeleport): this is the place all its teleports pass.
 */
@Mixin(EnderMan.class)
public abstract class EnderManEventsMixin {
    @Inject(method = "teleport(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void kegare$teleport(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EnderMan self = (EnderMan) (Object) this;
        if (self.level().isClientSide()) return;
        if (EventHooks.onEnderTeleport(self, x, y, z).isCanceled()) cir.setReturnValue(false);
    }
}
