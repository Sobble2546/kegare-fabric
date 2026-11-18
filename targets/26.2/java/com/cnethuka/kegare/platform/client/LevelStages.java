package com.cnethuka.kegare.platform.client;

import com.cnethuka.kegare.platform.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Where {@link RenderLevelStageEvent} comes from on 26.x. Nothing is drawn straight to the screen during the level render any more:
 * geometry is submitted to the frame's collector in Fabric's COLLECT_SUBMITS and vanilla sorts and draws it. So every stage is posted
 * there, in the old order (sky, entities, particles, weather), with {@link KBuffers} bound to the collector; the order between the
 * stages no longer decides what is drawn over what. UNVERIFIED in game.
 */
public final class LevelStages {
    private LevelStages() {}

    static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            CameraRenderState camera = context.levelState().cameraRenderState;
            Matrix4f view = new Matrix4f(camera.viewRotationMatrix), projection = new Matrix4f(camera.projectionMatrix);
            PoseStack pose = context.poseStack() != null ? context.poseStack() : new PoseStack();
            KBuffers.BufferSource.bind(context.submitNodeCollector());
            try {
                for (RenderLevelStageEvent.Stage stage : RenderLevelStageEvent.Stage.values()) {
                    ClientEventHooks.levelStage(stage, mc.gameRenderer.mainCamera(), mc.getDeltaTracker(), pose, new Matrix4f(view), new Matrix4f(projection));
                }
            } finally {
                KBuffers.BufferSource.bind(null);
            }
        });
    }
}
