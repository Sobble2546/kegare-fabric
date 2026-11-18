package com.cnethuka.kegare.platform.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HandOffTest {
    @Test
    void anItemAddedOnAnotherThreadIsDeliveredOnceOnTheDrainingThread() throws InterruptedException {
        HandOff<String> handOff = new HandOff<>();
        List<String> seen = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        Thread network = new Thread(() -> handOff.add("left"), "netty-stand-in");
        network.start();
        network.join();
        assertEquals(0, seen.size(), "nothing is delivered until someone drains");
        handOff.drain(item -> {
            seen.add(item);
            threads.add(Thread.currentThread());
        });
        handOff.drain(seen::add); // a second drain finds nothing: each item goes to one caller
        assertEquals(List.of("left"), seen);
        assertEquals(Thread.currentThread(), threads.get(0));
        assertNotEquals(network, threads.get(0));
    }

    @Test
    void itemsComeOutInTheOrderTheyWentIn() {
        HandOff<Integer> handOff = new HandOff<>();
        handOff.add(1);
        handOff.add(2);
        List<Integer> seen = new ArrayList<>();
        handOff.drain(seen::add);
        assertEquals(List.of(1, 2), seen);
    }
}
