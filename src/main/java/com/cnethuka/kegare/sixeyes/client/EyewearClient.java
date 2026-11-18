package com.cnethuka.kegare.sixeyes.client;

import com.cnethuka.kegare.client.visual.PlayerLayers;
import com.cnethuka.kegare.platform.client.event.EntityRenderersEvent;
import com.cnethuka.kegare.platform.bus.IEventBus;
import com.cnethuka.kegare.platform.client.event.FMLClientSetupEvent;
import com.cnethuka.kegare.platform.client.event.RegisterClientReloadListenersEvent;
import com.cnethuka.kegare.registry.KegareItems;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Eyewear on the client (DESIGN §3.32): each item's renderer on the face (drawn by the equipment slot mod's renderer, see
 * {@code EyewearPlatform}), and the layer that gives a disguise the face's eyewear.
 */
public final class EyewearClient {
    private static final EyewearRenderer BLINDFOLD = new EyewearRenderer("blindfold", RenderTypes::entityCutoutNoCull);
    private static final EyewearRenderer SUNGLASSES = new EyewearRenderer("sunglasses", RenderTypes::entityTranslucent);

    private EyewearClient() {}

    /** The renderer for a piece of eyewear, or null for none. */
    @javax.annotation.Nullable
    static EyewearRenderer renderer(com.cnethuka.kegare.sixeyes.Eyewear eyewear) {
        return switch (eyewear) {
            case BLINDFOLD -> BLINDFOLD;
            case SUNGLASSES -> SUNGLASSES;
            case NONE -> null;
        };
    }

    public static void init(IEventBus modBus) {
        // A disguise wears the face's eyewear: a layer on every player renderer draws it.
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> PlayerLayers.add(e, DisguisedEyewearLayer::new));
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(EyewearClient::setup));
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) manager -> {
            BLINDFOLD.forget();
            SUNGLASSES.forget();
        }));
    }

    private static void setup() {
        // Trinkets is optional: without it nobody can wear eyewear, and naming its classes would fail to load.
        if (!com.cnethuka.kegare.sixeyes.Eyewear.slotAvailable()) return;
        EyewearPlatform.register(KegareItems.BLINDFOLD.get(), BLINDFOLD);
        EyewearPlatform.register(KegareItems.SUNGLASSES.get(), SUNGLASSES);
    }
}
