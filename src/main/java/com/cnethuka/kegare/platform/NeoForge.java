package com.cnethuka.kegare.platform;

import com.cnethuka.kegare.platform.bus.EventBus;

/** Where NeoForge keeps the game-wide event bus. Everything gameplay related is posted here; see {@code event.ServerEventHooks}. */
public final class NeoForge {
    private NeoForge() {}

    public static final EventBus EVENT_BUS = new EventBus();
}
