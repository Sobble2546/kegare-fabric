package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;

/** Entity renderer registration. Three events: renderers, model layer definitions, and layers added to existing renderers. */
public abstract class EntityRenderersEvent extends Event {
    private EntityRenderersEvent() {}

    /** Where registrations go. Fabric's registries in the game, a recorder in tests. */
    public interface Registrar {
        <T extends Entity> void renderer(EntityType<? extends T> type, EntityRendererProvider<T> provider);

        void layerDefinition(ModelLayerLocation location, Supplier<LayerDefinition> definition);
    }

    public static final class RegisterRenderers extends EntityRenderersEvent {
        private final Registrar registrar;

        public RegisterRenderers(Registrar registrar) {
            this.registrar = registrar;
        }

        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
            registrar.renderer(type, provider);
        }
    }

    public static final class RegisterLayerDefinitions extends EntityRenderersEvent {
        private final Registrar registrar;

        public RegisterLayerDefinitions(Registrar registrar) {
            this.registrar = registrar;
        }

        public void registerLayerDefinition(ModelLayerLocation location, Supplier<LayerDefinition> definition) {
            registrar.layerDefinition(location, definition);
        }
    }

    /**
     * The player renderers just got built (after every resource reload): add layers to them. The renderers are
     * {@code AvatarRenderer}s, one per arm model; check with {@code instanceof AvatarRenderer<?>}.
     */
    public static final class AddLayers extends EntityRenderersEvent {
        private final Map<PlayerModelType, ? extends EntityRenderer<?, ?>> skins;
        private final EntityModelSet entityModels;

        public AddLayers(Map<PlayerModelType, ? extends EntityRenderer<?, ?>> skins, EntityModelSet entityModels) {
            this.skins = skins;
            this.entityModels = entityModels;
        }

        public Set<PlayerModelType> getSkins() {
            return skins.keySet();
        }

        public EntityRenderer<?, ?> getSkin(PlayerModelType model) {
            return skins.get(model);
        }

        public EntityModelSet getEntityModels() {
            return entityModels;
        }
    }
}
