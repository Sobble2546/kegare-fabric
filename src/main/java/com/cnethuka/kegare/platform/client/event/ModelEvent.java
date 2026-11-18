package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.Identifier;

/** Extra models: asked for before every model reload ({@link RegisterAdditional}) and told when they are baked ({@link BakingCompleted}). */
public abstract class ModelEvent extends Event {
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> KEYS = new ConcurrentHashMap<>();

    private ModelEvent() {}

    /**
     * The key an extra block-state model of this id is stored under, the same key every time. Read the baked model with
     * {@code Minecraft.getInstance().getModelManager().getModel(key)}.
     */
    public static ExtraModelKey<BlockStateModel> key(Identifier id) {
        return KEYS.computeIfAbsent(id, i -> ExtraModelKey.create(i::toString));
    }

    public static final class RegisterAdditional extends ModelEvent {
        private final Function<Identifier, ExtraModelKey<BlockStateModel>> loader;

        /** @param loader asks the model loader to load the model of that id and returns its key (see ClientCompat) */
        public RegisterAdditional(Function<Identifier, ExtraModelKey<BlockStateModel>> loader) {
            this.loader = loader;
        }

        /** Asks for the block-state model of this id to be loaded; returns its key. */
        public ExtraModelKey<BlockStateModel> register(Identifier id) {
            return loader.apply(id);
        }
    }

    /** The models were just baked: anything cached from the old ones is stale. */
    public static final class BakingCompleted extends ModelEvent {
        public BakingCompleted() {}
    }
}
