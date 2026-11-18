package com.cnethuka.kegare.severed.client;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.client.visual.FlatBodies;
import com.cnethuka.kegare.client.visual.FullscreenFx;
import com.cnethuka.kegare.client.visual.SilhouetteMask;
import com.cnethuka.kegare.severed.SensesPayload;
import com.cnethuka.kegare.severed.SeveredConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.cnethuka.kegare.platform.client.event.ClientPlayerNetworkEvent;
import com.cnethuka.kegare.platform.client.event.RenderLevelStageEvent;
import com.cnethuka.kegare.platform.NeoForge;

/**
 * What a Severed player senses, drawn (DESIGN §3.27): every body the server says they sense gets a faint, pale outline, through
 * walls, fading with distance. Each body's silhouette is drawn into a mask (its brightness how near it is), then
 * {@code shaders/post/senses.fsh} (a {@link FullscreenFx} pass) lays a two-pixel outline just outside each silhouette over the picture. Invisible bodies are
 * drawn into the mask as if they weren't. No names, no labels, no colors, no glow; at every effects setting, because it's what
 * you play by.
 */
public final class SensesClient {
    private static final Identifier SHADER = Kegare.id("post/senses");
    /** How bright the farthest outline is, against the nearest. */
    private static final float FAR = 0.3f;
    /** How strongly the pale outline is laid over the picture. */
    private static final float STRENGTH = 0.6f;
    private static final Set<Integer> SENSED = new HashSet<>();
    /** Real positions of what's sensed but seen behind where it is (DESIGN §3.30): the last two the server sent, for smooth drawing. */
    private static final java.util.Map<Integer, Vec3[]> REAL = new java.util.HashMap<>();
    @Nullable private static FullscreenFx outline;
    private static boolean broken;

    private SensesClient() {}

    public static void init() {
        SensesPayload.setClientHandler(p -> {
            SENSED.clear();
            SENSED.addAll(p.ids());
        });
        com.cnethuka.kegare.severed.SensedPositionsPayload.setClientHandler(p -> {
            java.util.Map<Integer, Vec3[]> next = new java.util.HashMap<>();
            for (var e : p.entries()) {
                Vec3[] was = REAL.get(e.id());
                next.put(e.id(), new Vec3[] {was == null ? e.pos() : was[1], e.pos()});
            }
            REAL.clear();
            REAL.putAll(next);
        });
        NeoForge.EVENT_BUS.addListener(SensesClient::onRender);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            SENSED.clear();
            REAL.clear();
        });
    }

    /** Who's sensed right now (for the tests and the HUD). */
    public static Set<Integer> sensed() {
        return SENSED;
    }

    /** Resource packs changed: the shader loads again. */
    public static void reload() {
        if (outline != null) outline.close();
        outline = null;
        broken = false;
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || SENSED.isEmpty() || broken) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        try {
            draw(mc, level, event);
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            Kegare.LOGGER.warn("Senses outline can't be drawn ({}); not drawn", e.toString());
        }
    }

    private static void draw(Minecraft mc, ClientLevel level, RenderLevelStageEvent event) {
        RenderTarget main = mc.getMainRenderTarget();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().position();
        double range = Math.max(1.0, SeveredConfig.SENSES_RANGE.get());
        VertexConsumer out = SilhouetteMask.begin(main);
        for (int id : SENSED) {
            Entity e = level.getEntity(id);
            if (e == null || e == mc.player || !e.isAlive()) continue;
            double d = e.position().distanceTo(mc.player.position());
            float shade = Mth.clamp((float) (1.0 - (1.0 - FAR) * d / range), FAR, 1f);
            int grey = Math.round(shade * 255f);
            Vec3[] real = REAL.get(id); // sensing isn't seeing: where they really are, not where eyes see them
            Vec3 at = (real != null ? real[0].lerp(real[1], partial) : e.getPosition(partial)).subtract(cam);
            boolean invisible = e.isInvisible();
            if (invisible) e.setInvisible(false); // sensed, not seen: an invisible body is there all the same
            try {
                FlatBodies.draw(e, at, partial, out, new FlatBodies.Look(grey << 16 | grey << 8 | grey, 255, 0, false));
            } finally {
                if (invisible) e.setInvisible(true);
            }
        }
        SilhouetteMask.end(); // draws the silhouettes into the mask
        if (outline == null) outline = new FullscreenFx("senses", SHADER, List.of("MaskSampler"), true);
        outline.draw(main, new float[] {STRENGTH}, false, SilhouetteMask.view());
    }
}
