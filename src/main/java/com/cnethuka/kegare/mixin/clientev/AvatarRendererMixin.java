package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RenderPlayerEvent.Pre / Post around a player's render-state extraction. On 1.21.11 a player's armor and held items are read into
 * the state at extraction, so anything swapped in Pre and put back in Post decides what is drawn. Players only (mannequins skip).
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    // The descriptor is spelled out: the generic method has a bridge with the same name that must not be hooked as well.
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("HEAD"))
    private void kegare$before(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof Player player) ClientEventHooks.playerExtractStart(player, (AvatarRenderer<?>) (Object) this, state, partialTick);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void kegare$after(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof Player player) ClientEventHooks.playerExtractEnd(player, (AvatarRenderer<?>) (Object) this, state, partialTick);
    }
}
