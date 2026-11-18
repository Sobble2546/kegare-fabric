package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.player.AttackEntityEvent;
import com.cnethuka.kegare.platform.event.entity.player.CriticalHitEvent;
import com.cnethuka.kegare.platform.event.tick.PlayerTickEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NeoForge's player events with no Fabric twin: the attack, the critical hit decision, and the tick (both sides, as NeoForge fires them). */
@Mixin(Player.class)
public abstract class PlayerEventsMixin {
    /** Cancel: no attack at all. Fabric's AttackEntityCallback is packet-only and misses mod code calling attack(). */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void kegare$attack(Entity target, CallbackInfo ci) {
        if (NeoForge.EVENT_BUS.post(new AttackEntityEvent((Player) (Object) this, target)).isCanceled()) ci.cancel();
    }

    /** The critical hit decision, before any damage event of this swing, so listeners can mark it. */
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean kegare$critical(Player self, Entity target, Operation<Boolean> original) {
        boolean vanilla = original.call(self, target);
        CriticalHitEvent event = new CriticalHitEvent(self, target, vanilla ? 1.5f : 1.0f, vanilla);
        NeoForge.EVENT_BUS.post(event);
        return event.isCriticalHit();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void kegare$tickPre(CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre((Player) (Object) this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void kegare$tickPost(CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post((Player) (Object) this));
    }
}
