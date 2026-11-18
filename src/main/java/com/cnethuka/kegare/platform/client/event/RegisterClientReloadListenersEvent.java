package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import java.util.function.Consumer;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/** Resource reload listeners (resource packs, F3+T) are being registered. */
public final class RegisterClientReloadListenersEvent extends Event {
    private final Consumer<PreparableReloadListener> registrar;

    public RegisterClientReloadListenersEvent(Consumer<PreparableReloadListener> registrar) {
        this.registrar = registrar;
    }

    public void registerReloadListener(PreparableReloadListener listener) {
        registrar.accept(listener);
    }
}
