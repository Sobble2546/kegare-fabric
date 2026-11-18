package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.damage.DamagePipeline;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's damage events on {@code LivingEntity} (map section 1; {@link DamagePipeline} does the work). {@code hurtServer} is where the
 * incoming event fires and the hurt cooldown is set; {@code actuallyHurt} is where the Pre and Post events fire.
 * {@link PlayerDamageMixin} repeats the {@code actuallyHurt} half for Player, which overrides it without calling super.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    /** The container for this hit lives for the whole of hurtServer. */
    @WrapMethod(method = "hurtServer")
    private boolean kegare$damageScope(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        DamagePipeline.Frame frame = DamagePipeline.begin((LivingEntity) (Object) this, source, amount);
        try {
            return original.call(level, source, amount);
        } finally {
            DamagePipeline.end(frame);
        }
    }

    /** After the early outs (invulnerable, dead, fire resistance), where NeoForge fires LivingIncomingDamageEvent (and Fabric's ALLOW_DAMAGE). */
    @Inject(method = "hurtServer", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z", ordinal = 0))
    private void kegare$incoming(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (DamagePipeline.incoming((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    /** Right after that: the amount is the container's (listeners may have changed it). */
    @ModifyVariable(method = "hurtServer", argsOnly = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z", ordinal = 0, shift = At.Shift.AFTER))
    private float kegare$amount(float amount) {
        return DamagePipeline.amount((LivingEntity) (Object) this, amount);
    }

    /** The one literal 20 in hurtServer is the hurt cooldown a fresh hit starts; the incoming event can set another. */
    @ModifyConstant(method = "hurtServer", constant = @Constant(intValue = 20))
    private int kegare$cooldown(int ticks) {
        return DamagePipeline.cooldown((LivingEntity) (Object) this, ticks);
    }

    @WrapMethod(method = "actuallyHurt")
    private void kegare$actuallyHurtScope(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
        DamagePipeline.Frame frame = DamagePipeline.beginActuallyHurt((LivingEntity) (Object) this, source, amount);
        boolean completed = false;
        try {
            original.call(level, source, amount);
            completed = true;
        } finally {
            DamagePipeline.endActuallyHurt(frame, completed);
        }
    }

    /** The store after getDamageAfterMagicAbsorb (the 2nd store to the amount): armor, effects and enchantments are done, absorption isn't. */
    @ModifyVariable(method = "actuallyHurt", argsOnly = true, at = @At(value = "STORE", ordinal = 1))
    private float kegare$pre(float amount) {
        return DamagePipeline.afterDefences((LivingEntity) (Object) this, amount);
    }
}
