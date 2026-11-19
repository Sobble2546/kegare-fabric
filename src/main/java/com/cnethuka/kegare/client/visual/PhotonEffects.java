package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.api.visual.Visual;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The seam between Kegare's visuals and a particle engine (client only). Today no engine is installed, so every
 * method reports "can't play" (null) and the callers fall back to vanilla particles, exactly as they do for a missing
 * or broken effect. An engine (Photon, once it exists on Fabric) plugs in through {@link #install(Backend)}; nothing
 * else in Kegare names its classes, so a missing engine never loads one.
 */
final class PhotonEffects {
    private static Backend backend;

    private PhotonEffects() {}

    /** A playing effect. Opaque to callers; only the backend that made it looks inside. */
    interface Playing {}

    /** A positional effect that follows {@link #moveTo}. */
    interface Movable extends Playing {
        void moveTo(Vec3 pos);
    }

    /** What a particle engine provides. Every method may return null/do nothing and must never throw. */
    interface Backend {
        @Nullable Playing onEntity(Identifier id, Entity entity, Visual.Anchor anchor, boolean reduced, boolean vanishWithEntity);

        @Nullable Playing at(Identifier id, Level level, Vec3 pos, boolean reduced);

        @Nullable Movable movableAt(Identifier id, Level level, Vec3 pos, boolean reduced);

        /** @param immediately also remove what is already visible */
        void stop(Playing playing, boolean immediately);

        boolean isPlaying(Playing playing);

        void scale(Playing playing, float scale);

        /** {@code argb} multiplied over every emitter named {@code tint...}. */
        void tint(Playing playing, int argb);

        /** Sets the particle lifetime of every emitter named {@code persist...}. */
        void persistLifetime(Playing playing, int ticks);

        void preload(Collection<Identifier> ids);
    }

    static void install(Backend engine) {
        backend = engine;
    }

    @Nullable
    static Playing onEntity(Identifier id, Entity entity, Visual.Anchor anchor, boolean reduced, boolean vanishWithEntity) {
        return backend == null ? null : backend.onEntity(id, entity, anchor, reduced, vanishWithEntity);
    }

    @Nullable
    static Playing at(Identifier id, Level level, Vec3 pos, boolean reduced) {
        return backend == null ? null : backend.at(id, level, pos, reduced);
    }

    @Nullable
    static Movable movableAt(Identifier id, Level level, Vec3 pos, boolean reduced) {
        return backend == null ? null : backend.movableAt(id, level, pos, reduced);
    }

    static void stop(@Nullable Playing playing) {
        stop(playing, false);
    }

    static void stop(@Nullable Playing playing, boolean immediately) {
        if (playing != null && backend != null) backend.stop(playing, immediately);
    }

    static boolean isPlaying(@Nullable Playing playing) {
        return playing != null && backend != null && backend.isPlaying(playing);
    }

    static void scale(@Nullable Playing playing, float scale) {
        if (playing != null && backend != null) backend.scale(playing, scale);
    }

    static void tint(@Nullable Playing playing, int argb) {
        if (argb != 0 && playing != null && backend != null) backend.tint(playing, argb);
    }

    static void persistLifetime(@Nullable Playing playing, int ticks) {
        if (playing != null && backend != null) backend.persistLifetime(playing, ticks);
    }

    static void preload(Collection<Identifier> ids) {
        if (!ids.isEmpty() && backend != null) backend.preload(ids);
    }
}
