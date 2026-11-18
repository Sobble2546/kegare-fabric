package com.cnethuka.kegare.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.platform.bus.Event;
import com.cnethuka.kegare.platform.bus.EventBus;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import com.cnethuka.kegare.platform.bus.IEventBus;
import com.cnethuka.kegare.platform.bus.SubscribeEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventBusTest {
    static class Base extends Event {}

    static class Sub extends Base implements ICancellableEvent {}

    @Test
    void lambdaTypeIsReadFromItsParameter() {
        IEventBus bus = new EventBus();
        List<String> seen = new ArrayList<>();
        bus.addListener((Sub e) -> seen.add("sub"));
        bus.addListener((Base e) -> seen.add("base"));
        bus.post(new Sub());
        assertEquals(List.of("sub", "base"), seen);
        seen.clear();
        bus.post(new Base());
        assertEquals(List.of("base"), seen);
    }

    @Test
    void priorityOrderAndCancelling() {
        IEventBus bus = new EventBus();
        List<String> seen = new ArrayList<>();
        bus.addListener(EventPriority.LOWEST, (Sub e) -> seen.add("lowest"));
        bus.addListener(EventPriority.HIGHEST, (Sub e) -> { seen.add("highest"); e.setCanceled(true); });
        bus.addListener(EventPriority.LOW, true, (Sub e) -> seen.add("low-sees-canceled"));
        Sub event = bus.post(new Sub());
        assertTrue(event.isCanceled());
        assertEquals(List.of("highest", "low-sees-canceled"), seen);
    }

    static void staticListener(Sub e) {
        COUNT++;
    }

    static int COUNT;

    @Test
    void methodReferenceAndUnregister() {
        IEventBus bus = new EventBus();
        COUNT = 0;
        bus.addListener(EventBusTest::staticListener);
        bus.post(new Sub());
        assertEquals(1, COUNT);
    }

    static class Annotated {
        int hits;

        @SubscribeEvent
        void on(Sub e) {
            hits++;
        }
    }

    @Test
    void annotatedObjectsRegisterAndUnregister() {
        IEventBus bus = new EventBus();
        Annotated a = new Annotated();
        bus.register(a);
        bus.post(new Sub());
        bus.unregister(a);
        bus.post(new Sub());
        assertEquals(1, a.hits);
    }
}
