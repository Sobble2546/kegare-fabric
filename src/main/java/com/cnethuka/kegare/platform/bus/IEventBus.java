package com.cnethuka.kegare.platform.bus;

import java.util.function.Consumer;

/** The subset of NeoForge's event bus Kegare uses: listeners by type, priorities, cancelling, annotated methods. */
public interface IEventBus {
    <T extends Event> void addListener(Listener<T> listener);

    <T extends Event> void addListener(EventPriority priority, Listener<T> listener);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Listener<T> listener);

    <T extends Event> void addListener(Class<T> type, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> listener);

    /** Subscribes every {@link SubscribeEvent} method of a class (static ones) or an object (instance and static ones). */
    void register(Object target);

    /** Removes a listener added with addListener, or everything {@link #register} added for the object/class. */
    void unregister(Object target);

    /** Delivers the event to every listener of its type or a supertype, in priority order, and returns it. */
    <T extends Event> T post(T event);
}
