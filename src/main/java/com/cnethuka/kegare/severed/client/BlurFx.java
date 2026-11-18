package com.cnethuka.kegare.severed.client;

import com.cnethuka.kegare.client.visual.FlatBodies;
import com.cnethuka.kegare.client.visual.WorldTypes;
import com.cnethuka.kegare.config.KegareClientConfig;
import com.cnethuka.kegare.config.KegareClientConfig.VfxQuality;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Blur as everyone else sees it (DESIGN §3.27): a smear of delayed afterimages behind the one blurring, desaturated and lit by the
 * world like the body (never glowing), with dust and grit kicked off the ground. The real body is drawn as always, a sharp, solid
 * silhouette at the front, with no afterimage within reach of it, so a hit on what you see is a hit on their real box. Reduced draws
 * fewer afterimages; Vanilla only the dust. No afterimage is drawn round the camera looking at it (yours in first person, or anyone
 * they blur past or through).
 */
final class BlurFx {
        /** Dust-grey: the afterimage's one colour. */
    private static final int GHOST_RGB = 0xA59E94;
    /** How many ticks back each afterimage is, at Full and at Reduced. */
    private static final int[] BACK_FULL = {2, 3, 5, 7, 9}, BACK_REDUCED = {3, 6, 9};
    /** No afterimage closer than this to the real body (the front stays clean), nor this close to another. */
    private static final double CLEAR_OF_BODY = 0.9, APART = 0.5;
    /** Nor this close to the camera looking at it: someone blurring past (or through) you never fills your view with grey. */
    private static final double CLEAR_OF_CAMERA = 1.6;
    private static final int KEPT = 12;
    /** Faster than a sprint (blocks a tick): dust flies. */
    private static final double DUSTY = 0.32;

    private static final Map<Integer, ArrayDeque<Vec3>> TRAILS = new HashMap<>();
    private static long clock;

    private BlurFx() {}

    static VfxQuality quality() {
        return KegareClientConfig.VFX_QUALITY.get();
    }

    static void started(Entity entity) {
        TRAILS.computeIfAbsent(entity.getId(), k -> new ArrayDeque<>());
    }

    static void stopped(Entity entity) {
        TRAILS.remove(entity.getId());
    }

    static void clear() {
        TRAILS.clear();
    }

    static void tick(ClientLevel level) {
        clock++;
        for (Iterator<Map.Entry<Integer, ArrayDeque<Vec3>>> it = TRAILS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, ArrayDeque<Vec3>> e = it.next();
            Entity entity = level.getEntity(e.getKey());
            if (entity == null || !entity.isAlive()) {
                it.remove();
                continue;
            }
            ArrayDeque<Vec3> trail = e.getValue();
            Vec3 at = entity.position();
            Vec3 was = trail.peekLast();
            if (was != null && was.distanceToSqr(at) > 64) trail.clear(); // a teleport isn't a smear
            trail.addLast(at);
            while (trail.size() > KEPT) trail.pollFirst();
            double speed = was == null ? 0 : Math.sqrt(Mth.square(at.x - was.x) + Mth.square(at.z - was.z));
            if (speed > DUSTY && entity.onGround()) dust(level, entity, at, speed);
        }
    }

    /** Dust and grit off the ground behind them: what they're running on. */
    private static void dust(ClientLevel level, Entity entity, Vec3 at, double speed) {
        int count = switch (quality()) {
            case FULL -> 2;
            case REDUCED -> 1;
            case VANILLA -> clock % 2 == 0 ? 1 : 0;
        };
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.2, at.z));
        if (ground.isAir() || count == 0) return;
        Vec3 motion = entity.getDeltaMovement();
        for (int i = 0; i < count; i++) {
            double x = at.x + (level.getRandom().nextDouble() - 0.5) * 0.5, z = at.z + (level.getRandom().nextDouble() - 0.5) * 0.5;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, at.y + 0.1, z, -motion.x * 0.4, 0.12 + speed * 0.1, -motion.z * 0.4);
        }
    }

    static void render(PoseStack pose, Camera camera, float partial) {
        if (TRAILS.isEmpty() || quality() == VfxQuality.VANILLA) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Vec3 cam = camera.position();
        int[] back = quality() == VfxQuality.FULL ? BACK_FULL : BACK_REDUCED;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Map.Entry<Integer, ArrayDeque<Vec3>> e : TRAILS.entrySet()) {
            Entity entity = level.getEntity(e.getKey());
            if (entity == null || (entity == mc.player && firstPerson)) continue;
            List<Vec3> trail = new ArrayList<>(e.getValue());
            Vec3 now = entity.getPosition(partial);
            Vec3 last = now;
            for (int k = 0; k < back.length; k++) {
                int index = trail.size() - 1 - back[k];
                if (index < 0) break;
                Vec3 at = trail.get(index);
                if (at.distanceTo(now) < CLEAR_OF_BODY || at.distanceTo(last) < APART) continue;
                if (at.add(0, entity.getBbHeight() / 2, 0).distanceTo(cam) < CLEAR_OF_CAMERA) continue;
                last = at;
                float alpha = 0.42f * (1f - k / (float) back.length);
                int light = LevelRenderer.getLightColor(level, BlockPos.containing(at.x, at.y + 1.0, at.z));
                ghost(buffers, entity, at.subtract(cam), alpha, light, partial);
            }
        }
    }

    /** The body drawn again at {@code at} (from the camera), all of it in dust-grey, see-through, lit as the world is there. */
    private static void ghost(MultiBufferSource.BufferSource buffers, Entity e, Vec3 at, float alpha, int light, float partial) {
        RenderType type = WorldTypes.ghost();
        int a = Math.round(Mth.clamp(alpha, 0f, 1f) * 255);
        FlatBodies.draw(e, at, partial, buffers.getBuffer(type), new FlatBodies.Look(GHOST_RGB, a, light, true));
        buffers.endBatch(type);
    }
}
