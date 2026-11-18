package com.cnethuka.kegare.platform.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Things one thread has to tell another: any thread {@link #add}s, and whoever {@link #drain}s runs the consumer on its own thread, once per
 * item. Used for a disconnect, which Fabric announces on the network thread and the client's own thread must announce to the mod.
 */
public final class HandOff<T> {
    private final Queue<T> waiting = new ConcurrentLinkedQueue<>();

    public void add(T item) {
        waiting.add(item);
    }

    /** Gives everything that is waiting to {@code consumer}, on the calling thread; each item goes to exactly one caller. */
    public void drain(Consumer<? super T> consumer) {
        for (T item; (item = waiting.poll()) != null; ) consumer.accept(item);
    }
}
