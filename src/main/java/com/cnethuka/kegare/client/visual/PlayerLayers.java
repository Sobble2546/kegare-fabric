package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.mixin.cworld.LivingEntityRendererInvoker;
import com.cnethuka.kegare.platform.client.event.EntityRenderersEvent;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Adds a layer to both player renderers (wide and slim arms) when {@link EntityRenderersEvent.AddLayers} fires. The renderer's
 * {@code addLayer} is protected, so it goes through {@link LivingEntityRendererInvoker}.
 */
public final class PlayerLayers {
    private PlayerLayers() {}

    /** A layer for a player renderer: it gets the renderer as its parent (so {@code getParentModel()} is that renderer's player model). */
    public static void add(EntityRenderersEvent.AddLayers event,
                           Function<RenderLayerParent<AvatarRenderState, PlayerModel>, RenderLayer<AvatarRenderState, PlayerModel>> layer) {
        for (PlayerModelType type : event.getSkins()) {
            EntityRenderer<?, ?> renderer = event.getSkin(type);
            if (!(renderer instanceof AvatarRenderer<?>)) continue;
            if (!(renderer instanceof LivingEntityRendererInvoker invoker)) {
                missingInvoker(renderer);
                continue;
            }
            @SuppressWarnings("unchecked")
            RenderLayerParent<AvatarRenderState, PlayerModel> parent = (RenderLayerParent<AvatarRenderState, PlayerModel>) renderer;
            invoker.kegare$addLayer(layer.apply(parent));
        }
    }

    /** The mixin config didn't apply: the layer is left off (logged), rather than the resource reload crashing. */
    private static void missingInvoker(EntityRenderer<?, ?> renderer) {
        Kegare.LOGGER.error("Can't add a layer to {}: the cworld mixin did not apply; that layer won't draw", renderer.getClass().getName());
    }

    /** Makes a layer for one player renderer; {@code slim} is its arm model, {@code models} bakes extra models for the layer. */
    public interface Factory {
        RenderLayer<AvatarRenderState, PlayerModel> create(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models, boolean slim);
    }

    /** The same, for a layer that bakes a model of its own (a frost body, say): it also gets the entity models and which arms the renderer has. */
    public static void addWithModels(EntityRenderersEvent.AddLayers event, Factory layer) {
        for (PlayerModelType type : event.getSkins()) {
            EntityRenderer<?, ?> renderer = event.getSkin(type);
            if (!(renderer instanceof AvatarRenderer<?>)) continue;
            if (!(renderer instanceof LivingEntityRendererInvoker invoker)) {
                missingInvoker(renderer);
                continue;
            }
            @SuppressWarnings("unchecked")
            RenderLayerParent<AvatarRenderState, PlayerModel> parent = (RenderLayerParent<AvatarRenderState, PlayerModel>) renderer;
            invoker.kegare$addLayer(layer.create(parent, event.getEntityModels(), type == PlayerModelType.SLIM));
        }
    }

    /** The entity this state was extracted from, if it is still in the level (layers get the state, not the player). */
    @Nullable
    public static Entity entity(AvatarRenderState state) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.getEntity(state.id);
    }
}
