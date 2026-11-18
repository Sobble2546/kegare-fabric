package com.cnethuka.kegare.platform.client;

import com.cnethuka.kegare.platform.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.joml.Matrix4f;

/**
 * Where {@link RenderLevelStageEvent} comes from on 1.21.11 (the per-version seam; 26.x replaces this class).
 * <ul>
 *   <li>AFTER_SKY: Fabric's START_MAIN (the main pass starts; a "world frame started" flag).</li>
 *   <li>AFTER_ENTITIES: Fabric's AFTER_ENTITIES.</li>
 *   <li>AFTER_PARTICLES / AFTER_WEATHER: a frame-graph pass added right after vanilla's particles / weather pass by
 *       {@code mixin.clientev.LevelRendererPassesMixin} through {@link #addPass}.</li>
 * </ul>
 */
public final class LevelStages {
    private LevelStages() {}

    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();

    static void init() {
        WorldRenderEvents.START_MAIN.register(context -> post(RenderLevelStageEvent.Stage.AFTER_SKY, new PoseStack()));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            PoseStack pose = context.matrices();
            post(RenderLevelStageEvent.Stage.AFTER_ENTITIES, pose != null ? pose : new PoseStack());
        });
    }

    /** The matrices of the level render about to run: first is the world-to-camera rotation, second the projection (parameter order of LevelRenderer.renderLevel; inferred, see the report). */
    public static void captureMatrices(Matrix4f modelView, Matrix4f projection) {
        MODEL_VIEW.set(modelView);
        PROJECTION.set(projection);
    }

    /** Adds a pass drawing after the one just added to the graph: it reads and writes the main target and posts the stage when it runs. */
    public static void addPass(FrameGraphBuilder builder, LevelTargetBundle targets, RenderLevelStageEvent.Stage stage) {
        FramePass pass = builder.addPass("kegare_" + stage.name().toLowerCase(java.util.Locale.ROOT));
        targets.main = pass.readsAndWrites(targets.main);
        pass.disableCulling();
        pass.executes(() -> post(stage, new PoseStack()));
    }

    private static void post(RenderLevelStageEvent.Stage stage, PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        ClientEventHooks.levelStage(stage, mc.gameRenderer.mainCamera(), mc.getDeltaTracker(), pose, new Matrix4f(MODEL_VIEW), new Matrix4f(PROJECTION));
    }
}
