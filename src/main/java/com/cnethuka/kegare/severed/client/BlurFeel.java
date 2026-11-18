package com.cnethuka.kegare.severed.client;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.config.KegareClientConfig;
import com.cnethuka.kegare.config.KegareClientConfig.VfxQuality;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import com.cnethuka.kegare.platform.bus.IEventBus;
import com.cnethuka.kegare.platform.client.event.RegisterGuiLayersEvent;
import com.cnethuka.kegare.platform.client.event.ViewportEvent;
import com.cnethuka.kegare.platform.client.gui.VanillaGuiLayers;
import com.cnethuka.kegare.platform.NeoForge;

/**
 * Blur from inside it (DESIGN §3.27), like a dash's feel but held for as long as you're moving fast: the view opens a little wider the
 * faster you go, and needle streaks run round the screen's edge, flowing from where you're headed. Nothing is ever drawn inside the
 * middle 60% of the screen. Half of it at Reduced, none at Vanilla; vanilla's FOV Effects slider scales the widening too. First
 * person only.
 */
final class BlurFeel {
    private static final Identifier LAYER = Kegare.id("blur_streaks");
    /** How much wider the view opens at full Blur speed, every setting at full. */
    static final float FOV_WIDEN = 0.12f;
    private static final int STREAKS_FULL = 18, STREAKS_REDUCED = 8;
    private static final float PEAK_ALPHA = 0.55f;
    private static final float CLEAR_RADIUS = 0.6f, FULL_RADIUS = 0.95f;
    /** The feel follows the speed smoothly: this much of the way each frame. */
    private static float level;

    private BlurFeel() {}

    static void init(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerBelow(VanillaGuiLayers.CROSSHAIR, LAYER, BlurFeel::render));
        NeoForge.EVENT_BUS.addListener(BlurFeel::onFov);
    }

    static float qualityScale() {
        VfxQuality quality = KegareClientConfig.VFX_QUALITY.get();
        return quality == VfxQuality.FULL ? 1f : quality == VfxQuality.REDUCED ? 0.5f : 0f;
    }

    /** 0 standing still, 1 at full Blur speed. */
    private static float target() {
        if (!BlurDrive.on() && !RushDrive.active()) return 0f;
        float top = Math.max(0.05f, BlurDrive.state.speed());
        float speed = (float) (RushDrive.active() ? RushDrive.speed() : BlurDrive.speed());
        return Mth.clamp(speed / top, 0f, 1.6f);
    }

    static void tick() {
        level += (target() - level) * 0.35f;
        if (level < 0.005f) level = 0f;
    }

    static void reset() {
        level = 0f;
    }

    private static boolean showing(Minecraft mc) {
        return mc.player != null && mc.options.getCameraType().isFirstPerson();
    }

    private static void onFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov() || level <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        if (!showing(mc)) return;
        float strength = qualityScale() * mc.options.fovEffectScale().get().floatValue();
        if (strength > 0f) event.setFOV(event.getFOV() * (1f + FOV_WIDEN * strength * Math.min(1.3f, level)));
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!showing(mc) || mc.options.hideGui || level <= 0.05f || mc.level == null) return;
        float quality = qualityScale();
        if (quality <= 0f) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        Vec3 velocity = RushDrive.active() ? RushDrive.direction() : BlurDrive.velocity();
        if (velocity.lengthSqr() < 1.0e-6) return;
        Vec3 dir = velocity.normalize();
        float width = g.guiWidth(), height = g.guiHeight(), cx = width / 2f, cy = height / 2f;
        float yaw = mc.player.getViewYRot(partial) * Mth.DEG_TO_RAD;
        float forward = (float) (-Mth.sin(yaw) * dir.x + Mth.cos(yaw) * dir.z);
        float right = (float) (-Mth.cos(yaw) * dir.x - Mth.sin(yaw) * dir.z);
        float fromX = cx + right * width * 0.5f;
        float flow = forward < -0.3f ? -1f : 1f;
        // A new set every tick, each sliding outward over the tick: a steady rush past the edges.
        long tick = mc.level.getGameTime();
        float slide = partial;
        RandomSource random = RandomSource.create(tick * 7919L);
        int streaks = quality >= 1f ? STREAKS_FULL : STREAKS_REDUCED;
        float strength = Math.min(1f, level) * quality;
        for (int i = 0; i < streaks; i++) {
            float angle = (i + random.nextFloat()) / streaks * Mth.TWO_PI;
            float reach = 0.78f + random.nextFloat() * 0.3f;
            float length = height * (0.1f + random.nextFloat() * 0.16f) * (0.6f + 0.4f * level);
            float thick = (0.4f + random.nextFloat() * 0.6f) * Math.min(1f, g.guiHeight() / 240f);
            float x = cx + Mth.cos(angle) * cx * reach, y = cy + Mth.sin(angle) * cy * reach;
            float dx = x - fromX, dy = y - cy, norm = Mth.sqrt(dx * dx + dy * dy);
            if (norm < 1f) continue;
            dx /= norm;
            dy /= norm;
            float shift = height * 0.12f * (flow > 0f ? slide : 1f - slide);
            float x0 = x + dx * shift, y0 = y + dy * shift, x1 = x0 + dx * length, y1 = y0 + dy * length;
            int inner = alpha(strength, x0, y0, cx, cy), outer = alpha(strength, x1, y1, cx, cy);
            if (inner == 0 && outer == 0) continue;
            needle(g, x0, y0, dx, dy, length, thick, inner, outer);
        }
    }

    /** Pieces a needle is cut into along its length: its width grows a step at a time from the point (the HUD fills rectangles, not triangles). */
    private static final int NEEDLE_STEPS = 3;

    /**
     * A needle from {@code (x0, y0)} out along {@code (dx, dy)} for {@code length}: a point at the inner end widening to {@code 2 * thick}
     * at the outer, its opacity running from {@code inner} to {@code outer} (0 to 255). Drawn as a few turned rectangles with a fade each.
     */
    private static void needle(GuiGraphics g, float x0, float y0, float dx, float dy, float length, float thick, int inner, int outer) {
        float angle = (float) Math.atan2(-dx, dy); // turns the HUD's down (+y) onto the needle's way
        g.pose().pushMatrix();
        g.pose().translate(x0, y0);
        g.pose().rotate(angle);
        for (int i = 0; i < NEEDLE_STEPS; i++) {
            int from = Math.round(length * i / NEEDLE_STEPS), to = Math.round(length * (i + 1) / NEEDLE_STEPS);
            if (to <= from) continue;
            float half = thick * (i + 0.5f) / NEEDLE_STEPS;
            int a0 = Math.round(Mth.lerp(i / (float) NEEDLE_STEPS, (float) inner, (float) outer));
            int a1 = Math.round(Mth.lerp((i + 1) / (float) NEEDLE_STEPS, (float) inner, (float) outer));
            g.pose().pushMatrix();
            g.pose().scale(half, 1f);
            g.fillGradient(-1, from, 1, to, a0 << 24 | NEEDLE_RGB, a1 << 24 | NEEDLE_RGB);
            g.pose().popMatrix();
        }
        g.pose().popMatrix();
    }

    private static final int NEEDLE_RGB = 0xF2EEE6;

    private static int alpha(float strength, float x, float y, float cx, float cy) {
        float ex = (x - cx) / cx, ey = (y - cy) / cy;
        return Mth.floor(255f * PEAK_ALPHA * strength * edgeFade(Mth.sqrt(ex * ex + ey * ey)));
    }

    /** 0 inside the middle 60% of the screen, rising to 1 near its edge. */
    static float edgeFade(float radius) {
        float t = (radius - CLEAR_RADIUS) / (FULL_RADIUS - CLEAR_RADIUS);
        return t <= 0f ? 0f : t >= 1f ? 1f : t * t * (3f - 2f * t);
    }
}
