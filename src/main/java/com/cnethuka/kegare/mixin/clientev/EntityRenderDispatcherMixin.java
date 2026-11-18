package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EntityRenderersEvent.AddLayers: the player renderers were just (re)built on a resource reload; layers must be added to the new ones. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers;
    @Shadow
    @Final
    private Supplier<EntityModelSet> entityModels;

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void kegare$addLayers(ResourceManager manager, CallbackInfo ci) {
        ClientEventHooks.addPlayerLayers(playerRenderers, entityModels.get());
    }
}
