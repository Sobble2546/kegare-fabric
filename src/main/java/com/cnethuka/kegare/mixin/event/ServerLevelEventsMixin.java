package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.EntityJoinLevelEvent;
import com.cnethuka.kegare.platform.event.level.SleepFinishedTimeEvent;
import com.cnethuka.kegare.platform.event.tick.EntityTickEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's entity tick and join events on the server's level. Fabric's ENTITY_LOAD fires after the entity is in and cannot cancel;
 * BattleDamage cancels the item entities of shattered blocks. (Entities loaded with a chunk do not pass through addEntity: no event.)
 * {@code client.ClientLevelEventsMixin} is this for the client's level.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelEventsMixin {
    /** Pre can cancel the tick (a frozen entity); Post follows a tick that ran. */
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

    @Inject(method = "addEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void kegare$join(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, (ServerLevel) (Object) this)).isCanceled()) cir.setReturnValue(false);
    }

    @Inject(method = "addPlayer(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true)
    private void kegare$joinPlayer(ServerPlayer player, CallbackInfo ci) {
        if (NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(player, (ServerLevel) (Object) this)).isCanceled()) ci.cancel();
    }

    /** Enough players slept through the night: just before they are woken (the time has been moved on by then). */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;wakeUpAllPlayers()V"))
    private void kegare$nightSlept(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        NeoForge.EVENT_BUS.post(new SleepFinishedTimeEvent(self, self.getDayTime()));
    }
}
