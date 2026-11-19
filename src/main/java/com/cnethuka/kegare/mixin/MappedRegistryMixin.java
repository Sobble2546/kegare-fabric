package com.cnethuka.kegare.mixin;

import com.cnethuka.kegare.api.KegareApi;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.innatetechnique.change.TechniqueViews;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's registry bake callback, for the innate technique registry: once it is complete (frozen, after every mod's initialiser
 * registered its techniques and changes), addons' changes are applied to every technique (DESIGN §3.34). On a launch Fabric freezes the
 * registries once, at the end; should anything freeze it again, baking again gives the same result.
 */
@Mixin(MappedRegistry.class)
abstract class MappedRegistryMixin<T> {
    @SuppressWarnings("unchecked")
    @Inject(method = "freeze", at = @At("RETURN"))
    private void kegare$bakeTechniques(CallbackInfoReturnable<Registry<T>> cir) {
        Registry<T> self = (Registry<T>) (Object) this;
        if (self.key().equals(KegareApi.INNATE_TECHNIQUES)) TechniqueViews.bake((Registry<InnateTechnique>) (Object) self);
    }
}
