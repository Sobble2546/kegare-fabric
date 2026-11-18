package com.cnethuka.kegare.impact.client;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.client.visual.FullscreenFx;
import com.cnethuka.kegare.impact.ScreenEffects;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * What the Impact System does to the whole picture (DESIGN §3.23), after the world and your hand are drawn and before the HUD (called
 * from {@code GameRendererMixin}):
 * <ul>
 * <li><b>Hit-stop</b>: the frame a hit lands on is shown again for a few frames, freezing the picture. Only the picture: the game
 * ticks on underneath, keys are read, the HUD stays live, and when it ends everything shows where it really is.</li>
 * <li><b>Screen effects</b>: a full-screen shader pass ({@link FullscreenFx}; {@code assets/<namespace>/shaders/post/<path>.fsh},
 * GLSL 330, reading the picture as {@code DiffuseSampler} and its fade as {@code Params.x}) over the whole screen for a time, faded
 * by that Amount. The impact frame is one; the server's screen effect hook plays others (grayscale for players a Standstill freezes;
 * its inverted frame comes from its own client code).</li>
 * </ul>
 * A shader that won't load (a driver that can't run it, or no such file) is logged once and skipped, and nothing is copied for it; the
 * rest still play. Only Kegare's own effects ({@link ScreenEffects#ALL}) play: the id is the server's to name, never to point at another
 * namespace's files. The two window-sized targets are let go on leaving a world, on a reload and after half a minute unused. 1.21.11 has no
 * JSON post chain to load any more (the old {@code shaders/post/*.json} format is gone), so an effect is a fragment shader alone.
 */
public final class ScreenFx {
    private static @Nullable TextureTarget held;
    /** What an effect reads: the picture as it was before the effect, since a pass can't read what it writes. */
    private static @Nullable TextureTarget scratch;
    private static boolean capturing, holding;
    /** The freeze asked for, counted from the frame it's captured on: a slow frame must never eat it. */
    private static long holdMillis;
    private static long holdUntil;
    /** When a target was last needed: a window-sized target is tens of megabytes at 4K, so an idle one is let go. */
    private static long lastUsed;
    private static final long IDLE_NANOS = 30_000_000_000L;

    private static final Map<Identifier, FullscreenFx> EFFECTS = new HashMap<>();
    private static final Set<Identifier> BROKEN = new HashSet<>();
    private static final Map<Identifier, Playing> PLAYING = new LinkedHashMap<>();

    /** {@code start} and {@code end} are -1 until it's first drawn: its time counts from there, so it's always seen at least once. */
    private record Playing(long millis, long fade, long start, long end) {
        Playing started(long now) {
            return start >= 0 ? this : new Playing(millis, fade, now, now + millis * 1_000_000L);
        }

        float amount(long now) {
            if (fade <= 0) return 1f;
            float in = (now - start) / (float) fade, out = (end - now) / (float) fade;
            return Math.max(0f, Math.min(1f, Math.min(in, out)));
        }
    }

    private ScreenFx() {}

    /** Freezes the picture for {@code millis} from the next frame (a hit landing). Overlapping freezes run to the latest end. */
    public static void hold(long millis) {
        if (millis <= 0) return;
        if (holding) {
            holdUntil = Math.max(holdUntil, System.nanoTime() + millis * 1_000_000L);
        } else {
            capturing = true;
            holdMillis = Math.max(holdMillis, millis);
        }
    }

    public static boolean holding() {
        return capturing || holding;
    }

    /**
     * Only Kegare's own screen effects ({@link ScreenEffects#ALL}) run: the id comes from the server, and it is never a way to make this
     * client compile a shader out of some other namespace's resources, nor to fill its tables with names.
     */
    public static boolean allowed(Identifier effect) {
        return ScreenEffects.ALL.contains(effect);
    }

    /** Plays a screen effect for {@code millis}, fading in and out over {@code fadeMillis}. Replayed while showing: {@code millis} from now. */
    public static void play(Identifier effect, long millis, long fadeMillis) {
        if (!allowed(effect)) return; // not logged: a server could send one a frame
        if (millis <= 0) {
            PLAYING.remove(effect);
            return;
        }
        long now = System.nanoTime();
        Playing was = PLAYING.get(effect);
        long fade = Math.min(fadeMillis, millis / 2) * 1_000_000L;
        if (was != null && was.start >= 0 && was.end > now) { // already showing: no fade in again, it just lasts longer
            PLAYING.put(effect, new Playing(millis, fade, was.start, Math.max(was.end, now + millis * 1_000_000L)));
        } else {
            PLAYING.put(effect, new Playing(millis, fade, -1, -1));
        }
    }

    public static boolean playing(Identifier effect) {
        Playing p = PLAYING.get(effect);
        return p != null && (p.start < 0 || p.end > System.nanoTime());
    }

    public static void afterLevel(DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        long now = System.nanoTime();
        if (!capturing && !holding && PLAYING.isEmpty()) {
            if ((held != null || scratch != null) && now - lastUsed > IDLE_NANOS) releaseTargets();
            return;
        }
        lastUsed = now;
        if (capturing) { // this frame is the one the hit landed on: keep it, and show it as it is
            capturing = false;
            holding = true;
            holdUntil = now + holdMillis * 1_000_000L;
            holdMillis = 0;
            FullscreenFx.copy(main, held(main));
        } else if (holding) {
            if (now >= holdUntil) holding = false;
            else if (held != null && held.width == main.width && held.height == main.height) FullscreenFx.copy(held, main);
        }
        if (PLAYING.isEmpty()) return;
        for (Iterator<Map.Entry<Identifier, Playing>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Identifier, Playing> e = it.next();
            Playing p = e.getValue().started(now);
            e.setValue(p);
            if (now > p.end && p.start != now) {
                it.remove();
                continue;
            }
            FullscreenFx fx = effect(mc, e.getKey()); // null for one that can't run: nothing is copied for it
            if (fx == null) continue;
            try {
                TextureTarget before = scratch(main);
                FullscreenFx.copy(main, before);
                fx.draw(main, new float[] {p.amount(now)}, false, before.getColorTextureView());
            } catch (RuntimeException | LinkageError error) {
                fail(e.getKey(), error);
            }
        }
    }

    private static TextureTarget held(RenderTarget main) {
        if (held == null) held = new TextureTarget("kegare_hit_stop", main.width, main.height, false);
        else if (held.width != main.width || held.height != main.height) held.resize(main.width, main.height);
        return held;
    }

    private static TextureTarget scratch(RenderTarget main) {
        if (scratch == null) scratch = new TextureTarget("kegare_screen_fx", main.width, main.height, false);
        else if (scratch.width != main.width || scratch.height != main.height) scratch.resize(main.width, main.height);
        return scratch;
    }

    private static @Nullable FullscreenFx effect(Minecraft mc, Identifier effect) {
        if (BROKEN.contains(effect)) return null;
        FullscreenFx fx = EFFECTS.get(effect);
        if (fx != null) return fx;
        Identifier file = Identifier.fromNamespaceAndPath(effect.getNamespace(), "shaders/post/" + effect.getPath() + ".fsh");
        if (mc.getResourceManager().getResource(file).isEmpty()) {
            BROKEN.add(effect);
            Kegare.LOGGER.warn("Screen effect {} has no shader ({}); skipping it", effect, file);
            return null;
        }
        fx = new FullscreenFx(effect.getNamespace() + "_" + effect.getPath(),
                Identifier.fromNamespaceAndPath(effect.getNamespace(), "post/" + effect.getPath()), List.of("DiffuseSampler"), false);
        if (!fx.isValid()) { // a shader that doesn't compile draws nothing and says nothing: don't copy the picture for it every frame
            fx.close();
            BROKEN.add(effect);
            Kegare.LOGGER.warn("Screen effect {} doesn't compile; skipping it", effect);
            return null;
        }
        EFFECTS.put(effect, fx);
        return fx;
    }

    private static void fail(Identifier effect, Throwable error) {
        BROKEN.add(effect);
        FullscreenFx fx = EFFECTS.remove(effect);
        if (fx != null) fx.close();
        Kegare.LOGGER.warn("Screen effect {} can't run ({}); skipping it", effect, error.toString());
    }

    /** Lets go of the two window-sized targets; they are made again the next time one is needed. */
    private static void releaseTargets() {
        if (held != null) held.destroyBuffers();
        if (scratch != null) scratch.destroyBuffers();
        held = null;
        scratch = null;
    }

    /** Resource packs changed: the shaders load again. */
    public static void reload() {
        EFFECTS.values().forEach(FullscreenFx::close);
        EFFECTS.clear();
        BROKEN.clear();
        releaseTargets();
    }

    /** A world is left. */
    public static void clear() {
        capturing = holding = false;
        holdMillis = 0;
        PLAYING.clear();
        releaseTargets();
    }
}
