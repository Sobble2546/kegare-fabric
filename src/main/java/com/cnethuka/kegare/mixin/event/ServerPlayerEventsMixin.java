package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.EntityTravelToDimensionEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingDeathEvent;
import com.cnethuka.kegare.platform.event.entity.player.PlayerContainerEvent;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The {@code ServerPlayer} overrides of hooks {@link LivingEntityEventsMixin} / {@link EntityEventsMixin} place on the shared code. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEventsMixin {
    /** A player's death: before ServerPlayer.die does its irreversible work (the death message), so a cancel is clean. Fires once per death. */
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void kegare$death(DamageSource source, CallbackInfo ci) {
        if (NeoForge.EVENT_BUS.post(new LivingDeathEvent((ServerPlayer) (Object) this, source)).isCanceled()) ci.cancel();
    }

    /** openMenu assigns containerMenu after its initMenu, so the event waits for the end; a cancelled/refused menu (empty result) opens nothing. */
    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At("RETURN"))
    private void kegare$opened(net.minecraft.world.MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (!cir.getReturnValue().isPresent()) return;
        ServerPlayer self = (ServerPlayer) (Object) this;
        NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(self, self.containerMenu));
    }

    @Inject(method = "openHorseInventory", at = @At("TAIL"))
    private void kegare$openedHorse(net.minecraft.world.entity.animal.equine.AbstractHorse horse, net.minecraft.world.Container container, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(self, self.containerMenu));
    }

    /** The nautilus' inventory is opened the same way as the horse's (a possessing player must not be able to open either). */
    @Inject(method = "openNautilusInventory", at = @At("TAIL"))
    private void kegare$openedNautilus(net.minecraft.world.entity.animal.nautilus.AbstractNautilus nautilus, net.minecraft.world.Container container, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(self, self.containerMenu));
    }

    /** A move to another dimension (ServerPlayer overrides Entity.teleport and returns the player). */
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("HEAD"), cancellable = true)
    private void kegare$travel(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (transition.newLevel() == self.level()) return;
        if (NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(self, transition.newLevel().dimension())).isCanceled()) cir.setReturnValue(null);
    }
}
