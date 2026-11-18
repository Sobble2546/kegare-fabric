package com.cnethuka.kegare.mixin;

import com.cnethuka.kegare.platform.MobReach;
import com.cnethuka.kegare.sight.DelayedSight;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Too fast for the eye (DESIGN §3.30): a mob swings at where it sees a delayed player (where they were), and the swing lands only if the
 * player is really there too: hits are judged against the real position, and no reach is widened. Bosses see the real position.
 */
@Mixin(Mob.class)
public abstract class MobSightMixin {
    /** The range check measures against the target's box; for a misled mob that is where it sees them (the weapon's reach is vanilla's own). */
    @WrapOperation(method = "isWithinMeleeAttackRange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHitbox()Lnet/minecraft/world/phys/AABB;"))
    private AABB kegare$swingAtTheImage(LivingEntity target, Operation<AABB> original) {
        Mob self = (Mob) (Object) this;
        return DelayedSight.misleads(self, target) ? DelayedSight.seenHitbox(self, target) : original.call(target);
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void kegare$missWhereTheyWere(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (target instanceof LivingEntity living && DelayedSight.misleads(self, living) && !MobReach.attackBox(self).intersects(living.getBoundingBox())) {
            cir.setReturnValue(false); // swung at where they were; they've gone
        }
    }
}
