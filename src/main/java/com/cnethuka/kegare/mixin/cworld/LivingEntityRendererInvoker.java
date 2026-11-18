package com.cnethuka.kegare.mixin.cworld;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code LivingEntityRenderer.addLayer} is protected and final; this opens it so Kegare's layers can be added to the player renderers. */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {
    @Invoker("addLayer")
    <S extends LivingEntityRenderState, M extends EntityModel<? super S>> boolean kegare$addLayer(RenderLayer<S, M> layer);
}
