package com.cnethuka.kegare.fists.client;

import com.cnethuka.kegare.config.KegareClientConfig;
import com.cnethuka.kegare.config.KegareClientConfig.VfxQuality;
import com.cnethuka.kegare.fists.BlackSparkPayload;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Black Spark's look (DESIGN §3.28): for one sharp moment, pixel art bursts from the point of contact, black and red and the same for
 * everyone: jagged stair-stepped bolts, black with red outlines, crackling outward and breaking up; a black starburst at the heart, rimmed
 * in red; black and red pixels thrown out and falling. {@link BlackSparkPixels} decides which pixels are lit; this lays them on a plane
 * facing the camera, as flat squares of one colour each (no texture, no blending, no blur), full-bright so the black holds against the sky
 * and the night alike.
 * <p>
 * Never on your own body in your own first person (it would fill your eyes: the Impact System gives you the hit). Smaller the closer it is
 * to the camera. Reduced: fewer bolts and sparks, sooner gone. Vanilla: black and red dust and crit sparks.
 */
public final class BlackSparkClient {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(WHITE);
    /** How big it is, in blocks, at full size: 16 pixels to it. */
    private static final float SIZE = 1.0f;
    private static final float PIXELS_PER_SIZE = 16f;

    private record Burst(Vec3 at, long born, int life, long seed, int bolts, int sparks, float size) {}

    private static final List<Burst> BURSTS = new ArrayList<>();

    private BlackSparkClient() {}

    public static void init() {
        BlackSparkPayload.setClientHandler(BlackSparkClient::spawn);
        NeoForge.EVENT_BUS.addListener(BlackSparkClient::render);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> BURSTS.clear());
    }

    private static void spawn(BlackSparkPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        if (firstPerson && p.victimId() == mc.player.getId()) return;
        VfxQuality quality = KegareClientConfig.VFX_QUALITY.get();
        if (quality == VfxQuality.VANILLA) {
            vanilla(level, p.at(), p.direction());
            return;
        }
        float size = SIZE;
        double d = mc.gameRenderer.getMainCamera().getPosition().distanceTo(p.at());
        if (firstPerson && d < 0.6) return;
        if (d < 2.5) size *= (float) Math.max(0.4, d / 2.5);
        boolean reduced = quality == VfxQuality.REDUCED;
        BURSTS.add(new Burst(p.at(), level.getGameTime(), reduced ? 4 : 6, level.getRandom().nextLong(), reduced ? 4 : 7, reduced ? 6 : 14, size));
    }

    /** Vanilla: black dust, red dust and crit sparks thrown out from the hit. */
    private static void vanilla(ClientLevel level, Vec3 at, Vec3 dir) {
        RandomSource r = level.getRandom();
        DustParticleOptions black = new DustParticleOptions(rgb(BlackSparkPixels.CORE), 1.2f);
        DustParticleOptions red = new DustParticleOptions(rgb(BlackSparkPixels.EDGE), 1.0f);
        for (int i = 0; i < 16; i++) {
            Vec3 v = out(r, dir).scale(0.15 + 0.25 * r.nextDouble());
            level.addParticle(i % 2 == 1 ? red : black, at.x, at.y, at.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 6; i++) {
            Vec3 v = out(r, dir).scale(0.3);
            level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, v.x, v.y, v.z);
        }
    }

    private static Vector3f rgb(int rgb) {
        return new Vector3f((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f);
    }

    /** A way out from the hit: mostly onward and to the sides, never back at the one who struck. */
    private static Vec3 out(RandomSource r, Vec3 dir) {
        Vec3 v = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize();
        if (v.dot(dir) < -0.2) v = v.subtract(dir.scale(2 * v.dot(dir)));
        return v.add(dir.scale(0.35)).normalize();
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            BURSTS.clear();
            return;
        }
        long now = level.getGameTime();
        BURSTS.removeIf(b -> now - b.born > b.life + 1);
    }

    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BURSTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float now = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(TYPE);
        Matrix4f m = event.getPoseStack().last().pose();
        Vec3 cam = camera.getPosition();
        for (Burst b : BURSTS) {
            float t = (now - b.born) / b.life;
            if (t < 0 || t >= 1) continue;
            draw(out, m, b, t, (long) Math.floor(now), cam);
        }
        buffers.endBatch(TYPE);
    }

    /** The burst's pixels, as squares on a plane through the point of contact that faces the camera, upright with the world. */
    private static void draw(VertexConsumer out, Matrix4f m, Burst b, float t, long tick, Vec3 cam) {
        Vec3 c = b.at.subtract(cam);
        double dist = c.length();
        if (dist < 1.0e-4) return;
        Vec3 forward = c.scale(1 / dist);
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        float px = b.size / PIXELS_PER_SIZE;
        Vec3 r = right.scale(px), u = up.scale(px);
        for (BlackSparkPixels.Pixel p : BlackSparkPixels.burst(b.seed, tick, t, b.life, b.bolts, b.sparks)) {
            Vec3 corner = c.add(r.scale(p.x() - 0.5)).add(u.scale(p.y() - 0.5));
            vertex(out, m, corner.add(u), p.rgb());
            vertex(out, m, corner.add(u).add(r), p.rgb());
            vertex(out, m, corner.add(r), p.rgb());
            vertex(out, m, corner, p.rgb());
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f m, Vec3 p, int rgb) {
        out.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255)
                .setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 1f, 0f);
    }
}
