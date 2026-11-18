package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import java.util.concurrent.CompletableFuture;

/** The client finished starting up (Fabric's CLIENT_STARTED). Work enqueued here runs at once, on the client thread. */
public final class FMLClientSetupEvent extends Event {
    public FMLClientSetupEvent() {}

    public CompletableFuture<Void> enqueueWork(Runnable work) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        try {
            work.run();
            done.complete(null);
        } catch (Throwable t) {
            done.completeExceptionally(t);
            throw t;
        }
        return done;
    }
}
