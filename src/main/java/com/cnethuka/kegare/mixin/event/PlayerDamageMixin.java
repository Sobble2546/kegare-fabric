package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.damage.DamagePipeline;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** {@link LivingEntityDamageMixin}'s {@code actuallyHurt} half, again: Player overrides it in full and never calls super (the main PvP target). */
@Mixin(Player.class)
public abstract class PlayerDamageMixin {
    @WrapMethod(method = "actuallyHurt")
    private void kegare$actuallyHurtScope(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
        DamagePipeline.Frame frame = DamagePipeline.beginActuallyHurt((Player) (Object) this, source, amount);
        boolean completed = false;
        try {
            original.call(level, source, amount);
            completed = true;
        } finally {
            DamagePipeline.endActuallyHurt(frame, completed);
        }
    }

    @ModifyVariable(method = "actuallyHurt", argsOnly = true, at = @At(value = "STORE", ordinal = 1))
    private float kegare$pre(float amount) {
        return DamagePipeline.afterDefences((Player) (Object) this, amount);
    }
}
