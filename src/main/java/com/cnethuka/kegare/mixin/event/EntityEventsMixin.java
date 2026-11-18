package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.EntityTravelToDimensionEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The dimension change of everything but players ({@link ServerPlayerEventsMixin} has theirs, so it fires once). */
@Mixin(Entity.class)
public abstract class EntityEventsMixin {
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"), cancellable = true)
    private void kegare$travel(TeleportTransition transition, CallbackInfoReturnable<Entity> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayer || self.level().isClientSide() || transition.newLevel() == self.level()) return;
        if (NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(self, transition.newLevel().dimension())).isCanceled()) cir.setReturnValue(null);
    }
}
