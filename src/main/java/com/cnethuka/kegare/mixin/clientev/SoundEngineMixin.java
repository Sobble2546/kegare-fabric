package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** PlaySoundEvent: a sound about to start. Silenced, nothing starts; a replacement plays in its place. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    /** On while the replacement of a sound is being played, so it is not offered to the listeners again. */
    private boolean kegare$replacing;

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void kegare$sound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (kegare$replacing) return;
        @Nullable SoundInstance result = ClientEventHooks.sound(sound);
        if (result == sound) return;
        if (result == null) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        kegare$replacing = true;
        try {
            cir.setReturnValue(((SoundEngine) (Object) this).play(result));
        } finally {
            kegare$replacing = false;
        }
    }
}
