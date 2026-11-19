package com.cnethuka.kegare.melee.client;

import com.cnethuka.kegare.fists.FistsPayload;
import com.cnethuka.kegare.fists.client.BlackSparkPixels;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * What the server says is round each player's fists (DESIGN §3.28, {@link com.cnethuka.kegare.fists.FistsTell}): Reinforced Fists, which
 * {@link FistTell} draws as the ready fist's own fire on both hands, and the zone's flicker, drawn here.
 */
public final class FistBurn {
    private static final Map<Integer, FistsPayload> SHOWN = new HashMap<>();

    private FistBurn() {}

    static void update(FistsPayload payload) {
        if (payload.flags() == 0) SHOWN.remove(payload.entityId());
        else SHOWN.put(payload.entityId(), payload);
    }

    static void clear() {
        SHOWN.clear();
    }

    /** What's on this player (null if nothing), as sent. */
    @Nullable
    static FistsPayload on(Player player) {
        FistsPayload shown = SHOWN.get(player.getId());
        Minecraft mc = Minecraft.getInstance();
        if (shown == null || mc.player == null || player.isInvisibleTo(mc.player)) return null;
        return shown;
    }

    /** The Malice color Fists burn in on this player, as the technique defines it, or null if they aren't on (or you can't see them). */
    @Nullable
    static Integer colorOn(Player player) {
        FistsPayload shown = on(player);
        return shown == null || !shown.has(FistsPayload.BURN) ? null : shown.color() & 0xFFFFFF;
    }

    /** Everyone something has been sent about. */
    static java.util.Set<Integer> ids() {
        return SHOWN.keySet();
    }

    /** Gone from view: the server sends it again when they're back. */
    static void forget(int entityId) {
        SHOWN.remove(entityId);
    }

    /** Whether they're in the zone (and you can see them): its flicker is black and red for everyone, so there's no colour to it. */
    static boolean zoneOn(Player player) {
        FistsPayload shown = on(player);
        return shown != null && shown.has(FistsPayload.ZONE);
    }

    private static final net.minecraft.client.renderer.RenderType FLICKER = net.minecraft.client.renderer.RenderType.entityTranslucentEmissive(
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png"));
    /** How many chips can be round a fist at once, and how often one shows (per tick, each). */
    private static final int FLICKER_CHIPS = 4;
    private static final float FLICKER_CHANCE = 0.3f;
    /** A skin pixel: the chips are the same pixel art as the fist's fire. */
    private static final float PIXEL = 1f / 16f;
    private static final float[][] BESIDE = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * The zone, on one fist: a faint flicker of black and red pixels, Black Spark's own colours (DESIGN §3.28). A few chips, each a black
     * pixel with a red one beside it (now and then a lone deep red one), here for a tick and gone, never the same place twice. Square and
     * flat, facing the camera. With the pose in the arm's space.
     */
    static void drawFlicker(PoseStack pose, MultiBufferSource buffers, int tick, int seed, boolean slim, boolean leftArm, Vector3f toCamera) {
        Vector3f look = pose.last().normal().transformTranspose(toCamera, new Vector3f()).normalize();
        Vector3f right = new Vector3f(look).cross(0f, 1f, 0f);
        if (right.lengthSquared() < 1.0e-6f) right.set(1f, 0f, 0f);
        right.normalize();
        Vector3f up = new Vector3f(right).cross(look).normalize();
        float cx = FistShellMesh.armCentre(slim, leftArm), cy = FistShellMesh.fistY();
        com.mojang.blaze3d.vertex.VertexConsumer out = buffers.getBuffer(FLICKER);
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(tick * 341873128712L + seed * 132897987541L);
        for (int i = 0; i < FLICKER_CHIPS; i++) {
            float x = cx + (r.nextFloat() - 0.5f) * 0.42f, y = cy + (r.nextFloat() - 0.6f) * 0.4f, z = (r.nextFloat() - 0.5f) * 0.42f;
            float[] beside = BESIDE[r.nextInt(BESIDE.length)];
            boolean lone = r.nextFloat() < 0.25f;
            if (r.nextFloat() > FLICKER_CHANCE) continue;
            if (lone) {
                pixel(out, pose.last(), x, y, z, right, up, 0f, 0f, BlackSparkPixels.DEEP);
            } else {
                pixel(out, pose.last(), x, y, z, right, up, 0f, 0f, BlackSparkPixels.CORE);
                pixel(out, pose.last(), x, y, z, right, up, beside[0], beside[1], BlackSparkPixels.EDGE);
            }
        }
    }

    /** One square pixel facing the camera, {@code (dx, dy)} pixels over from (x, y, z) as the camera sees it. */
    private static void pixel(com.mojang.blaze3d.vertex.VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, Vector3f right, Vector3f up,
                              float dx, float dy, int rgb) {
        float ox = x + (right.x * (dx - 0.5f) + up.x * (dy - 0.5f)) * PIXEL;
        float oy = y + (right.y * (dx - 0.5f) + up.y * (dy - 0.5f)) * PIXEL;
        float oz = z + (right.z * (dx - 0.5f) + up.z * (dy - 0.5f)) * PIXEL;
        float rx = right.x * PIXEL, ry = right.y * PIXEL, rz = right.z * PIXEL, ux = up.x * PIXEL, uy = up.y * PIXEL, uz = up.z * PIXEL;
        vertex(out, pose, ox + ux, oy + uy, oz + uz, rgb);
        vertex(out, pose, ox + ux + rx, oy + uy + ry, oz + uz + rz, rgb);
        vertex(out, pose, ox + rx, oy + ry, oz + rz, rgb);
        vertex(out, pose, ox, oy, oz, rgb);
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, int rgb) {
        out.addVertex(pose, x, y, z).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255).setUv(0.5f, 0.5f)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .setNormal(0f, 1f, 0f);
    }
}
