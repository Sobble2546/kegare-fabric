package com.cnethuka.kegare.client.visual;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.innatetechnique.visual.Visual;
import com.cnethuka.photonfx.api.Anchor;
import com.cnethuka.photonfx.api.FxHandle;
import com.cnethuka.photonfx.api.PhotonFx;
import java.util.Collection;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Plays Kegare's {@code .fx} effects with Photon FX (mod {@code photonfx}, the runtime port of Photon nested in the 1.21.11
 * jar; DESIGN §3.13). The only Kegare class that names photonfx's types, and {@code VisualClientEvents} touches it only
 * when that mod is loaded, so a missing engine never loads one. The 26.x builds have no engine: their overlay holds a
 * {@code PhotonBridge} that installs nothing.
 * <p>
 * Delegation only, one photonfx call per {@link PhotonEffects.Backend} method. A backend must never throw: the first call
 * that fails is logged, once, and switches the bridge off until the game restarts. From then on every factory reports
 * "can't play", so Kegare falls back to vanilla particles, and nothing playing reports itself as playing; {@code stop}
 * still reaches the engine, so an effect that is on screen ends with the loop it stands for.
 */
final class PhotonBridge implements PhotonEffects.Backend {
    private boolean disabled;

    /** Installs the bridge as Kegare's particle engine. Call only when the mod {@code photonfx} is loaded. */
    static void install() {
        PhotonEffects.install(new PhotonBridge());
        Kegare.LOGGER.info("Ability effects are played by Photon FX");
    }

    /** Whether a call failed and the bridge switched itself off. */
    boolean disabled() {
        return disabled;
    }

    static Anchor anchor(Visual.Anchor anchor) {
        return switch (anchor) {
            case EYES -> Anchor.EYES;
            case BODY -> Anchor.BODY;
            case FEET -> Anchor.FEET;
            case MOTION -> Anchor.MOTION;
        };
    }

    private static class Handle implements PhotonEffects.Playing {
        final FxHandle fx;

        Handle(FxHandle fx) {
            this.fx = fx;
        }
    }

    private final class MovableHandle extends Handle implements PhotonEffects.Movable {
        MovableHandle(com.cnethuka.photonfx.api.Movable fx) {
            super(fx);
        }

        @Override
        public void moveTo(Vec3 pos) {
            run("moveTo", () -> ((com.cnethuka.photonfx.api.Movable) fx).moveTo(pos));
        }
    }

    @Nullable
    @Override
    public PhotonEffects.Playing onEntity(Identifier id, Entity entity, Visual.Anchor anchor, boolean reduced, boolean vanishWithEntity) {
        return call("onEntity", null, () -> {
            FxHandle fx = PhotonFx.onEntity(id, entity, anchor(anchor), reduced, vanishWithEntity);
            return fx == null ? null : new Handle(fx);
        });
    }

    @Nullable
    @Override
    public PhotonEffects.Playing at(Identifier id, Level level, Vec3 pos, boolean reduced) {
        return call("at", null, () -> {
            FxHandle fx = PhotonFx.at(id, level, pos, reduced);
            return fx == null ? null : new Handle(fx);
        });
    }

    @Nullable
    @Override
    public PhotonEffects.Movable movableAt(Identifier id, Level level, Vec3 pos, boolean reduced) {
        return call("movableAt", null, () -> {
            com.cnethuka.photonfx.api.Movable fx = PhotonFx.movableAt(id, level, pos, reduced);
            return fx == null ? null : new MovableHandle(fx);
        });
    }

    @Override
    public void stop(PhotonEffects.Playing playing, boolean immediately) {
        // also when switched off: what the engine still shows must not outlive the loop it stands for
        if (!(playing instanceof Handle handle)) return;
        try {
            handle.fx.stop(immediately);
        } catch (Exception | LinkageError e) {
            failed("stop", e);
        }
    }

    @Override
    public boolean isPlaying(PhotonEffects.Playing playing) {
        return playing instanceof Handle handle && call("isPlaying", false, handle.fx::isPlaying);
    }

    @Override
    public void scale(PhotonEffects.Playing playing, float scale) {
        if (playing instanceof Handle handle) run("scale", () -> handle.fx.scale(scale));
    }

    @Override
    public void tint(PhotonEffects.Playing playing, int argb) {
        if (playing instanceof Handle handle) run("tint", () -> handle.fx.tint(argb));
    }

    @Override
    public void persistLifetime(PhotonEffects.Playing playing, int ticks) {
        if (playing instanceof Handle handle) run("persistLifetime", () -> handle.fx.persistLifetime(ticks));
    }

    @Override
    public void preload(Collection<Identifier> ids) {
        run("preload", () -> PhotonFx.preload(ids));
    }

    private void run(String what, Runnable call) {
        call(what, null, () -> {
            call.run();
            return null;
        });
    }

    /** One guarded call into the engine: {@code otherwise} when the bridge is off or the call fails. */
    private <T> T call(String what, T otherwise, Supplier<T> call) {
        if (disabled) return otherwise;
        try {
            return call.get();
        } catch (Exception | LinkageError e) {
            failed(what, e);
            return otherwise;
        }
    }

    private void failed(String what, Throwable e) {
        if (disabled) return;
        disabled = true;
        Kegare.LOGGER.error("Photon FX failed in {}; ability effects use vanilla particles until the game restarts", what, e);
    }
}
