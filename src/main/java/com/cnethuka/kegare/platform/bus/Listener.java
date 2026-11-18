package com.cnethuka.kegare.platform.bus;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * A listener lambda or method reference. It is {@link Serializable} only so the bus can read the event type back
 * from the lambda ({@link java.lang.invoke.SerializedLambda}); nothing is ever serialized.
 */
@FunctionalInterface
public interface Listener<T extends Event> extends Consumer<T>, Serializable {
}
