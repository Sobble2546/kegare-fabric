package com.cnethuka.kegare.fists.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Black Spark's pixel art (DESIGN §3.28): black and red only, one colour per pixel, stair-stepped bolts that crackle and break up. */
class BlackSparkPixelsTest {
    private static final float[] MOMENTS = {0f, 0.1f, 0.3f, 0.5f, 0.7f, 0.9f, 0.99f};

    private static Set<String> places(List<BlackSparkPixels.Pixel> pixels) {
        return pixels.stream().map(p -> p.x() + "," + p.y()).collect(Collectors.toSet());
    }

    @Test
    void blackAndRedOnlyAndEachPixelOnce() {
        Set<Integer> palette = Set.of(BlackSparkPixels.CORE, BlackSparkPixels.DEEP, BlackSparkPixels.EDGE);
        for (long seed = 1; seed <= 20; seed++) {
            for (float t : MOMENTS) {
                for (int[] kind : new int[][] {{6, 7, 14}, {4, 4, 6}}) {
                    List<BlackSparkPixels.Pixel> pixels = BlackSparkPixels.burst(seed, 100 + seed, t, kind[0], kind[1], kind[2]);
                    for (BlackSparkPixels.Pixel p : pixels) assertTrue(palette.contains(p.rgb()), "colour " + Integer.toHexString(p.rgb()));
                    assertEquals(pixels.size(), places(pixels).size(), "one colour per pixel");
                }
            }
        }
    }

    @Test
    void theBoltsAreStairSteppedNeverCornerToCorner() {
        for (long seed = 1; seed <= 30; seed++) {
            List<long[]> core = new ArrayList<>();
            BlackSparkPixels.bolts(seed, 7 + seed, 0.2f, 7, core);
            Set<String> lit = new HashSet<>();
            for (long[] c : core) lit.add(c[0] + "," + c[1]);
            for (long[] c : core) {
                boolean touches = lit.contains((c[0] + 1) + "," + c[1]) || lit.contains((c[0] - 1) + "," + c[1])
                        || lit.contains(c[0] + "," + (c[1] + 1)) || lit.contains(c[0] + "," + (c[1] - 1));
                assertTrue(touches, "seed " + seed + ": pixel " + c[0] + "," + c[1] + " joins its bolt along a side, not only a corner");
            }
        }
    }

    @Test
    void theyCrackleEveryTickAndBreakUpAsTheyFade() {
        assertEquals(places(BlackSparkPixels.burst(5, 40, 0.3f, 6, 7, 14)), places(BlackSparkPixels.burst(5, 40, 0.3f, 6, 7, 14)), "the same moment, the same pixels");
        assertNotEquals(places(BlackSparkPixels.burst(5, 40, 0.3f, 6, 7, 0)), places(BlackSparkPixels.burst(5, 41, 0.3f, 6, 7, 0)),
                "the next tick, the bolts take new crooks");
        int whole = 0, broken = 0;
        for (long seed = 1; seed <= 20; seed++) {
            List<long[]> early = new ArrayList<>(), late = new ArrayList<>();
            BlackSparkPixels.bolts(seed, 3, 0.3f, 7, early);
            BlackSparkPixels.bolts(seed, 3, 0.95f, 7, late);
            whole += early.size();
            broken += late.size();
        }
        assertTrue(broken < whole * 0.7, "breaking up: " + broken + " of " + whole);
    }

    @Test
    void aBlackHeartRimmedInRedThatGoesFirst() {
        List<BlackSparkPixels.Pixel> start = BlackSparkPixels.burst(9, 1, 0.05f, 6, 7, 14);
        assertTrue(start.contains(new BlackSparkPixels.Pixel(0, 0, BlackSparkPixels.CORE)), "black at the heart");
        assertTrue(start.stream().anyMatch(p -> p.rgb() == BlackSparkPixels.EDGE) && start.stream().anyMatch(p -> p.rgb() == BlackSparkPixels.DEEP),
                "rimmed in bright red and deep red");
        assertTrue(BlackSparkPixels.burst(9, 1, 0.6f, 6, 7, 0).stream().noneMatch(p -> p.rgb() == BlackSparkPixels.DEEP), "the starburst is gone by then");
    }

    @Test
    void reducedIsLess() {
        int full = 0, reduced = 0;
        for (long seed = 1; seed <= 20; seed++) {
            full += BlackSparkPixels.burst(seed, 2, 0.2f, 6, 7, 14).size();
            reduced += BlackSparkPixels.burst(seed, 2, 0.2f, 4, 4, 6).size();
        }
        assertTrue(reduced < full * 0.8, reduced + " pixels against " + full);
    }
}
