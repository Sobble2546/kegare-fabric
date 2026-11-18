package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RenderNameTagEvent: name tags are worked out while the render state is extracted ({@code state.nameTag}, null = none). Fired at the
 * end of the base extraction, once per entity per frame that has a tag; the listeners' text or "hide" is written back into the state.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("RETURN"))
    private void kegare$nameTag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (state.nameTag == null) return;
        state.nameTag = ClientEventHooks.nameTag(entity, state.nameTag, partialTick);
    }
}
