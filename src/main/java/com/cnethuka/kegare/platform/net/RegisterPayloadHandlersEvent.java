package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.platform.bus.Event;

/** Posted once on the mod bus by {@link PlatformNet#init}; listeners register their payloads on the registrar. */
public final class RegisterPayloadHandlersEvent extends Event {
    private final PayloadRegistrar.Backend backend;

    public RegisterPayloadHandlersEvent(PayloadRegistrar.Backend backend) {
        this.backend = backend;
    }

    /** A registrar on the main thread. The version is only kept for call compatibility: the hello checks it (DESIGN §3.5). */
    public PayloadRegistrar registrar(String version) {
        return new PayloadRegistrar(backend, HandlerThread.MAIN);
    }
}
