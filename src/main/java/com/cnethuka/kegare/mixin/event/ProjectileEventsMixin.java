package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.ProjectileImpactEvent;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every projectile's hit (throwables, arrows, fireballs, fishing hooks, shulker bullets...) passes {@code hitTargetOrDeflectSelf}. Server only. */
@Mixin(Projectile.class)
public abstract class ProjectileEventsMixin {
    @Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
    private void kegare$impact(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> cir) {
        Projectile self = (Projectile) (Object) this;
        if (self.level().isClientSide()) return;
        if (NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(self, hit)).isCanceled()) cir.setReturnValue(ProjectileDeflection.NONE);
    }
}
