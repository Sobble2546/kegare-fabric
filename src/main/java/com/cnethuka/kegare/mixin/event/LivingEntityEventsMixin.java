package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.living.LivingDeathEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingEntityUseItemEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingHealEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingKnockBackEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingUseTotemEvent;
import com.cnethuka.kegare.platform.event.entity.living.MobEffectEvent;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's events on {@code LivingEntity} that Fabric has no (usable) event for: death, knockback, heal, totem, effects, item use.
 * Item use is common (client and server; AccelerateAbility keeps client item use in step with the server), the rest server only.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEventsMixin {
    @Shadow protected int useItemRemaining;

    @Shadow public abstract ItemStack getItemInHand(InteractionHand hand);

    /**
     * Death, once: a ServerPlayer's goes through ServerPlayer.die (before its irreversible work), anyone else's through here. Fabric's
     * ALLOW_DEATH only fires inside hurtServer and AFTER_DEATH is too late to cancel, so neither can stand in.
     */
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void kegare$death(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer || self.level().isClientSide()) return;
        if (NeoForge.EVENT_BUS.post(new LivingDeathEvent(self, source)).isCanceled()) ci.cancel();
    }

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void kegare$knockback(double strength, double x, double z, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (NeoForge.EVENT_BUS.post(new LivingKnockBackEvent(self, strength, x, z)).isCanceled()) ci.cancel();
    }

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void kegare$heal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (NeoForge.EVENT_BUS.post(new LivingHealEvent(self, amount)).isCanceled()) ci.cancel();
    }

    /** A totem saved them. The stack and hand are not tracked (nothing in Kegare reads them). */
    @Inject(method = "checkTotemDeathProtection(Lnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("RETURN"))
    private void kegare$totem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        NeoForge.EVENT_BUS.post(new LivingUseTotemEvent((LivingEntity) (Object) this, source, ItemStack.EMPTY, InteractionHand.MAIN_HAND));
    }

    /** Neo fires this just before inserting the effect and also for an update; here it is just after, and only when something changed. */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void kegare$effectAdded(MobEffectInstance effect, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!cir.getReturnValueZ() || self.level().isClientSide()) return;
        NeoForge.EVENT_BUS.post(new MobEffectEvent.Added(self, null, effect, source));
    }

    @Inject(method = "forceAddEffect", at = @At("TAIL"))
    private void kegare$effectForced(MobEffectInstance effect, @Nullable Entity source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        NeoForge.EVENT_BUS.post(new MobEffectEvent.Added(self, null, effect, source));
    }

    @Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
    private void kegare$startUsing(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack stack = getItemInHand(hand);
        if (stack.isEmpty() || self.isUsingItem()) return; // vanilla ignores those too
        if (NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Start(self, stack, stack.getUseDuration(self))).isCanceled()) ci.cancel();
    }

    /** Every tick of item use, on both sides; a listener may change the ticks left. */
    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void kegare$usingTick(ItemStack stack, CallbackInfo ci) {
        LivingEntityUseItemEvent.Tick event = new LivingEntityUseItemEvent.Tick((LivingEntity) (Object) this, stack, useItemRemaining);
        NeoForge.EVENT_BUS.post(event);
        useItemRemaining = event.getDuration();
    }
}
