package com.cnethuka.kegare.platform.bus;

/** Base of everything posted on an {@link IEventBus}. Cancellable events also implement {@link ICancellableEvent}. */
public abstract class Event {
    private boolean canceled;

    boolean canceledFlag() {
        return canceled;
    }

    void canceledFlag(boolean value) {
        canceled = value;
    }
}
