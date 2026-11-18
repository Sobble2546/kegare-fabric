package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.resources.Identifier;

/**
 * HUD layers are being registered. A layer is a {@link HudElement} ({@code render(GuiGraphics, DeltaTracker)}, the shape
 * NeoForge's {@code LayeredDraw.Layer} had). "Above" is drawn after the other layer, "below" before it.
 */
public final class RegisterGuiLayersEvent extends Event {
    /** Where the layers go; the Fabric HUD registry, or a recorder in tests. */
    public interface Registrar {
        void attachAfter(Identifier other, Identifier id, HudElement layer);

        void attachBefore(Identifier other, Identifier id, HudElement layer);
    }

    private final Registrar registrar;

    public RegisterGuiLayersEvent(Registrar registrar) {
        this.registrar = registrar;
    }

    public void registerAbove(Identifier other, Identifier id, HudElement layer) {
        registrar.attachAfter(other, id, layer);
    }

    public void registerBelow(Identifier other, Identifier id, HudElement layer) {
        registrar.attachBefore(other, id, layer);
    }
}
