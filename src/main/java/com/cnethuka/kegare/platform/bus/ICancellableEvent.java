package com.cnethuka.kegare.platform.bus;

/** An {@link Event} a listener can cancel. Later listeners skip a canceled event unless they ask to receive it. */
public interface ICancellableEvent {
    default boolean isCanceled() {
        return ((Event) this).canceledFlag();
    }

    default void setCanceled(boolean canceled) {
        ((Event) this).canceledFlag(canceled);
    }
}
