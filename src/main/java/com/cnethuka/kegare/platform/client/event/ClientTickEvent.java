package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;

/** Once at the start ({@link Pre}) and once at the end ({@link Post}) of every client tick. */
public abstract class ClientTickEvent extends Event {
    private ClientTickEvent() {}

    public static final class Pre extends ClientTickEvent {
        public Pre() {}
    }

    public static final class Post extends ClientTickEvent {
        public Post() {}
    }
}
