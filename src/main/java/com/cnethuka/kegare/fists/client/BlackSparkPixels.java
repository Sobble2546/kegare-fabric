package com.cnethuka.kegare.fists.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Black Spark's pixel art (DESIGN §3.28): which pixels are lit, and in which colour, at one moment of the burst. Plain arithmetic on a grid
 * of square pixels round the point of contact (x right, y up, as the camera sees it), so it can be checked without a game: the client
 * ({@link BlackSparkClient}) lays the grid on a plane facing the camera, 16 pixels to a block at full size.
 * <ul>
 *   <li>Jagged bolts crackling outward, stair-stepped (every step is a straight move, never a diagonal), black with a red outline. Their
 *       crooks are rerolled every tick, so they crackle; they shoot out at once and break up from the tips as it fades.</li>
 *   <li>A black starburst at the heart, rimmed in bright red and then deep red, shrinking away in the first half.</li>
 *   <li>Sparks: single black and red pixels (some two by two) thrown out and falling.</li>
 * </ul>
 * Black and red only, the same for everyone.
 */
public final class BlackSparkPixels {
    /** Near-black: the bolts' and the starburst's core. */
    public static final int CORE = 0x0A0505;
    /** Deep red: the starburst's outer rim and some of the sparks. */
    public static final int DEEP = 0x8B0000;
    /** Bright red: every outline, and some of the sparks. */
    public static final int EDGE = 0xE01B24;
    public static final int[] PALETTE = {CORE, DEEP, EDGE};

    /** One lit pixel: its place on the grid (pixels from the point of contact) and its colour. */
    public record Pixel(int x, int y, int rgb) {}

    private BlackSparkPixels() {}

    /**
     * Every lit pixel at this moment, each place once.
     *
     * @param seed   the burst's own (where its bolts point, where its sparks fly)
     * @param tick   the game tick (the bolts' crooks are new every tick)
     * @param t      how far through its life it is, 0 to 1
     * @param life   its life in ticks (the sparks fly for that long)
     * @param bolts  how many bolts
     * @param sparks how many sparks
     */
    public static List<Pixel> burst(long seed, long tick, float t, int life, int bolts, int sparks) {
        Map<Long, Integer> lit = new LinkedHashMap<>();
        // Outlines first, cores over them, sparks over everything: the map keeps one colour per place.
        List<long[]> core = new ArrayList<>();
        if (t < 0.5f) starburst(t, core, lit);
        bolts(seed, tick, t, bolts, core);
        for (long[] c : core) outline(c[0], c[1], EDGE, lit, false);
        for (long[] c : core) lit.put(key(c[0], c[1]), CORE);
        sparks(seed, t, life, sparks, lit);
        List<Pixel> out = new ArrayList<>(lit.size());
        lit.forEach((k, rgb) -> out.add(new Pixel((int) (k >> 32), (int) (long) k, rgb)));
        return out;
    }

    /** The heart: a pixel star (arms straight out and corner to corner, round a solid middle), shrinking away. */
    private static void starburst(float t, List<long[]> core, Map<Long, Integer> lit) {
        int r = Math.max(1, Math.round(4f * (1f - t / 0.5f)));
        List<long[]> star = new ArrayList<>();
        for (int k = 0; k <= r; k++) {
            star.add(new long[] {k, 0});
            star.add(new long[] {-k, 0});
            star.add(new long[] {0, k});
            star.add(new long[] {0, -k});
        }
        for (int k = 1; k < r; k++) {
            star.add(new long[] {k, k});
            star.add(new long[] {-k, k});
            star.add(new long[] {k, -k});
            star.add(new long[] {-k, -k});
        }
        if (r >= 2) for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) star.add(new long[] {x, y});
        // Deep red round the bright red rim: two rings out from the core.
        for (long[] c : star) for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) {
            if (Math.abs(dx) + Math.abs(dy) <= 2) lit.putIfAbsent(key(c[0] + dx, c[1] + dy), DEEP);
        }
        for (long[] c : star) outline(c[0], c[1], EDGE, lit, true);
        core.addAll(star);
    }

    /** The bolts: each a stair-stepped walk outward, crooked anew every tick, shooting out at once and breaking up from the tips. */
    static void bolts(long seed, long tick, float t, int bolts, List<long[]> core) {
        SplittableRandom shape = new SplittableRandom(seed);
        float reach = Math.min(1f, t * 4f + 0.35f);
        for (int a = 0; a < bolts; a++) {
            double angle = 2 * Math.PI * (a + 0.8 * shape.nextDouble()) / bolts;
            int length = 9 + shape.nextInt(6);
            SplittableRandom crackle = new SplittableRandom(seed * 31 + a * 7919L + tick * 104729L);
            List<long[]> path = walk(0, 0, angle, Math.max(1, Math.round(length * reach)), crackle);
            // A fork now and then, off the third to fifth step.
            if (path.size() > 5 && crackle.nextDouble() < 0.5) {
                long[] from = path.get(3 + crackle.nextInt(Math.min(3, path.size() - 4)));
                double fork = angle + (crackle.nextBoolean() ? 0.75 : -0.75);
                List<long[]> branch = walk(from[0], from[1], fork, 3 + crackle.nextInt(3), crackle);
                path.addAll(branch.subList(1, branch.size()));
            }
            // Two pixels thick for the inner half, tapering to one at the tips, so the black reads as the core and the red as its edge.
            boolean alongX = Math.abs(Math.cos(angle)) >= Math.abs(Math.sin(angle));
            int thick = path.size() / 2;
            for (int i = 0; i < path.size(); i++) {
                // Breaking up: past halfway, pixels drop out, the outer ones first.
                if (t > 0.45f && i > 0) {
                    float out = (float) i / path.size();
                    if (crackle.nextFloat() < (t - 0.45f) * 1.8f * (0.5f + 0.5f * out)) continue;
                }
                long[] p = path.get(i);
                core.add(p);
                if (i < thick) core.add(alongX ? new long[] {p[0], p[1] + 1} : new long[] {p[0] + 1, p[1]});
            }
        }
    }

    /**
     * A walk of {@code steps} pixels from (x, y) toward {@code angle}, drifting sideways a pixel at a time at random. Every move is
     * straight: where it would go corner to corner, it steps along first, then across, so it reads as pixel-art stairs.
     */
    private static List<long[]> walk(long x, long y, double angle, int steps, SplittableRandom crackle) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        boolean alongX = Math.abs(dx) >= Math.abs(dy);
        double major = Math.max(Math.abs(dx), Math.abs(dy));
        List<long[]> out = new ArrayList<>();
        out.add(new long[] {x, y});
        long px = x, py = y;
        int drift = 0;
        for (int s = 1; s <= steps; s++) {
            if (crackle.nextDouble() < 0.35) drift = Math.max(-(2 + s / 4), Math.min(2 + s / 4, drift + (crackle.nextBoolean() ? 1 : -1)));
            long nx = x + Math.round(s * dx / major) + (alongX ? 0 : drift);
            long ny = y + Math.round(s * dy / major) + (alongX ? drift : 0);
            // Step there straight: along the major axis, then across, one pixel at a time.
            while (px != nx || py != ny) {
                if (alongX ? px != nx : py == ny) px += Long.signum(nx - px);
                else py += Long.signum(ny - py);
                out.add(new long[] {px, py});
            }
        }
        return out;
    }

    /** The sparks: single pixels (every third two by two) thrown out from the heart and falling, black, deep red and bright red in turn. */
    private static void sparks(long seed, float t, int life, int sparks, Map<Long, Integer> lit) {
        SplittableRandom r = new SplittableRandom(seed ^ 0xC0FFEEL);
        float age = t * life;
        for (int i = 0; i < sparks; i++) {
            double angle = 2 * Math.PI * r.nextDouble();
            double speed = 1.6 + 2.2 * r.nextDouble();
            long x = Math.round(Math.cos(angle) * speed * age);
            long y = Math.round(Math.sin(angle) * speed * age - 0.35 * age * age);
            int rgb = switch (i % 3) {
                case 0 -> CORE;
                case 1 -> EDGE;
                default -> DEEP;
            };
            int size = i % 3 == 0 ? 2 : 1;
            for (int dx = 0; dx < size; dx++) for (int dy = 0; dy < size; dy++) lit.put(key(x + dx, y + dy), rgb);
        }
    }

    /** The four pixels next to (x, y), in this colour where nothing is yet (or, with {@code over}, over anything but a core pixel). */
    private static void outline(long x, long y, int rgb, Map<Long, Integer> lit, boolean over) {
        long[][] next = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (long[] n : next) {
            long k = key(x + n[0], y + n[1]);
            Integer there = lit.get(k);
            if (there == null || over && there != CORE) lit.put(k, rgb);
        }
    }

    private static long key(long x, long y) {
        return (x << 32) | (y & 0xFFFFFFFFL);
    }
}
