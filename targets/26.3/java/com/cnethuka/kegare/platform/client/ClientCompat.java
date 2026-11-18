package com.cnethuka.kegare.platform.client;

import com.cnethuka.kegare.platform.client.event.EntityRenderersEvent;
import com.cnethuka.kegare.platform.client.event.ModelEvent;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Every Fabric-API / Minecraft spelling the client event shim needs that differs between Minecraft versions: this is the
 * 1.21.11 variant (targets/1.21.11/java). A 26.x variant of this class, same package and members, replaces it.
 */
final class ClientCompat {
    private ClientCompat() {}

    // ------------------------------------------------------------------------------------------------ ticks, connection, lifecycle

    static void registerTicks(Runnable start, Runnable end) {
        ClientTickEvents.START_CLIENT_TICK.register(mc -> start.run());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> end.run());
    }

    /** @param join a play connection became ready; @param leave the play connection ended (also called when the client quit a world) */
    static void registerConnection(BiConsumer<Minecraft, ClientPacketListener> join, BiConsumer<Minecraft, ClientPacketListener> leave) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> join.accept(mc, handler));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> leave.accept(mc, handler));
    }

    static void onClientStarted(Runnable work) {
        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> work.run());
    }

    // ------------------------------------------------------------------------------------------------ keys

    static KeyMapping.Category keyCategory(Identifier id) {
        return KeyMapping.Category.register(id);
    }

    static KeyMapping newKey(String name, InputConstants.Type type, int code, KeyMapping.Category category) {
        return new KeyMapping(name, type, code, category);
    }

    static void registerKey(KeyMapping mapping) {
        KeyMappingHelper.registerKeyMapping(mapping);
    }

    // ------------------------------------------------------------------------------------------------ resources

    /** Registers a client resource reload listener under this id; it runs after vanilla's own. */
    static void registerReloader(Identifier id, PreparableReloadListener listener) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id, listener);
    }

    /** Same, but ordered after the model manager (so the models it bakes are ready). */
    static void registerReloaderAfterModels(Identifier id, PreparableReloadListener listener) {
        ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(id, listener);
        loader.addListenerOrdering(ResourceReloaderKeys.Client.MODELS, id);
    }

    // ------------------------------------------------------------------------------------------------ renderers, layers, models

    static EntityRenderersEvent.Registrar rendererRegistrar() {
        return new EntityRenderersEvent.Registrar() {
            @Override
            public <T extends Entity> void renderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                EntityRendererRegistry.register(type, provider);
            }

            @Override
            public void layerDefinition(ModelLayerLocation location, Supplier<LayerDefinition> definition) {
                ModelLayerRegistry.registerModelLayer(location, definition::get);
            }
        };
    }

    /** Calls {@code poster} with a fresh RegisterAdditional on every model (re)load. */
    static void registerExtraModels(Consumer<ModelEvent.RegisterAdditional> poster) {
        ModelLoadingPlugin.register(context -> poster.accept(new ModelEvent.RegisterAdditional(id -> {
            ExtraModelKey<BlockStateModel> key = ModelEvent.key(id);
            context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id));
            return key;
        })));
    }
}
