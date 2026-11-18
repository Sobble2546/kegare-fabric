package com.cnethuka.kegare.mixin.event.client;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.EntityJoinLevelEvent;
import com.cnethuka.kegare.platform.event.tick.EntityTickEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The client's twin of {@code ServerLevelEventsMixin}: TimeStopClient freezes entities (entity tick Pre) and tags the flight of projectiles
 * that join (entity join), both on the client's level.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelEventsMixin {
    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void kegare$tick(Entity entity, Operation<Void> original) {
        if (NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity)).isCanceled()) return;
        original.call(entity);
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
    }

    @WrapOperation(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void kegare$rideTick(Entity entity, Operation<Void> original) {
        if (NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity)).isCanceled()) return;
        original.call(entity);
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void kegare$join(Entity entity, CallbackInfo ci) {
        if (NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, (ClientLevel) (Object) this)).isCanceled()) ci.cancel();
    }
}
